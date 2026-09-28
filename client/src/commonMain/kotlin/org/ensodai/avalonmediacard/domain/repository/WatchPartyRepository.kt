package org.ensodai.avalonmediacard.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest
import org.ensodai.avalonmediacard.contract.model.UpdateRoomSourceRequest
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import kotlin.uuid.Uuid

interface WatchPartyRepository {
    suspend fun createRoom(request: CreateRoomRequest): WatchRoomDto
    suspend fun joinRoomByPin(pin: String): JoinRoomResult
    suspend fun joinRoomById(roomId: Uuid): JoinRoomResult
    suspend fun getSavedRoomsForMedia(mediaId: String): List<WatchRoomSummaryDto>
    fun streamSavedRoomsForMedia(mediaId: String): Flow<List<WatchRoomSummaryDto>>
    val userRoomsFlow: StateFlow<List<WatchRoomSummaryDto>>
    suspend fun refreshUserRooms(): List<WatchRoomSummaryDto>
    suspend fun getUserRooms(): List<WatchRoomSummaryDto>
    suspend fun leaveRoom(roomId: Uuid): Boolean
    suspend fun closeRoom(roomId: Uuid): Boolean
    fun streamLobbyState(roomId: Uuid): Flow<LobbyEvent>
    suspend fun setLobbyStatus(roomId: Uuid, request: SetLobbyStatusRequest): Boolean
    suspend fun updateRoomSource(request: UpdateRoomSourceRequest): Boolean
    suspend fun triggerStartPlayback(roomId: Uuid): Boolean
    fun streamRoomEvents(roomId: Uuid): Flow<WatchRoomEvent>
    suspend fun sendPlaybackCommand(roomId: Uuid, command: RoomPlaybackCommand): Boolean
    suspend fun sendReaction(roomId: Uuid, emoji: String): Boolean
}
