package org.ensodai.avalonmediacard.data.rpc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.ClockSyncPing
import org.ensodai.avalonmediacard.contract.model.ClockSyncPong
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest
import org.ensodai.avalonmediacard.contract.model.UpdateRoomSourceRequest
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

    override fun streamSavedRoomsForMedia(mediaId: String): Flow<List<WatchRoomSummaryDto>> {
        return flow {
            emitAll(getService().streamSavedRoomsForMedia(mediaId))
        }.retryWhen { cause, attempt ->
            if (cause is CancellationException && !executor.isNetworkCancellation(cause)) {
                return@retryWhen false
            }
            logger.w(cause) { "Saved Rooms for Media Stream failed (attempt $attempt). Retrying..." }
            connectionManager.notifyStreamFailure(cause)
            delay(min(1000L * (attempt + 1), 5000L).milliseconds)
            true
        }
    }

    override suspend fun getUserRooms(): List<WatchRoomSummaryDto> =
        executor.execute("getUserRooms", getService = { getService() }) {
            getUserRooms()
        }

    override fun streamUserRooms(): Flow<List<WatchRoomSummaryDto>> {
        return flow {
            emitAll(getService().streamUserRooms())
        }.retryWhen { cause, attempt ->
            if (cause is CancellationException && !executor.isNetworkCancellation(cause)) {
                return@retryWhen false
            }
            logger.w(cause) { "User Rooms Stream failed (attempt $attempt). Retrying..." }
            connectionManager.notifyStreamFailure(cause)
            delay(min(1000L * (attempt + 1), 5000L).milliseconds)
            true
        }
    }

    override suspend fun leaveRoom(roomId: Uuid): Boolean =
        executor.execute("leaveRoom", getService = { getService() }) {
            leaveRoom(roomId)
        }

    override suspend fun closeRoom(roomId: Uuid): Boolean =
        executor.execute("closeRoom", getService = { getService() }) {
            closeRoom(roomId)
        }

    override fun streamLobbyState(roomId: Uuid): Flow<LobbyEvent> {
        return flow {
            emitAll(getService().streamLobbyState(roomId))
        }.retryWhen { cause, attempt ->
            if (cause is CancellationException && !executor.isNetworkCancellation(cause)) {
                return@retryWhen false
            }
            logger.w(cause) { "Lobby State Stream failed for $roomId (attempt $attempt). Retrying..." }
            connectionManager.notifyStreamFailure(cause)
            delay(min(1000L * (attempt + 1), 5000L).milliseconds)
            true
        }
    }

    override suspend fun setLobbyStatus(roomId: Uuid, request: SetLobbyStatusRequest): Boolean =
        executor.execute("setLobbyStatus", getService = { getService() }) {
            setLobbyStatus(roomId, request)
        }

    override suspend fun triggerStartPlayback(roomId: Uuid): Boolean =
        executor.execute("triggerStartPlayback", getService = { getService() }) {
            triggerStartPlayback(roomId)
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

    override suspend fun updateRoomSource(request: UpdateRoomSourceRequest): Boolean =
        executor.execute("updateRoomSource", getService = { getService() }) {
            updateRoomSource(request)
        }

    override suspend fun syncClock(ping: ClockSyncPing): ClockSyncPong =
        executor.execute("syncClock", getService = { getService() }) {
            syncClock(ping)
        }
}
