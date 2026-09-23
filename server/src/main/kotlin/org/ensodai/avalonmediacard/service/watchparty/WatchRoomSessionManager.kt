package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.ensodai.avalonmediacard.repository.UserEpisodeRepository
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.ensodai.avalonmediacard.repository.watchparty.WatchRoomRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Синглтон-сервис управления активными сессиями комнат совместного просмотра в памяти сервера.
 */
@Single
class WatchRoomSessionManager(
    private val watchRoomRepository: WatchRoomRepository,
    private val userMovieRepository: UserMovieRepository,
    private val userEpisodeRepository: UserEpisodeRepository
) {
    private val logger = LoggerFactory.getLogger(WatchRoomSessionManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Активные сессии комнат в оперативной памяти
    private val sessions = ConcurrentHashMap<Uuid, WatchRoomSession>()

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
            }
        )

        // Загружаем постоянных участников комнаты
        roomDto.participants.forEach { p ->
            newSession.addOrUpdateParticipant(
                userId = p.userId,
                username = p.username,
                role = p.role
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
            }
        )

        session.addOrUpdateParticipant(
            userId = hostUser.userId,
            username = hostUser.username,
            role = WatchRoomParticipantRole.HOST
        )

        sessions[roomDto.id] = session
        return session
    }

    /**
     * Вход пользователя в комнату.
     */
    suspend fun joinRoom(roomId: Uuid, user: AuthState.Authorized): WatchRoomDto? {
        val session = getOrCreateSession(roomId) ?: return null

        val isHost = session.hostUserId == user.userId
        val role = if (isHost) WatchRoomParticipantRole.HOST else WatchRoomParticipantRole.MEMBER

        session.addOrUpdateParticipant(user.userId, user.username, role)
        watchRoomRepository.addParticipant(roomId, user.userId, role)

        val dbRoom = watchRoomRepository.findRoomById(roomId) ?: return null
        return dbRoom.copy(
            participants = session.getParticipantList()
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
        return true
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
     * Подключение к реактивному потоку событий комнаты.
     */
    suspend fun streamEvents(
        roomId: Uuid,
        user: AuthState.Authorized
    ): Flow<WatchRoomEvent>? {
        val session = getOrCreateSession(roomId) ?: return null

        val role = if (session.hostUserId == user.userId) WatchRoomParticipantRole.HOST else WatchRoomParticipantRole.MEMBER
        session.addOrUpdateParticipant(user.userId, user.username, role)

        return session.events.onCompletion {
            // При закрытии WebSocket соединения отмечаем участника офлайн
            session.markParticipantOffline(user.userId)
        }
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
}
