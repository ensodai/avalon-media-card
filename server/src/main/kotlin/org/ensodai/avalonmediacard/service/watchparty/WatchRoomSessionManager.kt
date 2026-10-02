package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.model.WatchRoomChatMessageDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPhase
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.repository.UserEpisodeRepository
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.ensodai.avalonmediacard.repository.watchparty.WatchRoomRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

/**
 * Синглтон-сервис управления активными сессиями комнат совместного просмотра в памяти сервера.
 */
@Single
class WatchRoomSessionManager(
    private val watchRoomRepository: WatchRoomRepository,
    private val userMovieRepository: UserMovieRepository,
    private val userEpisodeRepository: UserEpisodeRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val logger = LoggerFactory.getLogger(WatchRoomSessionManager::class.java)

    // Активные сессии комнат в оперативной памяти
    private val sessions = ConcurrentHashMap<Uuid, WatchRoomSession>()

    // Шина оповещений об изменениях в комнатах для реактивного стриминга
    private val _roomUpdatesFlow = MutableSharedFlow<Uuid>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val roomUpdatesFlow: Flow<Uuid> = _roomUpdatesFlow.asSharedFlow()

    fun notifyRoomUpdated(roomId: Uuid) {
        _roomUpdatesFlow.tryEmit(roomId)
    }

    /**
     * Получение существующей или восстановление сессии комнаты из БД.
     */
    suspend fun getOrCreateSession(roomId: Uuid): WatchRoomSession? {
        val existing = sessions[roomId]
        if (existing != null) return existing

        val roomDto = watchRoomRepository.findRoomById(roomId) ?: return null
        if (roomDto.status == WatchRoomStatus.ARCHIVED) return null

        val newSession = WatchRoomSession(
            roomId = roomDto.id,
            mediaId = roomDto.mediaId,
            title = roomDto.title,
            hostUserId = roomDto.hostUserId,
            controlMode = roomDto.controlMode,
            initialSeason = roomDto.currentSeason,
            initialEpisode = roomDto.currentEpisode,
            initialPositionSeconds = roomDto.lastPositionSeconds,
            initialSourceType = roomDto.sourceType,
            initialSourceId = roomDto.sourceId,
            scope = scope,
            onProgressChanged = { season, episode, posSec ->
                watchRoomRepository.updateRoomProgress(roomDto.id, season, episode, posSec)
                syncProgressToParticipants(
                    roomId = roomDto.id,
                    mediaId = roomDto.mediaId,
                    mediaType = roomDto.mediaType,
                    season = season,
                    episode = episode,
                    posSec = posSec
                )
            },
            onHostMigrated = { newHostId ->
                watchRoomRepository.transferHost(roomDto.id, newHostId)
            },
            onRoomStateChanged = { updatedRoomId -> notifyRoomUpdated(updatedRoomId) }
        )

        // Загружаем постоянных участников комнаты из БД строго со статусом оффлайн
        roomDto.participants.forEach { p ->
            newSession.addOrUpdateParticipant(
                userId = p.userId,
                username = p.username,
                role = p.role,
                isOnline = false
            )
        }

        sessions.putIfAbsent(roomId, newSession)
        return sessions[roomId] ?: newSession
    }

    /**
     * Регистрация только что созданной комнаты.
     */
    suspend fun registerNewRoom(roomDto: WatchRoomDto, hostUser: AuthState.Authorized): WatchRoomSession {
        val session = WatchRoomSession(
            roomId = roomDto.id,
            mediaId = roomDto.mediaId,
            title = roomDto.title,
            hostUserId = hostUser.userId,
            controlMode = roomDto.controlMode,
            initialSeason = roomDto.currentSeason,
            initialEpisode = roomDto.currentEpisode,
            initialPositionSeconds = roomDto.lastPositionSeconds,
            initialSourceType = roomDto.sourceType,
            initialSourceId = roomDto.sourceId,
            scope = scope,
            onProgressChanged = { season, episode, posSec ->
                watchRoomRepository.updateRoomProgress(roomDto.id, season, episode, posSec)
                syncProgressToParticipants(
                    roomId = roomDto.id,
                    mediaId = roomDto.mediaId,
                    mediaType = roomDto.mediaType,
                    season = season,
                    episode = episode,
                    posSec = posSec
                )
            },
            onHostMigrated = { newHostId ->
                watchRoomRepository.transferHost(roomDto.id, newHostId)
            },
            onRoomStateChanged = { updatedRoomId -> notifyRoomUpdated(updatedRoomId) }
        )

        session.addOrUpdateParticipant(
            userId = hostUser.userId,
            username = hostUser.username,
            role = WatchRoomParticipantRole.HOST,
            isOnline = false
        )

        sessions[roomDto.id] = session
        notifyRoomUpdated(roomDto.id)
        return session
    }

    /**
     * Получение активной сессии комнаты из памяти (если существует).
     */
    fun getActiveSession(roomId: Uuid): WatchRoomSession? = sessions[roomId]

    /**
     * Вход пользователя в комнату.
     */
    suspend fun joinRoom(roomId: Uuid, user: AuthState.Authorized): WatchRoomDto? {
        val session = getOrCreateSession(roomId) ?: return null

        val isHost = session.hostUserId == user.userId
        val role = if (isHost) WatchRoomParticipantRole.HOST else WatchRoomParticipantRole.MEMBER

        session.addOrUpdateParticipant(user.userId, user.username, role, isOnline = false)
        watchRoomRepository.addParticipant(roomId, user.userId, role)
        notifyRoomUpdated(roomId)

        val dbRoom = watchRoomRepository.findRoomById(roomId) ?: return null
        return dbRoom.copy(
            joinPin = if (isHost) dbRoom.joinPin else null,
            participants = session.getParticipantList(),
            phase = session.phase,
            sourceType = session.sourceType ?: dbRoom.sourceType,
            sourceId = session.sourceId ?: dbRoom.sourceId,
            currentSeason = session.currentSeason ?: dbRoom.currentSeason,
            currentEpisode = session.currentEpisode ?: dbRoom.currentEpisode,
            lastPositionSeconds = session.getCurrentPositionSeconds()
        )
    }

    /**
     * Выход пользователя из комнаты.
     */
    suspend fun leaveRoom(roomId: Uuid, userId: Uuid): Boolean {
        val session = sessions[roomId]
        if (session != null) {
            session.removeParticipant(userId)
        }
        watchRoomRepository.removeParticipant(roomId, userId)
        notifyRoomUpdated(roomId)
        return true
    }

    /**
     * Закрытие комнаты хостом.
     */
    suspend fun closeRoom(roomId: Uuid, hostUserId: Uuid): Boolean {
        val session = sessions[roomId] ?: return false
        if (session.hostUserId != hostUserId) {
            logger.warn("User {} tried to close room {} without host privileges", hostUserId, roomId)
            return false
        }

        session.close()
        sessions.remove(roomId)
        watchRoomRepository.updateRoomStatus(roomId, WatchRoomStatus.ARCHIVED)
        notifyRoomUpdated(roomId)
        return true
    }

    /**
     * Смена медиа-источника комнаты хостом.
     */
    suspend fun updateRoomSource(
        roomId: Uuid,
        userId: Uuid,
        sourceType: String,
        sourceId: String,
        sourceName: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): Boolean {
        val session = sessions[roomId] ?: getOrCreateSession(roomId) ?: return false
        if (session.hostUserId != userId) {
            logger.warn("User {} tried to update source for room {} without host privileges", userId, roomId)
            return false
        }
        val dbUpdated = watchRoomRepository.updateRoomSource(roomId, sourceType, sourceId, season, episode)
        if (!dbUpdated) return false

        session.updateSource(sourceType, sourceId, sourceName, season, episode)
        notifyRoomUpdated(roomId)
        return true
    }

    suspend fun getUserRooms(userId: Uuid): List<WatchRoomSummaryDto> {
        val dbRooms = watchRoomRepository.getRoomsForUser(userId)
        return enrichRoomsWithLiveSessionState(dbRooms)
    }

    suspend fun getSavedRoomsForMedia(mediaId: String, currentUserId: Uuid): List<WatchRoomSummaryDto> {
        val dbRooms = watchRoomRepository.getRoomsForMedia(mediaId, currentUserId)
        return enrichRoomsWithLiveSessionState(dbRooms)
    }

    fun enrichRoomsWithLiveSessionState(rooms: List<WatchRoomSummaryDto>): List<WatchRoomSummaryDto> {
        return rooms.map { room ->
            val session = sessions[room.id]
            if (session != null) {
                val sessionPhase = session.phase
                val participants = session.getParticipantList()
                val posSec = session.getCurrentPositionSeconds()
                room.copy(
                    phase = sessionPhase,
                    participants = participants,
                    lastPositionSeconds = posSec,
                    sourceType = session.sourceType ?: room.sourceType,
                    sourceId = session.sourceId ?: room.sourceId,
                    currentSeason = session.currentSeason ?: room.currentSeason,
                    currentEpisode = session.currentEpisode ?: room.currentEpisode,
                    status = if (sessionPhase == WatchRoomPhase.PLAYING_IN_SYNC) WatchRoomStatus.ACTIVE else WatchRoomStatus.PAUSED
                )
            } else {
                room.copy(
                    phase = WatchRoomPhase.LOBBY
                )
            }
        }
    }

    /**
     * Реактивный поток актуального списка комнат пользователя.
     * При подключении отдает текущий слепок, а затем пушит свежий список при любых событиях комнат.
     */
    @OptIn(FlowPreview::class)
    fun streamUserRooms(userId: Uuid): Flow<List<WatchRoomSummaryDto>> = flow {
        emit(getUserRooms(userId))
        _roomUpdatesFlow
            .debounce(100.milliseconds)
            .collect {
                emit(getUserRooms(userId))
            }
    }

    /**
     * Реактивный поток актуального списка сохраненных комнат для конкретного тайтла.
     */
    @OptIn(FlowPreview::class)
    fun streamSavedRoomsForMedia(mediaId: String, currentUserId: Uuid): Flow<List<WatchRoomSummaryDto>> = flow {
        emit(getSavedRoomsForMedia(mediaId, currentUserId))
        _roomUpdatesFlow
            .debounce(100.milliseconds)
            .collect {
                emit(getSavedRoomsForMedia(mediaId, currentUserId))
            }
    }

    /**
     * Отправка команды управления воспроизведением.
     */
    suspend fun sendPlaybackCommand(
        roomId: Uuid,
        userId: Uuid,
        command: RoomPlaybackCommand
    ): Boolean {
        val session = sessions[roomId] ?: getOrCreateSession(roomId) ?: return false
        return session.handleCommand(userId, command)
    }

    /**
     * Отправка быстрой реакции.
     */
    suspend fun sendReaction(
        roomId: Uuid,
        userId: Uuid,
        emoji: String
    ): Boolean {
        val session = sessions[roomId] ?: return false
        return session.sendReaction(userId, emoji)
    }

    /**
     * Отправка текстового сообщения в чат комнаты.
     */
    suspend fun sendChatMessage(
        roomId: Uuid,
        userId: Uuid,
        text: String,
        playbackPositionMs: Long
    ): Boolean {
        val session = sessions[roomId] ?: return false
        return session.sendChatMessage(userId, text, playbackPositionMs)
    }

    /**
     * Получение истории сообщений для указанного эпизода.
     */
    fun getRoomChatHistory(
        roomId: Uuid,
        season: Int?,
        episode: Int?
    ): List<WatchRoomChatMessageDto> {
        val session = sessions[roomId] ?: return emptyList()
        return session.getChatHistory(season, episode)
    }

    /**
     * Подключение к реактивному потоку событий комнаты (TrueSync / Плеер).
     */
    suspend fun streamEvents(
        roomId: Uuid,
        user: AuthState.Authorized
    ): Flow<WatchRoomEvent>? {
        val session = getOrCreateSession(roomId) ?: return null

        val role = if (session.hostUserId == user.userId) WatchRoomParticipantRole.HOST else WatchRoomParticipantRole.MEMBER
        val connectionId = Uuid.random()
        session.handleClientConnected(user.userId, user.username, role, connectionId)

        return flow {
            // Мгновенный начальный снимок состояния для синхронизации подключающегося участника
            emit(session.getCurrentSyncState())
            emit(WatchRoomEvent.ParticipantsUpdated(session.getParticipantList()))
            emit(
                WatchRoomEvent.ChatHistorySnapshot(
                    season = session.currentSeason,
                    episode = session.currentEpisode,
                    messages = session.getChatHistory(session.currentSeason, session.currentEpisode)
                )
            )
            emitAll(session.events)
        }.onCompletion {
            withContext(NonCancellable) {
                session.handleClientDisconnected(user.userId, connectionId)
            }
        }
    }

    /**
     * Подключение к реактивному потоку предстартового лобби (Snapshot + Upsert Item).
     */
    suspend fun streamLobbyState(
        roomId: Uuid,
        user: AuthState.Authorized
    ): Flow<LobbyEvent>? {
        val session = getOrCreateSession(roomId) ?: return null
        val role = if (session.hostUserId == user.userId) WatchRoomParticipantRole.HOST else WatchRoomParticipantRole.MEMBER
        val connectionId = Uuid.random()
        session.handleClientConnected(user.userId, user.username, role, connectionId)

        return session.subscribeLobby().onCompletion {
            withContext(NonCancellable) {
                session.handleClientDisconnected(user.userId, connectionId)
            }
        }
    }

    /**
     * Обновление готовности и намерения участника в лобби.
     */
    suspend fun setLobbyStatus(
        roomId: Uuid,
        userId: Uuid,
        request: SetLobbyStatusRequest
    ): Boolean {
        val session = sessions[roomId] ?: getOrCreateSession(roomId) ?: return false
        return session.setLobbyStatus(userId, request.intent, request.isReady)
    }

    /**
     * Запуск совместного просмотра хостом из лобби.
     */
    suspend fun triggerStartPlayback(
        roomId: Uuid,
        userId: Uuid
    ): Boolean {
        val session = sessions[roomId] ?: getOrCreateSession(roomId) ?: return false
        return session.triggerStartPlayback(userId)
    }

    /**
     * Фоновая синхронизация прогресса просмотра в личные истории всех участников.
     */
    private fun syncProgressToParticipants(
        roomId: Uuid,
        mediaId: String,
        mediaType: MediaType,
        season: Int?,
        episode: Int?,
        posSec: Long
    ) {
        val session = sessions[roomId] ?: return
        val currentParticipants = session.getParticipantList()

        scope.launch {
            try {
                if (mediaType == MediaType.MOVIE) {
                    currentParticipants.forEach { p ->
                        userMovieRepository.updateUserMovie(
                            UserMovieItem(
                                id = Uuid.random(),
                                userId = p.userId,
                                catalogId = "tmdb",
                                mediaId = mediaId,
                                mediaType = MediaType.MOVIE,
                                status = MediaStatus.WATCHING,
                                progressSeconds = posSec,
                                durationSeconds = 0L,
                                lastWatchedAt = Clock.System.now(),
                                updatedAt = Clock.System.now()
                            )
                        )
                    }
                } else {
                    val s = season ?: 1
                    val ep = episode ?: 1
                    currentParticipants.forEach { p ->
                        userEpisodeRepository.saveEpisodeProgress(
                            userId = p.userId,
                            catalogId = "tmdb",
                            mediaId = mediaId,
                            season = s,
                            episode = ep,
                            progressSeconds = posSec,
                            durationSeconds = 0L,
                            isWatched = false
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to sync watch progress to participants for room {}: {}", roomId, e.message)
            }
        }
    }

    /**
     * Сброс прогресса всех активных сессий в БД (например, при остановке сервера).
     */
    suspend fun flushAllSessions() {
        logger.info("Flushing progress for all active watch room sessions ({} active)...", sessions.size)
        sessions.values.forEach { session ->
            try {
                session.flushProgress()
            } catch (e: Exception) {
                logger.warn("Failed to flush session progress for room {}: {}", session.roomId, e.message)
            }
        }
    }
}
