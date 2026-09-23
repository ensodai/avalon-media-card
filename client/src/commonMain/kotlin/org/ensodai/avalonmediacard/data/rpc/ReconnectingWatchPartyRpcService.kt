package org.ensodai.avalonmediacard.data.rpc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

class ReconnectingWatchPartyRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : WatchPartyRpcService {

    private val logger = AppLogging.logger("ReconnectingWatchPartyRpcService")

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

    override suspend fun leaveRoom(roomId: Uuid): Boolean =
        executor.execute("leaveRoom", getService = { getService() }) {
            leaveRoom(roomId)
        }

    override suspend fun closeRoom(roomId: Uuid): Boolean =
        executor.execute("closeRoom", getService = { getService() }) {
            closeRoom(roomId)
        }

    override fun streamRoomEvents(roomId: Uuid): Flow<WatchRoomEvent> {
        return flow {
            emitAll(getService().streamRoomEvents(roomId))
        }.retryWhen { cause, attempt ->
            if (cause is CancellationException && !executor.isNetworkCancellation(cause)) {
                return@retryWhen false
            }
            logger.w(cause) { "Room Events Stream failed for $roomId (attempt $attempt). Retrying..." }
            connectionManager.notifyStreamFailure(cause)
            delay(min(1000L * (attempt + 1), 5000L).milliseconds)
            true
        }
    }

    override suspend fun sendPlaybackCommand(roomId: Uuid, command: RoomPlaybackCommand): Boolean =
        executor.execute("sendPlaybackCommand", getService = { getService() }) {
            sendPlaybackCommand(roomId, command)
        }

    override suspend fun sendReaction(roomId: Uuid, emoji: String): Boolean =
        executor.execute("sendReaction", getService = { getService() }) {
            sendReaction(roomId, emoji)
        }
}
