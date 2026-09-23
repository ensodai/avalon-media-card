package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPlaybackState
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
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
)

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

    // Реактивная шина событий комнаты
    private val _events = MutableSharedFlow<WatchRoomEvent>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: Flow<WatchRoomEvent> = _events.asSharedFlow()

    init {
        // Эмитим начальный слепок состояния
        emitCurrentSyncState(null)
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
        _events.emit(WatchRoomEvent.ParticipantsUpdated(dtoList))
        dtoList
    }

    /**
     * Отметка об отключении участника.
     */
    suspend fun markParticipantOffline(userId: Uuid) = mutex.withLock {
        val p = participants[userId] ?: return@withLock
        p.isOnline = false
        p.playbackState = WatchRoomPlaybackState.OFFLINE
        bufferingParticipants.remove(userId)

        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))

        // Если отключился хост, запускаем таймер миграции прав
        if (userId == hostUserId) {
            checkHostMigrationLocked()
        }
    }

    /**
     * Полное удаление участника из комнаты (при выходе по кнопке).
     */
    suspend fun removeParticipant(userId: Uuid) = mutex.withLock {
        participants.remove(userId)
        bufferingParticipants.remove(userId)

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
        _events.emit(WatchRoomEvent.SystemNotice("Комната была закрыта хостом."))
    }
}
