package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPlaybackState
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

/**
 * Модель активного участника комнаты в оперативной памяти сервера.
 */
data class ActiveParticipant(
    val userId: Uuid,
    val username: String,
    var role: WatchRoomParticipantRole,
    var isOnline: Boolean = true,
    var playbackState: WatchRoomPlaybackState = WatchRoomPlaybackState.READY,
    var isReady: Boolean = false,
    var intent: WatchParticipantIntent = WatchParticipantIntent.WATCHING_ATTENTIVELY,
    var lastPingAt: Long = Clock.System.now().toEpochMilliseconds(),
    var isDesynced: Boolean = false
) {
    fun toDto() = WatchRoomParticipantDto(
        userId = userId,
        username = username,
        role = role,
        isOnline = isOnline,
        playbackState = playbackState,
        isReady = isReady,
        intent = intent
    )
}

/**
 * In-Memory State Machine комнаты совместного просмотра.
 * Реализует алгоритм синхронизации TrueSync (PlayAt детерминированный запуск,
 * Grace Period буферизации 8 сек, реакции и миграцию хоста).
 */
class WatchRoomSession(
    val roomId: Uuid,
    val mediaId: String,
    val title: String,
    var hostUserId: Uuid,
    var controlMode: WatchRoomControlMode,
    initialSeason: Int?,
    initialEpisode: Int?,
    initialPositionSeconds: Long,
    private val scope: CoroutineScope,
    private val onProgressChanged: suspend (season: Int?, episode: Int?, positionSeconds: Long) -> Unit,
    private val onHostMigrated: suspend (newHostUserId: Uuid) -> Unit
) {
    private val logger = LoggerFactory.getLogger(WatchRoomSession::class.java)
    private val mutex = Mutex()

    // Текущее состояние воспроизведения
    var isPlaying: Boolean = false
        private set
    var anchorPositionMs: Long = initialPositionSeconds * 1000L
        private set
    var anchorServerTimestampMs: Long = Clock.System.now().toEpochMilliseconds()
        private set
    var currentSeason: Int? = initialSeason
        private set
    var currentEpisode: Int? = initialEpisode
        private set

    // Участники сессии
    private val participants = ConcurrentHashMap<Uuid, ActiveParticipant>()

    // Буферизующиеся участники
    private val bufferingParticipants = ConcurrentHashMap.newKeySet<Uuid>()
    private var gracePeriodJob: Job? = null

    // Реактивная шина событий комнаты (TrueSync / Плеер)
    private val _events = MutableSharedFlow<WatchRoomEvent>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: Flow<WatchRoomEvent> = _events.asSharedFlow()

    // Реактивная шина событий предстартового лобби (Snapshot + Upsert Item)
    private val _lobbyEvents = MutableSharedFlow<LobbyEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val lobbyEvents: Flow<LobbyEvent> = _lobbyEvents.asSharedFlow()

    // Реестр корутин отложенного отключения (Grace-Period 20 секунд)
    private val disconnectJobs = ConcurrentHashMap<Uuid, Job>()

    // Реестр активных соединений по пользователю (userId -> Set<connectionId>)
    private val userConnections = ConcurrentHashMap<Uuid, MutableSet<Uuid>>()

    init {
        // Эмитим начальный слепок состояния
        emitCurrentSyncState(null)
    }

    /**
     * Поток событий предстартового лобби.
     * При подключении всегда гарантированно отдает InitialSnapshot, затем шлет точечные события.
     */
    fun subscribeLobby(): Flow<LobbyEvent> = flow {
        emit(LobbyEvent.InitialSnapshot(WatchRoomStatus.ACTIVE, getParticipantList()))
        emitAll(lobbyEvents)
    }

    /**
     * Обработка подключения клиента к лобби или плееру.
     * Регистрирует connectionId и снимает таймер Grace-Period при переподключении.
     */
    suspend fun handleClientConnected(
        userId: Uuid,
        username: String,
        role: WatchRoomParticipantRole,
        connectionId: Uuid = Uuid.random()
    ): WatchRoomParticipantDto = mutex.withLock {
        disconnectJobs.remove(userId)?.cancel()
        userConnections.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(connectionId)

        val existing = participants[userId]
        val p = if (existing != null) {
            existing.isOnline = true
            existing.lastPingAt = Clock.System.now().toEpochMilliseconds()
            existing
        } else {
            val newP = ActiveParticipant(
                userId = userId,
                username = username,
                role = role,
                isOnline = true,
                playbackState = WatchRoomPlaybackState.READY,
                lastPingAt = Clock.System.now().toEpochMilliseconds()
            )
            participants[userId] = newP
            newP
        }

        val dto = p.toDto()
        _lobbyEvents.emit(LobbyEvent.ParticipantUpdated(dto))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
        dto
    }

    /**
     * Обработка потери сетевого соединения.
     * Если у пользователя больше нет других активных соединений, помечает оффлайн и запускает Grace-Period.
     */
    suspend fun handleClientDisconnected(
        userId: Uuid,
        connectionId: Uuid? = null
    ) = mutex.withLock {
        if (connectionId != null) {
            val connections = userConnections[userId]
            if (connections != null) {
                connections.remove(connectionId)
                if (connections.isNotEmpty()) {
                    logger.info("Connection {} for user {} closed, but {} other connection(s) still active.", connectionId, userId, connections.size)
                    return@withLock
                }
                userConnections.remove(userId)
            }
        } else {
            userConnections.remove(userId)
        }

        val p = participants[userId] ?: return@withLock
        p.isOnline = false
        p.playbackState = WatchRoomPlaybackState.OFFLINE
        bufferingParticipants.remove(userId)

        val dto = p.toDto()
        _lobbyEvents.emit(LobbyEvent.ParticipantUpdated(dto))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
    }

    /**
     * Окончательное исключение участника по истечении Grace-Period.
     */
    suspend fun evictParticipant(userId: Uuid) = mutex.withLock {
        disconnectJobs.remove(userId)
        userConnections.remove(userId)
        val removed = participants.remove(userId) ?: return@withLock
        bufferingParticipants.remove(userId)

        _lobbyEvents.emit(LobbyEvent.ParticipantRemoved(userId))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))

        if (removed.role == WatchRoomParticipantRole.HOST) {
            checkHostMigrationLocked()
        }
    }

    /**
     * Обновление готовности и намерения участника в лобби.
     */
    suspend fun setLobbyStatus(
        userId: Uuid,
        intent: WatchParticipantIntent,
        isReady: Boolean
    ): Boolean = mutex.withLock {
        val p = participants[userId] ?: return@withLock false
        p.intent = intent
        p.isReady = isReady

        val dto = p.toDto()
        _lobbyEvents.emit(LobbyEvent.ParticipantUpdated(dto))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
        true
    }

    /**
     * Запуск воспроизведения хостом из лобби.
     */
    suspend fun triggerStartPlayback(userId: Uuid): Boolean = mutex.withLock {
        val p = participants[userId] ?: return@withLock false
        if (p.role != WatchRoomParticipantRole.HOST && controlMode == WatchRoomControlMode.HOST_ONLY) {
            logger.warn("Non-host {} tried to trigger start playback in room {}", userId, roomId)
            return@withLock false
        }

        gracePeriodJob?.cancel()
        gracePeriodJob = null
        bufferingParticipants.clear()

        val now = Clock.System.now().toEpochMilliseconds()
        val targetStartMs = now + 500L
        isPlaying = true
        anchorPositionMs = 0L
        anchorServerTimestampMs = targetStartMs
        p.playbackState = WatchRoomPlaybackState.PLAYING

        emitCurrentSyncState(userId)
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
        _lobbyEvents.emit(
            LobbyEvent.TransitionToPlayer(
                playAtServerTimestampMs = targetStartMs,
                season = currentSeason,
                episode = currentEpisode
            )
        )

        onProgressChanged(currentSeason, currentEpisode, 0L)
        true
    }

    /**
     * Возвращает текущее интерполированное время воспроизведения в миллисекундах.
     */
    fun getCurrentPositionMs(): Long {
        if (!isPlaying) return anchorPositionMs
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = (now - anchorServerTimestampMs).coerceAtLeast(0L)
        return anchorPositionMs + elapsed
    }

    /**
     * Добавление или обновление участника при подключении.
     */
    suspend fun addOrUpdateParticipant(
        userId: Uuid,
        username: String,
        role: WatchRoomParticipantRole
    ): List<WatchRoomParticipantDto> = mutex.withLock {
        disconnectJobs.remove(userId)?.cancel()

        val existing = participants[userId]
        if (existing != null) {
            existing.isOnline = true
            existing.lastPingAt = Clock.System.now().toEpochMilliseconds()
        } else {
            participants[userId] = ActiveParticipant(
                userId = userId,
                username = username,
                role = role,
                isOnline = true,
                playbackState = WatchRoomPlaybackState.READY,
                lastPingAt = Clock.System.now().toEpochMilliseconds()
            )
        }

        val dtoList = getParticipantListLocked()
        val p = participants[userId]
        if (p != null) {
            _lobbyEvents.emit(LobbyEvent.ParticipantUpdated(p.toDto()))
        }
        _events.emit(WatchRoomEvent.ParticipantsUpdated(dtoList))
        dtoList
    }

    /**
     * Отметка об отключении участника.
     */
    suspend fun markParticipantOffline(userId: Uuid) = handleClientDisconnected(userId)

    /**
     * Полное удаление участника из комнаты (при выходе по кнопке).
     */
    suspend fun removeParticipant(userId: Uuid) = mutex.withLock {
        disconnectJobs.remove(userId)?.cancel()
        userConnections.remove(userId)
        participants.remove(userId)
        bufferingParticipants.remove(userId)

        _lobbyEvents.emit(LobbyEvent.ParticipantRemoved(userId))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))

        if (userId == hostUserId) {
            checkHostMigrationLocked()
        }
    }

    /**
     * Обработка команд управления воспроизведением.
     */
    suspend fun handleCommand(userId: Uuid, command: RoomPlaybackCommand): Boolean = mutex.withLock {
        val participant = participants[userId] ?: return@withLock false

        // Проверка прав: в режиме HOST_ONLY управлять могут только хост или команда буферизации
        if (controlMode == WatchRoomControlMode.HOST_ONLY &&
            participant.role != WatchRoomParticipantRole.HOST &&
            command !is RoomPlaybackCommand.ReportBuffer &&
            command !is RoomPlaybackCommand.SetLobbyStatus
        ) {
            logger.warn("User {} tried to issue command {} in HOST_ONLY room {}", userId, command, roomId)
            return@withLock false
        }

        when (command) {
            is RoomPlaybackCommand.Play -> {
                gracePeriodJob?.cancel()
                gracePeriodJob = null
                bufferingParticipants.clear()

                // Детерминированный запуск через 500мс в будущем
                val now = Clock.System.now().toEpochMilliseconds()
                val targetStartMs = now + 500L
                isPlaying = true
                anchorPositionMs = command.positionMs
                anchorServerTimestampMs = targetStartMs

                participant.playbackState = WatchRoomPlaybackState.PLAYING
                emitCurrentSyncState(userId)
                _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))

                onProgressChanged(currentSeason, currentEpisode, anchorPositionMs / 1000L)
                true
            }

            is RoomPlaybackCommand.Pause -> {
                gracePeriodJob?.cancel()
                gracePeriodJob = null

                val now = Clock.System.now().toEpochMilliseconds()
                isPlaying = false
                anchorPositionMs = command.positionMs
                anchorServerTimestampMs = now

                participant.playbackState = WatchRoomPlaybackState.PAUSED
                emitCurrentSyncState(userId)
                _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))

                onProgressChanged(currentSeason, currentEpisode, anchorPositionMs / 1000L)
                true
            }

            is RoomPlaybackCommand.Seek -> {
                val now = Clock.System.now().toEpochMilliseconds()
                anchorPositionMs = command.targetPositionMs

                if (isPlaying) {
                    // Запуск с новой позиции через 500мс
                    anchorServerTimestampMs = now + 500L
                } else {
                    anchorServerTimestampMs = now
                }

                emitCurrentSyncState(userId)
                onProgressChanged(currentSeason, currentEpisode, anchorPositionMs / 1000L)
                true
            }

            is RoomPlaybackCommand.ChangeEpisode -> {
                gracePeriodJob?.cancel()
                gracePeriodJob = null
                bufferingParticipants.clear()

                currentSeason = command.season
                currentEpisode = command.episode
                anchorPositionMs = 0L
                isPlaying = false
                anchorServerTimestampMs = Clock.System.now().toEpochMilliseconds()

                participant.playbackState = WatchRoomPlaybackState.READY
                emitCurrentSyncState(userId)
                _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))

                onProgressChanged(currentSeason, currentEpisode, 0L)
                true
            }

            is RoomPlaybackCommand.ReportBuffer -> {
                handleBufferReportLocked(participant, command.isBuffering)
                true
            }

            is RoomPlaybackCommand.SetLobbyStatus -> {
                participant.intent = command.intent
                participant.isReady = command.isReady
                _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
                true
            }
        }
    }

    /**
     * Обработка буферизации по алгоритму TrueSync (Grace Period 8 секунд).
     */
    private suspend fun handleBufferReportLocked(
        participant: ActiveParticipant,
        isBuffering: Boolean
    ) {
        if (isBuffering) {
            participant.playbackState = WatchRoomPlaybackState.BUFFERING
            bufferingParticipants.add(participant.userId)

            if (isPlaying && gracePeriodJob == null) {
                // Ставим воспроизведение на паузу на текущей интерполированной позиции
                val currentPos = getCurrentPositionMs()
                val now = Clock.System.now().toEpochMilliseconds()
                isPlaying = false
                anchorPositionMs = currentPos
                anchorServerTimestampMs = now

                emitCurrentSyncState(participant.userId)
                _events.emit(WatchRoomEvent.SystemNotice("Зритель ${participant.username} буферизует видео..."))

                // Запускаем таймер Grace Period на 8 секунд
                gracePeriodJob = scope.launch {
                    delay(8000L)
                    onGracePeriodExpired()
                }
            }
        } else {
            participant.playbackState = if (isPlaying) WatchRoomPlaybackState.PLAYING else WatchRoomPlaybackState.READY
            participant.isDesynced = false
            bufferingParticipants.remove(participant.userId)

            // Если все буферизовавшиеся зрители готовы и был запущен таймер ожидания
            if (bufferingParticipants.isEmpty() && gracePeriodJob != null) {
                gracePeriodJob?.cancel()
                gracePeriodJob = null

                // Возобновляем воспроизведение
                val now = Clock.System.now().toEpochMilliseconds()
                isPlaying = true
                anchorServerTimestampMs = now + 500L

                emitCurrentSyncState(null)
                _events.emit(WatchRoomEvent.SystemNotice("Все зрители готовы. Возобновляем воспроизведение!"))
            }
        }

        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
    }

    /**
     * Срабатывание 8-секундного таймера буферизации:
     * Отставшие зрители помечаются как desynced, а комната возобновляет просмотр.
     */
    private suspend fun onGracePeriodExpired() = mutex.withLock {
        if (bufferingParticipants.isEmpty()) return@withLock

        val names = bufferingParticipants.mapNotNull { participants[it]?.username }.joinToString(", ")
        bufferingParticipants.forEach { uid ->
            participants[uid]?.isDesynced = true
        }
        bufferingParticipants.clear()
        gracePeriodJob = null

        val now = Clock.System.now().toEpochMilliseconds()
        isPlaying = true
        anchorServerTimestampMs = now + 500L

        emitCurrentSyncState(null)
        _events.emit(WatchRoomEvent.SystemNotice("Возобновляем просмотр. $names сможет нагнать комнату."))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
    }

    /**
     * Отправка быстрой реакции (Danmaku).
     */
    suspend fun sendReaction(userId: Uuid, emoji: String): Boolean {
        val participant = participants[userId] ?: return false
        _events.emit(
            WatchRoomEvent.ReactionTriggered(
                userId = userId,
                username = participant.username,
                emoji = emoji
            )
        )
        return true
    }

    /**
     * Проверка и передача прав хоста (Seniority-based) при отключении текущего.
     */
    private suspend fun checkHostMigrationLocked() {
        val currentHost = participants[hostUserId]
        if (currentHost != null && currentHost.isOnline) return

        // Ищем первого доступного онлайн-участника
        val nextHost = participants.values.firstOrNull { it.isOnline && it.userId != hostUserId }
        if (nextHost != null) {
            hostUserId = nextHost.userId
            nextHost.role = WatchRoomParticipantRole.HOST
            currentHost?.role = WatchRoomParticipantRole.MEMBER

            logger.info("Host migrated in room {}: new host is {} ({})", roomId, nextHost.username, nextHost.userId)
            _events.emit(WatchRoomEvent.SystemNotice("${nextHost.username} теперь ведущий комнаты."))
            _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))

            onHostMigrated(nextHost.userId)
        }
    }

    /**
     * Возвращает список участников комнаты в виде DTO.
     */
    fun getParticipantList(): List<WatchRoomParticipantDto> {
        return participants.values.map {
            WatchRoomParticipantDto(
                userId = it.userId,
                username = it.username,
                role = it.role,
                isOnline = it.isOnline,
                playbackState = it.playbackState,
                isReady = it.isReady,
                intent = it.intent
            )
        }
    }

    private fun getParticipantListLocked(): List<WatchRoomParticipantDto> {
        return participants.values.map {
            WatchRoomParticipantDto(
                userId = it.userId,
                username = it.username,
                role = it.role,
                isOnline = it.isOnline,
                playbackState = it.playbackState,
                isReady = it.isReady,
                intent = it.intent
            )
        }
    }

    fun getCurrentSyncState(): WatchRoomEvent.SyncState {
        return WatchRoomEvent.SyncState(
            isPlaying = isPlaying,
            anchorPositionMs = anchorPositionMs,
            anchorServerTimestampMs = anchorServerTimestampMs,
            season = currentSeason,
            episode = currentEpisode,
            triggeredByUserId = null
        )
    }

    private fun emitCurrentSyncState(triggeredByUserId: Uuid?) {
        _events.tryEmit(
            WatchRoomEvent.SyncState(
                isPlaying = isPlaying,
                anchorPositionMs = anchorPositionMs,
                anchorServerTimestampMs = anchorServerTimestampMs,
                season = currentSeason,
                episode = currentEpisode,
                triggeredByUserId = triggeredByUserId
            )
        )
    }

    /**
     * Закрытие сессии: отменяет фоновые таймеры и уведомляет участников.
     */
    suspend fun close() = mutex.withLock {
        gracePeriodJob?.cancel()
        gracePeriodJob = null
        disconnectJobs.values.forEach { it.cancel() }
        disconnectJobs.clear()
        userConnections.clear()
        _events.emit(WatchRoomEvent.SystemNotice("Комната была закрыта хостом."))
        _lobbyEvents.emit(LobbyEvent.SystemNotice("Комната была закрыта хостом."))
    }
}
