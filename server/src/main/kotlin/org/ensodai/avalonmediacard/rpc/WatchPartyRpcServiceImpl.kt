package org.ensodai.avalonmediacard.rpc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import org.ensodai.avalonmediacard.repository.watchparty.WatchRoomRepository
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.ensodai.avalonmediacard.service.watchparty.WatchRoomSessionManager
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.slf4j.LoggerFactory
import kotlin.random.Random
import kotlin.uuid.Uuid

/**
 * Серверная реализация RPC сервиса для совместного просмотра (Watch Party).
 */
@Factory
class WatchPartyRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val watchRoomRepository: WatchRoomRepository,
    private val watchRoomSessionManager: WatchRoomSessionManager
) : WatchPartyRpcService {

    private val logger = LoggerFactory.getLogger(WatchPartyRpcServiceImpl::class.java)

    private fun currentAuthorizedUser(): AuthState.Authorized? {
        return session.state.value as? AuthState.Authorized
    }

    override suspend fun createRoom(request: CreateRoomRequest): WatchRoomDto {
        val authUser = currentAuthorizedUser()
            ?: throw IllegalStateException("Пользователь не авторизован")

        var pin = generateRoomPin()
        var retries = 5
        while (retries > 0 && watchRoomRepository.findRoomByPin(pin) != null) {
            pin = generateRoomPin()
            retries--
        }

        val roomDto = watchRoomRepository.createRoom(
            hostUserId = authUser.userId,
            request = request,
            pin = pin
        )

        watchRoomSessionManager.registerNewRoom(roomDto, authUser)
        logger.info("Room created by {}: roomId={}, pin={}", authUser.username, roomDto.id, pin)
        return roomDto
    }

    override suspend fun joinRoomByPin(pin: String): JoinRoomResult {
        val authUser = currentAuthorizedUser()
            ?: return JoinRoomResult.Error("Пользователь не авторизован")

        val cleanPin = pin.trim()
        val room = watchRoomRepository.findRoomByPin(cleanPin)
            ?: return JoinRoomResult.Error("Комната с PIN-кодом $cleanPin не найдена")

        val joinedRoom = watchRoomSessionManager.joinRoom(room.id, authUser)
            ?: return JoinRoomResult.Error("Не удалось войти в комнату")

        logger.info("User {} joined room {} by PIN", authUser.username, room.id)
        return JoinRoomResult.Success(joinedRoom)
    }

    override suspend fun joinRoomById(roomId: Uuid): JoinRoomResult {
        val authUser = currentAuthorizedUser()
            ?: return JoinRoomResult.Error("Пользователь не авторизован")

        val joinedRoom = watchRoomSessionManager.joinRoom(roomId, authUser)
            ?: return JoinRoomResult.Error("Комната не найдена или недоступна")

        logger.info("User {} joined room {} by ID", authUser.username, roomId)
        return JoinRoomResult.Success(joinedRoom)
    }

    override suspend fun getSavedRoomsForMedia(mediaId: String): List<WatchRoomSummaryDto> {
        val authUser = currentAuthorizedUser() ?: return emptyList()
        return watchRoomRepository.getRoomsForMedia(mediaId, authUser.userId)
    }

    override suspend fun getUserRooms(): List<WatchRoomSummaryDto> {
        val authUser = currentAuthorizedUser() ?: return emptyList()
        return watchRoomRepository.getRoomsForUser(authUser.userId)
    }

    override suspend fun leaveRoom(roomId: Uuid): Boolean {
        val authUser = currentAuthorizedUser() ?: return false
        logger.info("User {} left room {}", authUser.username, roomId)
        return watchRoomSessionManager.leaveRoom(roomId, authUser.userId)
    }

    override suspend fun closeRoom(roomId: Uuid): Boolean {
        val authUser = currentAuthorizedUser() ?: return false
        logger.info("User {} requested to close room {}", authUser.username, roomId)
        return watchRoomSessionManager.closeRoom(roomId, authUser.userId)
    }

    override fun streamLobbyState(roomId: Uuid): Flow<LobbyEvent> = flow {
        val authUser = currentAuthorizedUser()
        if (authUser == null) {
            emit(LobbyEvent.SystemNotice("Ошибка: вы не авторизованы"))
            return@flow
        }

        val lobbyFlow = watchRoomSessionManager.streamLobbyState(roomId, authUser)
        if (lobbyFlow == null) {
            emit(LobbyEvent.SystemNotice("Комната не найдена или уже закрыта"))
            return@flow
        }

        emitAll(lobbyFlow)
    }

    override suspend fun setLobbyStatus(roomId: Uuid, request: SetLobbyStatusRequest): Boolean {
        val authUser = currentAuthorizedUser() ?: return false
        return watchRoomSessionManager.setLobbyStatus(roomId, authUser.userId, request)
    }

    override suspend fun triggerStartPlayback(roomId: Uuid): Boolean {
        val authUser = currentAuthorizedUser() ?: return false
        return watchRoomSessionManager.triggerStartPlayback(roomId, authUser.userId)
    }

    override fun streamRoomEvents(roomId: Uuid): Flow<WatchRoomEvent> = flow {
        val authUser = currentAuthorizedUser()
        if (authUser == null) {
            emit(WatchRoomEvent.SystemNotice("Ошибка: вы не авторизованы"))
            return@flow
        }

        val eventFlow = watchRoomSessionManager.streamEvents(roomId, authUser)
        if (eventFlow == null) {
            emit(WatchRoomEvent.SystemNotice("Комната не найдена или уже закрыта"))
            return@flow
        }

        emitAll(eventFlow)
    }

    override suspend fun sendPlaybackCommand(roomId: Uuid, command: RoomPlaybackCommand): Boolean {
        val authUser = currentAuthorizedUser() ?: return false
        return watchRoomSessionManager.sendPlaybackCommand(roomId, authUser.userId, command)
    }

    override suspend fun sendReaction(roomId: Uuid, emoji: String): Boolean {
        val authUser = currentAuthorizedUser() ?: return false
        return watchRoomSessionManager.sendReaction(roomId, authUser.userId, emoji)
    }

    private fun generateRoomPin(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}
