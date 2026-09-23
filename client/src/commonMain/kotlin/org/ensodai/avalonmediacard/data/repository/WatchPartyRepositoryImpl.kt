package org.ensodai.avalonmediacard.data.repository

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
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

    override suspend fun createRoom(request: CreateRoomRequest): WatchRoomDto =
        rpcService.createRoom(request)

    override suspend fun joinRoomByPin(pin: String): JoinRoomResult =
        rpcService.joinRoomByPin(pin)

    override suspend fun joinRoomById(roomId: Uuid): JoinRoomResult =
        rpcService.joinRoomById(roomId)

    override suspend fun getSavedRoomsForMedia(mediaId: String): List<WatchRoomSummaryDto> =
        rpcService.getSavedRoomsForMedia(mediaId)

    override suspend fun leaveRoom(roomId: Uuid): Boolean =
        rpcService.leaveRoom(roomId)

    override suspend fun closeRoom(roomId: Uuid): Boolean =
        rpcService.closeRoom(roomId)

    override fun streamRoomEvents(roomId: Uuid): Flow<WatchRoomEvent> =
        rpcService.streamRoomEvents(roomId)

    override suspend fun sendPlaybackCommand(roomId: Uuid, command: RoomPlaybackCommand): Boolean =
        rpcService.sendPlaybackCommand(roomId, command)

    override suspend fun sendReaction(roomId: Uuid, emoji: String): Boolean =
        rpcService.sendReaction(roomId, emoji)
}
