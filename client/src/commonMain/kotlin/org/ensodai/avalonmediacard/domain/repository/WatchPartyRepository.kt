package org.ensodai.avalonmediacard.domain.repository

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import kotlin.uuid.Uuid

interface WatchPartyRepository {
    suspend fun createRoom(request: CreateRoomRequest): WatchRoomDto
    suspend fun joinRoomByPin(pin: String): JoinRoomResult
    suspend fun joinRoomById(roomId: Uuid): JoinRoomResult
    suspend fun getSavedRoomsForMedia(mediaId: String): List<WatchRoomSummaryDto>
    suspend fun leaveRoom(roomId: Uuid): Boolean
    suspend fun closeRoom(roomId: Uuid): Boolean
    fun streamRoomEvents(roomId: Uuid): Flow<WatchRoomEvent>
    suspend fun sendPlaybackCommand(roomId: Uuid, command: RoomPlaybackCommand): Boolean
    suspend fun sendReaction(roomId: Uuid, emoji: String): Boolean
}
