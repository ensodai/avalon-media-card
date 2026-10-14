package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
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
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPhase
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.repository.UserEpisodeRepository
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.ensodai.avalonmediacard.repository.watchparty.WatchRoomRepository
import org.ensodai.avalonmediacard.service.TorrServerKeepAlivePinger
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

/**
 * Синглтон-сервис управления активными сессиями комнат совместного (PARTY)
 * и одиночного (SOLO) просмотра в памяти сервера.
 */
@Single
class WatchRoomSessionManager(
    private val watchRoomRepository: WatchRoomRepository,
    private val userMovieRepository: UserMovieRepository,
    private val userEpisodeRepository: UserEpisodeRepository,
    private val torrServerPinger: TorrServerKeepAlivePinger? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val logger = LoggerFactory.getLogger(WatchRoomSessionManager::class.java)

    // Активные сессии комнат в оперативной памяти
    private val sessions = ConcurrentHashMap<Uuid, WatchRoomSession>()
    private val soloCleanupJobs = ConcurrentHashMap<Uuid, Job>()

    // Шина оповещений об изменениях в комнатах для реактивного стриминга дашборда
    private val _roomUpdatesFlow = MutableSharedFlow<Uuid>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val roomUpdatesFlow: Flow<Uuid> = _roomUpdatesFlow.asSharedFlow()

    fun notifyRoomUpdated(roomId: Uuid) {
        _roomUpdatesFlow.tryEmit(roomId)
    }

    /**
     * Создает или возвращает существующую сессию одиночного просмотра (SOLO).
     * Сессия живет строго в оперативной памяти сервера (не персистится в таблицу БД watch_rooms).
     */
    fun getOrCreateSoloSession(
        userId: Uuid,
        mediaId: String,
        title: String,
        sourceType: String?,
        sourceId: String?,
        season: Int?,
        episode: Int?,
        startPositionSeconds: Long = 0L
    ): WatchRoomSession {
        val existing = sessions.values.firstOrNull { it.mode == RoomMode.SOLO && it.hostUserId == userId }
        if (existing != null) {
            soloCleanupJobs.remove(existing.roomId)?.cancel()
            scope.launch {
                existing.updateSource(
                    newSourceType = sourceType ?: "",
                    newSourceId = sourceId ?: "",
                    newSeason = season,
                    newEpisode = episode
                )
            }
            return existing
        }

        val soloRoomId = Uuid.random()
        val soloSession = WatchRoomSession(
            roomId = soloRoomId,
            mediaId = mediaId,
            title = title,
            hostUserId = userId,
            controlMode = WatchRoomControlMode.HOST_ONLY,
            initialSeason = season,
            initialEpisode = episode,
            initialPositionSeconds = startPositionSeconds,
            initialSourceType = sourceType,
            initialSourceId = sourceId,
            scope = scope,
            mode = RoomMode.SOLO,
            torrServerPinger = torrServerPinger
        )
        sessions[soloRoomId] = soloSession
        logger.info("[SoloSession] Created solo session {} for user {}, mediaId={}, sourceId={}", soloRoomId, userId, mediaId, sourceId)
        return soloSession
    }

    /**
     * Сброс прогресса всех активных комнат совместного просмотра в БД при выключении сервера.
     */
    suspend fun flushAllSessions() {
        soloCleanupJobs.values.forEach { it.cancel() }
        soloCleanupJobs.clear()
        sessions.values.forEach { session ->
            if (session.mode == RoomMode.PARTY) {
                try {
                    session.flushProgress()
                } catch (e: Exception) {
                    logger.warn("Failed to flush session {}: {}", session.roomId, e.message)
                }
            }
        }
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
            onRoomStateChanged = { updatedRoomId -> notifyRoomUpdated(updatedRoomId) },
            mode = RoomMode.PARTY,
            torrServerPinger = torrServerPinger
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
     * Регистрация только что созданной комнаты совместного просмотра.
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
            onRoomStateChanged = { updatedRoomId -> notifyRoomUpdated(updatedRoomId) },
            mode = RoomMode.PARTY,
            torrServerPinger = torrServerPinger
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
            val session = sessions[room.id]?.takeIf { it.mode == RoomMode.PARTY }
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

    @OptIn(FlowPreview::class)
    fun streamUserRooms(userId: Uuid): Flow<List<WatchRoomSummaryDto>> = flow {
        emit(getUserRooms(userId))
        _roomUpdatesFlow
            .debounce(100.milliseconds)
            .collect {
                emit(getUserRooms(userId))
            }
    }

    @OptIn(FlowPreview::class)
    fun streamSavedRoomsForMedia(mediaId: String, currentUserId: Uuid): Flow<List<WatchRoomSummaryDto>> = flow {
        emit(getSavedRoomsForMedia(mediaId, currentUserId))
        _roomUpdatesFlow
            .debounce(100.milliseconds)
            .collect {
                emit(getSavedRoomsForMedia(mediaId, currentUserId))
            }
    }

    suspend fun sendPlaybackCommand(
        roomId: Uuid,
        userId: Uuid,
        command: RoomPlaybackCommand
    ): Boolean {
        val session = sessions[roomId] ?: getOrCreateSession(roomId) ?: return false
        return session.handleCommand(userId, command)
    }

    suspend fun sendReaction(
        roomId: Uuid,
        userId: Uuid,
        emoji: String
    ): Boolean {
        val session = sessions[roomId] ?: return false
        return session.sendReaction(userId, emoji)
    }

    suspend fun sendChatMessage(
        roomId: Uuid,
        userId: Uuid,
        text: String,
        playbackPositionMs: Long
    ): Boolean {
        val session = sessions[roomId] ?: return false
        return session.sendChatMessage(userId, text, playbackPositionMs)
    }

    fun getRoomChatHistory(
        roomId: Uuid,
        season: Int?,
        episode: Int?
    ): List<WatchRoomChatMessageDto> {
        val session = sessions[roomId] ?: return emptyList()
        return session.getChatHistory(season, episode)
    }

    suspend fun streamEvents(
        roomId: Uuid,
        user: AuthState.Authorized
    ): Flow<WatchRoomEvent>? {
        val session = sessions[roomId] ?: getOrCreateSession(roomId) ?: return null

        val role = if (session.hostUserId == user.userId) WatchRoomParticipantRole.HOST else WatchRoomParticipantRole.MEMBER
        val connectionId = Uuid.random()
        session.handleClientConnected(user.userId, user.username, role, connectionId)
        if (session.mode == RoomMode.SOLO) {
            soloCleanupJobs.remove(roomId)?.cancel()
        }

        return session.createEventsFlow(user, connectionId).onCompletion { cause ->
            withContext(NonCancellable) {
                logger.info("[streamEvents] Client disconnected: room={}, user={}, cause={}", roomId, user.username, cause?.message ?: "Normal completion")
                session.handleClientDisconnected(user.userId, connectionId)
                if (session.mode == RoomMode.SOLO && session.getOnlineCount() == 0) {
                    soloCleanupJobs[roomId]?.cancel()
                    soloCleanupJobs[roomId] = scope.launch {
                        delay(45_000L)
                        val s = sessions[roomId]
                        if (s != null && s.mode == RoomMode.SOLO && s.getOnlineCount() == 0) {
                            logger.info("[SoloSession] 45s grace period expired. Cleaning up solo session {} for mediaId={}", roomId, s.mediaId)
                            sessions.remove(roomId)
                            s.close()
                        }
                        soloCleanupJobs.remove(roomId)
                    }
                }
            }
        }
    }

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

    suspend fun setLobbyStatus(
        roomId: Uuid,
        userId: Uuid,
        request: SetLobbyStatusRequest
    ): Boolean {
        val session = sessions[roomId] ?: getOrCreateSession(roomId) ?: return false
        return session.setLobbyStatus(userId, request.intent, request.isReady)
    }

    suspend fun triggerStartPlayback(
        roomId: Uuid,
        userId: Uuid
    ): Boolean {
        val session = sessions[roomId] ?: getOrCreateSession(roomId) ?: return false
        return session.triggerStartPlayback(userId)
    }

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
}
