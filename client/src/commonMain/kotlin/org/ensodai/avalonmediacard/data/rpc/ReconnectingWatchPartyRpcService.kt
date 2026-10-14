package org.ensodai.avalonmediacard.data.rpc

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.model.ClockSyncPing
import org.ensodai.avalonmediacard.contract.model.ClockSyncPong
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest
import org.ensodai.avalonmediacard.contract.model.UpdateRoomSourceRequest
import org.ensodai.avalonmediacard.contract.model.WatchRoomChatMessageDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import kotlin.uuid.Uuid

class ReconnectingWatchPartyRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : WatchPartyRpcService {

    private suspend fun getService(): WatchPartyRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun createRoom(request: CreateRoomRequest): WatchRoomDto =
        executor.execute("createRoom", getService = { getService() }) {
            createRoom(request)
        }

    override suspend fun joinRoomByPin(pin: String): JoinRoomResult =
        executor.execute("joinRoomByPin", getService = { getService() }) {
            joinRoomByPin(pin)
        }

    override suspend fun joinRoomById(roomId: Uuid): JoinRoomResult =
        executor.execute("joinRoomById", getService = { getService() }) {
            joinRoomById(roomId)
        }

    override suspend fun getSavedRoomsForMedia(mediaId: String): List<WatchRoomSummaryDto> =
        executor.execute("getSavedRoomsForMedia", getService = { getService() }) {
            getSavedRoomsForMedia(mediaId)
        }

    override fun streamSavedRoomsForMedia(mediaId: String): Flow<List<WatchRoomSummaryDto>> =
        executor.executeFlow("streamSavedRoomsForMedia", getService = { getService() }) {
            streamSavedRoomsForMedia(mediaId)
        }

    override suspend fun getUserRooms(): List<WatchRoomSummaryDto> =
        executor.execute("getUserRooms", getService = { getService() }) {
            getUserRooms()
        }

    override fun streamUserRooms(): Flow<List<WatchRoomSummaryDto>> =
        executor.executeFlow("streamUserRooms", getService = { getService() }) {
            streamUserRooms()
        }

    override suspend fun leaveRoom(roomId: Uuid): Boolean =
        executor.execute("leaveRoom", getService = { getService() }) {
            leaveRoom(roomId)
        }

    override suspend fun closeRoom(roomId: Uuid): Boolean =
        executor.execute("closeRoom", getService = { getService() }) {
            closeRoom(roomId)
        }

    override fun streamLobbyState(roomId: Uuid): Flow<LobbyEvent> =
        executor.executeFlow("streamLobbyState($roomId)", getService = { getService() }) {
            streamLobbyState(roomId)
        }

    override suspend fun setLobbyStatus(roomId: Uuid, request: SetLobbyStatusRequest): Boolean =
        executor.execute("setLobbyStatus", getService = { getService() }) {
            setLobbyStatus(roomId, request)
        }

    override suspend fun triggerStartPlayback(roomId: Uuid): Boolean =
        executor.execute("triggerStartPlayback", getService = { getService() }) {
            triggerStartPlayback(roomId)
        }

    override fun streamRoomEvents(roomId: Uuid): Flow<WatchRoomEvent> =
        executor.executeFlow("streamRoomEvents($roomId)", getService = { getService() }) {
            streamRoomEvents(roomId)
        }

    override suspend fun sendPlaybackCommand(roomId: Uuid, command: RoomPlaybackCommand): Boolean =
        executor.execute("sendPlaybackCommand", getService = { getService() }) {
            sendPlaybackCommand(roomId, command)
        }

    override suspend fun sendReaction(roomId: Uuid, emoji: String): Boolean =
        executor.execute("sendReaction", getService = { getService() }) {
            sendReaction(roomId, emoji)
        }

    override suspend fun sendChatMessage(roomId: Uuid, text: String, playbackPositionMs: Long): Boolean =
        executor.execute("sendChatMessage", getService = { getService() }) {
            sendChatMessage(roomId, text, playbackPositionMs)
        }

    override suspend fun getRoomChatHistory(roomId: Uuid, season: Int?, episode: Int?): List<WatchRoomChatMessageDto> =
        executor.execute("getRoomChatHistory", getService = { getService() }) {
            getRoomChatHistory(roomId, season, episode)
        }

    override suspend fun updateRoomSource(request: UpdateRoomSourceRequest): Boolean =
        executor.execute("updateRoomSource", getService = { getService() }) {
            updateRoomSource(request)
        }

    override suspend fun syncClock(ping: ClockSyncPing): ClockSyncPong =
        executor.execute("syncClock", getService = { getService() }) {
            syncClock(ping)
        }
}
