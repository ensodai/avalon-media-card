package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
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
import org.ensodai.avalonmediacard.contract.model.WatchRoomPhase
import org.ensodai.avalonmediacard.contract.model.WatchRoomPlaybackState
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Модель активного участника комнаты в оперативной памяти сервера.
 */
data class ActiveParticipant(
    val userId: Uuid,
    val username: String,
    var role: WatchRoomParticipantRole,
    var isOnline: Boolean = false,
    var playbackState: WatchRoomPlaybackState = WatchRoomPlaybackState.OFFLINE,
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
 * Фазы жизненного цикла воспроизведения комнаты совместного просмотра TrueSync.
 * Алиас к контракту WatchRoomPhase для полной совместимости.
 */
typealias RoomPhase = WatchRoomPhase

/**
 * In-Memory State Machine комнаты совместного просмотра.
 * Реализует канонический алгоритм синхронизации TrueSync на базе 5-состояний FSM.
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
    val initialSourceType: String? = null,
    val initialSourceId: String? = null,
    private val scope: CoroutineScope,
    private val onProgressChanged: suspend (season: Int?, episode: Int?, positionSeconds: Long) -> Unit,
    private val onHostMigrated: suspend (newHostUserId: Uuid) -> Unit,
    val onRoomStateChanged: (roomId: Uuid) -> Unit = {}
) {
    private val logger = LoggerFactory.getLogger(WatchRoomSession::class.java)
    private val mutex = Mutex()

    var sourceType: String? = initialSourceType
        private set
    var sourceId: String? = initialSourceId
        private set

    private fun notifyRoomStateChanged() {
        try {
            onRoomStateChanged(roomId)
        } catch (e: Exception) {
            logger.warn("Error invoking onRoomStateChanged for room $roomId", e)
        }
    }

    // Текущая фаза воспроизведения FSM
    var phase: RoomPhase = RoomPhase.LOBBY
        private set(value) {
            val changed = field != value
            field = value
            if (changed) {
                if (field != RoomPhase.PLAYING_IN_SYNC) {
                    progressPersistenceJob?.cancel()
                    progressPersistenceJob = null
                }
                notifyRoomStateChanged()
            }
        }

    val isPlaying: Boolean
        get() = phase == RoomPhase.PLAYING_IN_SYNC || phase == RoomPhase.STARTING_SCHEDULED

    var anchorPositionMs: Long = initialPositionSeconds * 1000L
        private set
    var anchorServerTime: Instant = Clock.System.now()
        private set
    var currentSeason: Int? = initialSeason
        private set
    var currentEpisode: Int? = initialEpisode
        private set

    // Участники сессии
    private val participants = ConcurrentHashMap<Uuid, ActiveParticipant>()

    // Буферизующиеся участники и таймеры
    private val bufferingParticipants = ConcurrentHashMap.newKeySet<Uuid>()
    private var gracePeriodJob: Job? = null
    private var scheduledStartJob: Job? = null
    private var preparationTimeoutJob: Job? = null
    private var progressPersistenceJob: Job? = null

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
        emit(
            LobbyEvent.InitialSnapshot(
                roomStatus = WatchRoomStatus.ACTIVE,
                participants = getParticipantList(),
                sourceType = sourceType,
                sourceId = sourceId,
                season = currentSeason,
                episode = currentEpisode
            )
        )
        if (phase == RoomPhase.PLAYING_IN_SYNC || phase == RoomPhase.STARTING_SCHEDULED) {
            emit(
                LobbyEvent.TransitionToPlayer(
                    playAtServerTime = Clock.System.now(),
                    season = currentSeason,
                    episode = currentEpisode,
                    startPositionSeconds = getCurrentPositionSeconds()
                )
            )
        }
        emitAll(lobbyEvents)
    }

    /**
     * Обновление медиа-источника комнаты на лету хостом.
     */
    suspend fun updateSource(
        newSourceType: String,
        newSourceId: String,
        newSourceName: String? = null,
        newSeason: Int? = null,
        newEpisode: Int? = null
    ) = mutex.withLock {
        sourceType = newSourceType
        sourceId = newSourceId
        if (newSeason != null) currentSeason = newSeason
        if (newEpisode != null) currentEpisode = newEpisode
        _lobbyEvents.emit(
            LobbyEvent.SourceUpdated(
                sourceType = newSourceType,
                sourceId = newSourceId,
                sourceName = newSourceName,
                season = newSeason ?: currentSeason,
                episode = newEpisode ?: currentEpisode
            )
        )
        notifyRoomStateChanged()
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
            if (phase == RoomPhase.PREPARING) {
                existing.playbackState = WatchRoomPlaybackState.BUFFERING
                bufferingParticipants.add(userId)
            }
            existing
        } else {
            val initialPlaybackState = if (phase == RoomPhase.PREPARING) WatchRoomPlaybackState.BUFFERING else WatchRoomPlaybackState.READY
            val newP = ActiveParticipant(
                userId = userId,
                username = username,
                role = role,
                isOnline = true,
                playbackState = initialPlaybackState,
                lastPingAt = Clock.System.now().toEpochMilliseconds(),
                isDesynced = (role != WatchRoomParticipantRole.HOST && (phase == RoomPhase.PLAYING_IN_SYNC || phase == RoomPhase.STARTING_SCHEDULED))
            )
            if (phase == RoomPhase.PREPARING) {
                bufferingParticipants.add(userId)
            }
            participants[userId] = newP
            newP
        }

        val dto = p.toDto()
        _lobbyEvents.emit(LobbyEvent.ParticipantUpdated(dto))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
        notifyRoomStateChanged()
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
        if (phase != RoomPhase.PREPARING) {
            bufferingParticipants.remove(userId)
        }
        if (phase == RoomPhase.PREPARING) {
            checkMediaPreparationResolvedLocked()
        } else {
            checkBufferingResolvedLocked()
        }

        val dto = p.toDto()
        _lobbyEvents.emit(LobbyEvent.ParticipantUpdated(dto))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
        notifyRoomStateChanged()

        val anyOnline = participants.values.any { it.isOnline }
        if (!anyOnline && phase != RoomPhase.PREPARING) {
            autoPauseAndConserveLocked()
        }
    }

    /**
     * Автоматическая консервация состояния и сброс в LOBBY при отсутствии активных участников онлайн.
     */
    private fun autoPauseAndConserveLocked() {
        if (phase != RoomPhase.LOBBY) {
            val currentPosMs = getCurrentPositionMs()
            anchorPositionMs = currentPosMs
            anchorServerTime = Clock.System.now()
            scheduledStartJob?.cancel()
            scheduledStartJob = null
            gracePeriodJob?.cancel()
            gracePeriodJob = null
            preparationTimeoutJob?.cancel()
            preparationTimeoutJob = null
            bufferingParticipants.clear()
            phase = RoomPhase.LOBBY
            logger.info(
                "[TrueSync:Server] Room {}: All participants disconnected. Auto-pausing at {} ms and conserving state in LOBBY.",
                roomId, anchorPositionMs
            )
            scope.launch {
                onProgressChanged(currentSeason, currentEpisode, anchorPositionMs / 1000L)
            }
            emitCurrentSyncState(null)
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
     * Переводит комнату в фазу PREPARING (барьер готовности), чтобы все участники
     * успели открыть плеер, получить URL и демультиплексировать видеопоток перед синхронным стартом.
     */
    suspend fun triggerStartPlayback(userId: Uuid): Boolean = mutex.withLock {
        val p = participants[userId] ?: return@withLock false
        if (p.role != WatchRoomParticipantRole.HOST && controlMode == WatchRoomControlMode.HOST_ONLY) {
            logger.warn("Non-host {} tried to trigger start playback in room {}", userId, roomId)
            return@withLock false
        }

        scheduledStartJob?.cancel()
        scheduledStartJob = null
        gracePeriodJob?.cancel()
        gracePeriodJob = null
        preparationTimeoutJob?.cancel()
        preparationTimeoutJob = null

        phase = RoomPhase.PREPARING
        if (anchorPositionMs < 0L) {
            anchorPositionMs = 0L
        }
        anchorServerTime = Clock.System.now()
        bufferingParticipants.clear()

        // Все активные онлайн-участники ожидают подготовки медиа
        participants.values.forEach { participant ->
            if (participant.isOnline) {
                participant.playbackState = WatchRoomPlaybackState.BUFFERING
                participant.isDesynced = false
                bufferingParticipants.add(participant.userId)
            }
        }

        val targetStartPlaceholder = Clock.System.now()
        _lobbyEvents.emit(
            LobbyEvent.TransitionToPlayer(
                playAtServerTime = targetStartPlaceholder,
                season = currentSeason,
                episode = currentEpisode,
                startPositionSeconds = anchorPositionMs / 1000L
            )
        )

        emitCurrentSyncState(userId)
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
        _events.emit(WatchRoomEvent.SystemNotice("Подготовка к совместному просмотру... Ожидание загрузки видео всеми участниками."))

        startPreparationTimeoutLocked()
        onProgressChanged(currentSeason, currentEpisode, anchorPositionMs / 1000L)
        true
    }

    /**
     * Возвращает текущее интерполированное время воспроизведения в миллисекундах.
     * Время идет только в фазе PLAYING_IN_SYNC. В фазе STARTING_SCHEDULED (преролл)
     * время зафиксировано на anchorPositionMs.
     */
    fun getCurrentPositionMs(): Long {
        if (phase != RoomPhase.PLAYING_IN_SYNC) return anchorPositionMs
        val now = Clock.System.now()
        val elapsed = (now - anchorServerTime).inWholeMilliseconds.coerceAtLeast(0L)
        return anchorPositionMs + elapsed
    }

    fun isActivelyPlaying(): Boolean =
        (phase == RoomPhase.PLAYING_IN_SYNC || phase == RoomPhase.STARTING_SCHEDULED || phase == RoomPhase.PARTIAL_BUFFERING) &&
                participants.values.any { it.isOnline }

    fun getOnlineCount(): Int = participants.values.count { it.isOnline }

    fun getCurrentPositionSeconds(): Long = getCurrentPositionMs() / 1000L

    /**
     * Добавление или обновление участника при подключении.
     */
    suspend fun addOrUpdateParticipant(
        userId: Uuid,
        username: String,
        role: WatchRoomParticipantRole,
        isOnline: Boolean = false
    ): List<WatchRoomParticipantDto> = mutex.withLock {
        disconnectJobs.remove(userId)?.cancel()

        val existing = participants[userId]
        if (existing != null) {
            existing.role = role
            val hasActiveConnection = userConnections[userId]?.isNotEmpty() == true
            existing.isOnline = hasActiveConnection || isOnline
            existing.lastPingAt = Clock.System.now().toEpochMilliseconds()
        } else {
            participants[userId] = ActiveParticipant(
                userId = userId,
                username = username,
                role = role,
                isOnline = isOnline,
                playbackState = if (isOnline) WatchRoomPlaybackState.READY else WatchRoomPlaybackState.OFFLINE,
                lastPingAt = Clock.System.now().toEpochMilliseconds()
            )
        }

        val dtoList = getParticipantListLocked()
        val p = participants[userId]
        if (p != null) {
            _lobbyEvents.emit(LobbyEvent.ParticipantUpdated(p.toDto()))
        }
        _events.emit(WatchRoomEvent.ParticipantsUpdated(dtoList))
        notifyRoomStateChanged()
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
        if (phase == RoomPhase.PREPARING) {
            checkMediaPreparationResolvedLocked()
        } else {
            checkBufferingResolvedLocked()
        }

        _lobbyEvents.emit(LobbyEvent.ParticipantRemoved(userId))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
        notifyRoomStateChanged()

        if (userId == hostUserId) {
            checkHostMigrationLocked()
        }

        val anyOnline = participants.values.any { it.isOnline }
        if (!anyOnline) {
            autoPauseAndConserveLocked()
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
            command !is RoomPlaybackCommand.ReportMediaReady &&
            command !is RoomPlaybackCommand.SetLobbyStatus
        ) {
            logger.warn("User {} tried to issue command {} in HOST_ONLY room {}", userId, command, roomId)
            return@withLock false
        }

        when (command) {
            is RoomPlaybackCommand.Play -> {
                bufferingParticipants.clear()
                val now = Clock.System.now()
                val leadTimeMs = if (phase == RoomPhase.LOBBY || phase == RoomPhase.PREPARING) START_LEAD_TIME_MS else RESUME_LEAD_TIME_MS
                val targetStart = now + leadTimeMs.milliseconds
                logger.info(
                    "[TrueSync:Server] Room {}: Received PLAY from user {} at pos {} ms. ServerNow={}, targetStart={} (+{}ms)",
                    roomId, userId, command.positionMs, now, targetStart, leadTimeMs
                )

                participant.playbackState = WatchRoomPlaybackState.PLAYING
                participant.isDesynced = false
                schedulePrerollTransitionLocked(targetStart, command.positionMs, userId)
                _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))

                onProgressChanged(currentSeason, currentEpisode, command.positionMs / 1000L)
                true
            }

            is RoomPlaybackCommand.Pause -> {
                scheduledStartJob?.cancel()
                scheduledStartJob = null
                gracePeriodJob?.cancel()
                gracePeriodJob = null
                preparationTimeoutJob?.cancel()
                preparationTimeoutJob = null

                val now = Clock.System.now()
                val pausePos = if (phase == RoomPhase.PLAYING_IN_SYNC) getCurrentPositionMs() else command.positionMs
                phase = RoomPhase.FORCE_PAUSED
                anchorPositionMs = pausePos
                anchorServerTime = now

                logger.info(
                    "[TrueSync:Server] Room {}: Received PAUSE from user {} at pos {} ms (savedPos={} ms). ServerNow={}",
                    roomId, userId, command.positionMs, pausePos, now
                )

                participant.playbackState = WatchRoomPlaybackState.PAUSED
                participant.isDesynced = false
                emitCurrentSyncState(userId)
                _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))

                onProgressChanged(currentSeason, currentEpisode, anchorPositionMs / 1000L)
                true
            }

            is RoomPlaybackCommand.Seek -> {
                participant.isDesynced = false
                val now = Clock.System.now()
                if (isPlaying) {
                    val targetStart = now + 2000.milliseconds
                    logger.info(
                        "[TrueSync:Server] Room {}: Received SEEK while playing from user {} to {} ms. Scheduled preroll targetStart={}",
                        roomId, userId, command.targetPositionMs, targetStart
                    )
                    bufferingParticipants.clear()
                    participants.values.forEach { p ->
                        if (p.isOnline && !p.isDesynced) {
                            p.playbackState = WatchRoomPlaybackState.BUFFERING
                            bufferingParticipants.add(p.userId)
                        }
                    }
                    _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
                    schedulePrerollTransitionLocked(targetStart, command.targetPositionMs, userId)
                } else {
                    scheduledStartJob?.cancel()
                    scheduledStartJob = null
                    gracePeriodJob?.cancel()
                    gracePeriodJob = null
                    preparationTimeoutJob?.cancel()
                    preparationTimeoutJob = null
                    anchorPositionMs = command.targetPositionMs
                    anchorServerTime = now
                    logger.info(
                        "[TrueSync:Server] Room {}: Received SEEK while paused from user {} to {} ms.",
                        roomId, userId, command.targetPositionMs
                    )
                    emitCurrentSyncState(userId)
                }
                onProgressChanged(currentSeason, currentEpisode, command.targetPositionMs / 1000L)
                true
            }

            is RoomPlaybackCommand.ChangeEpisode -> {
                scheduledStartJob?.cancel()
                scheduledStartJob = null
                gracePeriodJob?.cancel()
                gracePeriodJob = null
                preparationTimeoutJob?.cancel()
                preparationTimeoutJob = null
                bufferingParticipants.clear()

                currentSeason = command.season
                currentEpisode = command.episode
                anchorPositionMs = 0L
                phase = RoomPhase.PREPARING
                anchorServerTime = Clock.System.now()

                participants.values.forEach { p ->
                    if (p.isOnline) {
                        p.playbackState = WatchRoomPlaybackState.BUFFERING
                        p.isDesynced = false
                        bufferingParticipants.add(p.userId)
                    }
                }

                logger.info(
                    "[TrueSync:Server] Room {}: Received ChangeEpisode S{}E{} from user {}. Entering PREPARING.",
                    roomId, command.season, command.episode, userId
                )

                emitCurrentSyncState(userId)
                _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
                _events.emit(WatchRoomEvent.SystemNotice("Смена серии (S${command.season}E${command.episode}). Загрузка видео всеми участниками..."))

                startPreparationTimeoutLocked()
                onProgressChanged(currentSeason, currentEpisode, 0L)
                true
            }

            is RoomPlaybackCommand.ReportBuffer -> {
                handleBufferReportLocked(participant, command.isBuffering)
                true
            }

            is RoomPlaybackCommand.ReportMediaReady -> {
                handleMediaReadyReportLocked(participant, command.positionMs)
                true
            }

            is RoomPlaybackCommand.SetLobbyStatus -> {
                participant.intent = command.intent
                participant.isReady = command.isReady
                _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
                true
            }

            is RoomPlaybackCommand.ReturnToLobby -> {
                scheduledStartJob?.cancel()
                scheduledStartJob = null
                gracePeriodJob?.cancel()
                gracePeriodJob = null
                preparationTimeoutJob?.cancel()
                preparationTimeoutJob = null
                bufferingParticipants.clear()

                val now = Clock.System.now()
                val currentPos = if (phase == RoomPhase.PLAYING_IN_SYNC) getCurrentPositionMs() else anchorPositionMs
                phase = RoomPhase.LOBBY
                anchorPositionMs = currentPos
                anchorServerTime = now

                logger.info(
                    "[TrueSync:Server] Room {}: Received ReturnToLobby from user {}. Conserved pos={} ms.",
                    roomId, userId, currentPos
                )

                participants.values.forEach { p ->
                    if (p.isOnline) {
                        p.playbackState = WatchRoomPlaybackState.READY
                    }
                }

                _events.emit(WatchRoomEvent.ReturnedToLobby)
                _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
                _lobbyEvents.emit(LobbyEvent.InitialSnapshot(WatchRoomStatus.ACTIVE, getParticipantListLocked()))

                onProgressChanged(currentSeason, currentEpisode, anchorPositionMs / 1000L)
                true
            }
        }
    }

    /**
     * Планирование детерминированного перехода в PLAYING_IN_SYNC через фазу STARTING_SCHEDULED (Preroll).
     * Во время Preroll виртуальные часы стоят на месте, а отчеты о буферизации не вызывают паузу.
     */
    private fun schedulePrerollTransitionLocked(
        targetStart: Instant,
        targetPosMs: Long,
        causedByUserId: Uuid? = null
    ) {
        scheduledStartJob?.cancel()
        gracePeriodJob?.cancel()
        gracePeriodJob = null

        phase = RoomPhase.STARTING_SCHEDULED
        anchorPositionMs = targetPosMs
        anchorServerTime = targetStart

        emitCurrentSyncState(causedByUserId)

        scheduledStartJob = scope.launch {
            val waitDuration = targetStart - Clock.System.now()
            if (waitDuration.isPositive()) {
                delay(waitDuration)
            }
            mutex.withLock {
                if (phase != RoomPhase.STARTING_SCHEDULED) return@withLock

                val activeBuffering = bufferingParticipants.filter { uid ->
                    val p = participants[uid]
                    p != null && p.isOnline && !p.isDesynced
                }

                if (activeBuffering.isEmpty()) {
                    phase = RoomPhase.PLAYING_IN_SYNC
                    anchorServerTime = targetStart
                    startPeriodicProgressPersistenceLocked()
                    logger.info("[TrueSync:Server] Room {}: STARTING_SCHEDULED -> PLAYING_IN_SYNC at {}", roomId, targetStart)
                    participants.values.forEach { p ->
                        if (p.isOnline && !p.isDesynced) {
                            p.playbackState = WatchRoomPlaybackState.PLAYING
                        }
                    }
                    emitCurrentSyncState(null)
                    _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
                } else {
                    phase = RoomPhase.PARTIAL_BUFFERING
                    val names = activeBuffering.mapNotNull { participants[it]?.username }.joinToString(", ")
                    logger.warn("[TrueSync:Server] Room {}: STARTING_SCHEDULED -> PARTIAL_BUFFERING. Participants still buffering: {}", roomId, names)

                    anchorPositionMs = targetPosMs
                    anchorServerTime = Clock.System.now()
                    emitCurrentSyncState(null)
                    _events.emit(WatchRoomEvent.SystemNotice("Зритель $names буферизует видео..."))

                    startGracePeriodTimerLocked()
                }
            }
        }
    }

    /**
     * Обработка буферизации по алгоритму TrueSync на основе 5-состояний FSM.
     */
    private suspend fun handleBufferReportLocked(
        participant: ActiveParticipant,
        isBuffering: Boolean
    ) {
        if (isBuffering) {
            participant.playbackState = WatchRoomPlaybackState.BUFFERING
            bufferingParticipants.add(participant.userId)

            when (phase) {
                RoomPhase.PREPARING -> {
                    // Во время подготовки медиа буферизация ожидаема
                    logger.debug("[TrueSync:Server] Room {}: Participant {} buffering during PREPARING.", roomId, participant.userId)
                }
                RoomPhase.STARTING_SCHEDULED -> {
                    // Во время преролла буферизация нормальна: идет загрузка чанков в MSE.
                    // Паузу НЕ вызываем, участник учтен в bufferingParticipants.
                    logger.debug("[TrueSync:Server] Room {}: Participant {} buffering during STARTING_SCHEDULED (preroll). Recorded.", roomId, participant.userId)
                }
                RoomPhase.PLAYING_IN_SYNC -> {
                    // Аварийный останов посреди активного просмотра
                    if (!participant.isDesynced || participant.role == WatchRoomParticipantRole.HOST) {
                        val currentPos = getCurrentPositionMs()
                        val now = Clock.System.now()
                        logger.warn("[TrueSync:Server] Room {}: Participant {} stalled during PLAYING_IN_SYNC at {} ms. Entering PARTIAL_BUFFERING.", roomId, participant.userId, currentPos)

                        scheduledStartJob?.cancel()
                        scheduledStartJob = null
                        phase = RoomPhase.PARTIAL_BUFFERING
                        anchorPositionMs = currentPos
                        anchorServerTime = now

                        emitCurrentSyncState(participant.userId)
                        val names = bufferingParticipants.mapNotNull { participants[it]?.username }.joinToString(", ")
                        _events.emit(WatchRoomEvent.SystemNotice("Зритель $names буферизует видео..."))

                        startGracePeriodTimerLocked()
                    }
                }
                RoomPhase.PARTIAL_BUFFERING,
                RoomPhase.FORCE_PAUSED,
                RoomPhase.LOBBY -> {
                    // Уже на паузе или в лобби
                }
            }
        } else {
            participant.playbackState = when (phase) {
                RoomPhase.PLAYING_IN_SYNC -> WatchRoomPlaybackState.PLAYING
                RoomPhase.FORCE_PAUSED -> WatchRoomPlaybackState.PAUSED
                else -> WatchRoomPlaybackState.READY
            }
            participant.isDesynced = false
            bufferingParticipants.remove(participant.userId)

            if (phase == RoomPhase.PREPARING) {
                checkMediaPreparationResolvedLocked()
            } else {
                checkBufferingResolvedLocked()
            }
        }

        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
    }

    private suspend fun handleMediaReadyReportLocked(
        participant: ActiveParticipant,
        positionMs: Long
    ) {
        participant.playbackState = when (phase) {
            RoomPhase.PLAYING_IN_SYNC -> WatchRoomPlaybackState.PLAYING
            RoomPhase.FORCE_PAUSED -> WatchRoomPlaybackState.PAUSED
            else -> WatchRoomPlaybackState.READY
        }
        participant.isDesynced = false
        bufferingParticipants.remove(participant.userId)
        logger.info("[TrueSync:Server] Room {}: Participant {} reported MEDIA READY at pos {} ms. Phase={}", roomId, participant.userId, positionMs, phase)

        if (phase == RoomPhase.PREPARING) {
            checkMediaPreparationResolvedLocked()
        } else {
            checkBufferingResolvedLocked()
        }
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
    }

    private suspend fun checkMediaPreparationResolvedLocked() {
        if (phase != RoomPhase.PREPARING) return

        val anyOnline = participants.values.any { it.isOnline }
        val allReady = bufferingParticipants.isEmpty()

        if (anyOnline && allReady) {
            preparationTimeoutJob?.cancel()
            preparationTimeoutJob = null

            val targetStart = Clock.System.now() + 1500.milliseconds
            logger.info("[TrueSync:Server] Room {}: All participants READY in PREPARING phase! Scheduling synchronized start at {}", roomId, targetStart)
            schedulePrerollTransitionLocked(targetStart, anchorPositionMs, null)
            _events.emit(WatchRoomEvent.SystemNotice("Все участники готовы! Старт воспроизведения через 1.5 секунды."))
            _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
        } else {
            val total = participants.values.count { !it.isDesynced }
            val readyCount = (total - bufferingParticipants.size).coerceAtLeast(0)
            val unreadyNames = bufferingParticipants.mapNotNull { participants[it]?.username }.joinToString(", ")
            logger.info("[TrueSync:Server] Room {}: Media preparation in progress: {}/{} ready. Waiting for: {}", roomId, readyCount, total, unreadyNames)
            _events.emit(WatchRoomEvent.SystemNotice("Подготовка видео: $readyCount/$total готовы ($unreadyNames загружает...)"))
            _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
        }
    }

    private fun startPreparationTimeoutLocked() {
        preparationTimeoutJob?.cancel()
        preparationTimeoutJob = scope.launch {
            delay(PREPARATION_TIMEOUT_MS)
            onPreparationTimeoutExpired()
        }
    }

    private suspend fun onPreparationTimeoutExpired() = mutex.withLock {
        if (phase != RoomPhase.PREPARING) return@withLock

        val anyOnline = participants.values.any { it.isOnline }
        if (!anyOnline) {
            autoPauseAndConserveLocked()
            return@withLock
        }

        val unready = bufferingParticipants.filter { uid ->
            val p = participants[uid]
            p != null && !p.isDesynced
        }
        if (unready.isEmpty()) return@withLock

        val names = unready.mapNotNull { participants[it]?.username }.joinToString(", ")
        logger.warn("[TrueSync:Server] Room {}: Preparation watchdog timeout ({}s) expired for: {}. Marking desynced.", roomId, PREPARATION_TIMEOUT_MS / 1000, names)
        unready.forEach { uid ->
            participants[uid]?.isDesynced = true
        }
        bufferingParticipants.clear()
        preparationTimeoutJob = null

        val targetStart = Clock.System.now() + START_LEAD_TIME_MS.milliseconds
        schedulePrerollTransitionLocked(targetStart, anchorPositionMs, null)
        _events.emit(WatchRoomEvent.SystemNotice("Старт воспроизведения. $names продолжит просмотр после завершения загрузки."))
        _events.emit(WatchRoomEvent.ParticipantsUpdated(getParticipantListLocked()))
    }

    private suspend fun checkBufferingResolvedLocked() {
        if (phase == RoomPhase.PARTIAL_BUFFERING) {
            val activeBuffering = bufferingParticipants.filter { uid ->
                val p = participants[uid]
                p != null && p.isOnline && !p.isDesynced
            }
            if (activeBuffering.isEmpty()) {
                logger.info("[TrueSync:Server] Room {}: All buffering participants ready in PARTIAL_BUFFERING. Resuming playback!", roomId)
                gracePeriodJob?.cancel()
                gracePeriodJob = null
                bufferingParticipants.clear()

                val targetStart = Clock.System.now() + RESUME_LEAD_TIME_MS.milliseconds
                schedulePrerollTransitionLocked(targetStart, anchorPositionMs, null)
                _events.emit(WatchRoomEvent.SystemNotice("Все зрители готовы. Возобновляем воспроизведение!"))
            }
        }
    }

    private fun startGracePeriodTimerLocked() {
        gracePeriodJob?.cancel()
        gracePeriodJob = scope.launch {
            delay(GRACE_PERIOD_MS)
            onGracePeriodExpired()
        }
    }

    /**
     * Срабатывание 8-секундного таймера буферизации:
     * Отставшие зрители помечаются как desynced, а комната возобновляет просмотр (+500мс lead time).
     */
    private suspend fun onGracePeriodExpired() = mutex.withLock {
        if (phase != RoomPhase.PARTIAL_BUFFERING) return@withLock

        val activeBuffering = bufferingParticipants.filter { uid ->
            val p = participants[uid]
            p != null && p.isOnline && !p.isDesynced
        }
        if (activeBuffering.isEmpty()) return@withLock

        val hostIsBuffering = activeBuffering.any { participants[it]?.role == WatchRoomParticipantRole.HOST }
        if (hostIsBuffering) {
            val hostName = activeBuffering
                .mapNotNull { participants[it] }
                .firstOrNull { it.role == WatchRoomParticipantRole.HOST }?.username ?: "Хост"
            logger.warn("[TrueSync:Server] Room {}: Host ({}) is still buffering after grace period. Holding room in pause.", roomId, hostName)
            _events.emit(WatchRoomEvent.SystemNotice("Ожидаем хоста ($hostName): загрузка видео продолжается..."))
            startGracePeriodTimerLocked()
            return@withLock
        }

        val names = activeBuffering.mapNotNull { participants[it]?.username }.joinToString(", ")
        logger.warn("[TrueSync:Server] Room {}: Grace period expired for participants: {}. Marking desynced.", roomId, names)
        activeBuffering.forEach { uid ->
            participants[uid]?.isDesynced = true
        }
        bufferingParticipants.clear()
        gracePeriodJob = null

        val targetStart = Clock.System.now() + 500.milliseconds
        schedulePrerollTransitionLocked(targetStart, anchorPositionMs, null)
        _events.emit(WatchRoomEvent.SystemNotice("Возобновляем воспроизведение. $names продолжит просмотр после завершения загрузки."))
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
            anchorServerTime = anchorServerTime,
            season = currentSeason,
            episode = currentEpisode,
            triggeredByUserId = null
        )
    }

    private fun emitCurrentSyncState(triggeredByUserId: Uuid?) {
        val state = WatchRoomEvent.SyncState(
            isPlaying = isPlaying,
            anchorPositionMs = anchorPositionMs,
            anchorServerTime = anchorServerTime,
            season = currentSeason,
            episode = currentEpisode,
            triggeredByUserId = triggeredByUserId
        )
        logger.info(
            "[TrueSync:Server] Room {}: Emitting SyncState: isPlaying={}, anchorPositionMs={} ms, anchorServerTime={}, triggeredBy={}",
            roomId, isPlaying, anchorPositionMs, anchorServerTime, triggeredByUserId
        )
        _events.tryEmit(state)
    }

    private fun startPeriodicProgressPersistenceLocked() {
        progressPersistenceJob?.cancel()
        progressPersistenceJob = scope.launch {
            while (isActive) {
                delay(5000L)
                mutex.withLock {
                    if (phase == RoomPhase.PLAYING_IN_SYNC) {
                        val posSeconds = getCurrentPositionSeconds()
                        onProgressChanged(currentSeason, currentEpisode, posSeconds)
                    }
                }
            }
        }
    }

    /**
     * Закрытие сессии: отменяет фоновые таймеры и уведомляет участников.
     */
    suspend fun close() = mutex.withLock {
        progressPersistenceJob?.cancel()
        progressPersistenceJob = null
        scheduledStartJob?.cancel()
        scheduledStartJob = null
        gracePeriodJob?.cancel()
        gracePeriodJob = null
        preparationTimeoutJob?.cancel()
        preparationTimeoutJob = null
        disconnectJobs.values.forEach { it.cancel() }
        disconnectJobs.clear()
        userConnections.clear()
        _events.emit(WatchRoomEvent.SystemNotice("Комната была закрыта хостом."))
        _lobbyEvents.emit(LobbyEvent.SystemNotice("Комната была закрыта хостом."))
    }

    companion object {
        const val PREPARATION_TIMEOUT_MS = 120_000L
        const val GRACE_PERIOD_MS = 10_000L
        const val START_LEAD_TIME_MS = 1500L
        const val SEEK_LEAD_TIME_MS = 2000L
        const val RESUME_LEAD_TIME_MS = 500L
    }
}
