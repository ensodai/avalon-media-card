package org.ensodai.avalonmediacard.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class WatchPartyRepositoryImpl(
    private val rpcService: WatchPartyRpcService
) : WatchPartyRepository {

    private val _userRoomsFlow = MutableStateFlow<List<WatchRoomSummaryDto>>(emptyList())
    override val userRoomsFlow: StateFlow<List<WatchRoomSummaryDto>> = _userRoomsFlow.asStateFlow()

    override suspend fun refreshUserRooms(): List<WatchRoomSummaryDto> {
        val rooms = rpcService.getUserRooms()
        _userRoomsFlow.value = rooms
        return rooms
    }

    override suspend fun createRoom(request: CreateRoomRequest): WatchRoomDto {
        val room = rpcService.createRoom(request)
        runCatching { refreshUserRooms() }
        return room
    }

    override suspend fun joinRoomByPin(pin: String): JoinRoomResult =
        rpcService.joinRoomByPin(pin)

    override suspend fun joinRoomById(roomId: Uuid): JoinRoomResult =
        rpcService.joinRoomById(roomId)

    override suspend fun getSavedRoomsForMedia(mediaId: String): List<WatchRoomSummaryDto> =
        rpcService.getSavedRoomsForMedia(mediaId)

    override suspend fun getUserRooms(): List<WatchRoomSummaryDto> =
        refreshUserRooms()

    override suspend fun leaveRoom(roomId: Uuid): Boolean {
        val result = rpcService.leaveRoom(roomId)
        if (result) runCatching { refreshUserRooms() }
        return result
    }

    override suspend fun closeRoom(roomId: Uuid): Boolean {
        val result = rpcService.closeRoom(roomId)
        if (result) runCatching { refreshUserRooms() }
        return result
    }

    override fun streamLobbyState(roomId: Uuid): Flow<LobbyEvent> =
        rpcService.streamLobbyState(roomId)

    override suspend fun setLobbyStatus(roomId: Uuid, request: SetLobbyStatusRequest): Boolean =
        rpcService.setLobbyStatus(roomId, request)

    override suspend fun triggerStartPlayback(roomId: Uuid): Boolean =
        rpcService.triggerStartPlayback(roomId)

    override fun streamRoomEvents(roomId: Uuid): Flow<WatchRoomEvent> =
        rpcService.streamRoomEvents(roomId)

    override suspend fun sendPlaybackCommand(roomId: Uuid, command: RoomPlaybackCommand): Boolean =
        rpcService.sendPlaybackCommand(roomId, command)

    override suspend fun sendReaction(roomId: Uuid, emoji: String): Boolean =
        rpcService.sendReaction(roomId, emoji)
}
