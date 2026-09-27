package org.ensodai.avalonmediacard.service.watchparty

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.repository.UserEpisodeRepository
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.ensodai.avalonmediacard.repository.watchparty.WatchRoomRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class WatchRoomSessionManagerTest {

    private class FakeWatchRoomRepository(
        val rooms: MutableList<WatchRoomSummaryDto>
    ) : WatchRoomRepository {
        override suspend fun createRoom(hostUserId: Uuid, request: CreateRoomRequest, pin: String): WatchRoomDto = TODO()
        override suspend fun findRoomById(roomId: Uuid): WatchRoomDto? {
            val r = rooms.find { it.id == roomId } ?: return null
            return WatchRoomDto(
                id = r.id,
                title = r.title,
                mediaId = r.mediaId,
                mediaType = r.mediaType,
                currentSeason = r.currentSeason,
                currentEpisode = r.currentEpisode,
                lastPositionSeconds = r.lastPositionSeconds,
                hostUserId = r.participants.firstOrNull { it.role == WatchRoomParticipantRole.HOST }?.userId ?: Uuid.random(),
                controlMode = WatchRoomControlMode.HOST_ONLY,
                status = r.status,
                joinPin = r.joinPin,
                participants = r.participants
            )
        }
        override suspend fun findRoomByPin(pin: String): WatchRoomDto? = TODO()
        override suspend fun getRoomsForMedia(mediaId: String, currentUserId: Uuid, limit: Int, offset: Long): List<WatchRoomSummaryDto> = TODO()
        override suspend fun getRoomsForUser(userId: Uuid, limit: Int, offset: Long): List<WatchRoomSummaryDto> = rooms
        override suspend fun updateRoomProgress(roomId: Uuid, season: Int?, episode: Int?, positionSeconds: Long) {}
        override suspend fun updateRoomStatus(roomId: Uuid, status: WatchRoomStatus) {}
        override suspend fun updateRoomControlMode(roomId: Uuid, controlMode: WatchRoomControlMode) {}
        override suspend fun addParticipant(roomId: Uuid, userId: Uuid, role: WatchRoomParticipantRole): Boolean = true
        override suspend fun removeParticipant(roomId: Uuid, userId: Uuid): Boolean = true
        override suspend fun getParticipants(roomId: Uuid): List<WatchRoomParticipantDto> = emptyList()
        override suspend fun transferHost(roomId: Uuid, newHostUserId: Uuid): Boolean = true
        override suspend fun deleteRoom(roomId: Uuid): Boolean = true
    }

    @Test
    fun test_streamUserRooms_emits_initial_state_and_live_updates_when_participant_enters_and_leaves() = runTest {
        val roomId = Uuid.random()
        val hostUserId = Uuid.random()
        val guestUserId = Uuid.random()

        val initialSummary = WatchRoomSummaryDto(
            id = roomId,
            title = "Party Room",
            mediaId = "m1",
            mediaType = MediaType.MOVIE,
            participants = listOf(
                WatchRoomParticipantDto(userId = hostUserId, username = "Host", role = WatchRoomParticipantRole.HOST, isOnline = false),
                WatchRoomParticipantDto(userId = guestUserId, username = "Guest", role = WatchRoomParticipantRole.MEMBER, isOnline = false)
            ),
            isHost = true,
            status = WatchRoomStatus.ACTIVE
        )

        val fakeRepo = FakeWatchRoomRepository(mutableListOf(initialSummary))
        val manager = WatchRoomSessionManager(
            watchRoomRepository = fakeRepo,
            userMovieRepository = UserMovieRepository(),
            userEpisodeRepository = UserEpisodeRepository()
        )

        // 1. Подписываемся на streamUserRooms
        val emittedSnapshots = mutableListOf<List<WatchRoomSummaryDto>>()
        val job = launch {
            manager.streamUserRooms(hostUserId).collect {
                emittedSnapshots.add(it)
            }
        }
        runCurrent()

        // Проверяем начальный слепок: 0 онлайн
        assertEquals(1, emittedSnapshots.size)
        val initialList = emittedSnapshots.first()
        assertEquals(0, initialList.first().onlineParticipantsCount)
        assertFalse(initialList.first().participants.first { it.userId == hostUserId }.isOnline)
        assertFalse(initialList.first().participants.first { it.userId == guestUserId }.isOnline)

        // 2. Гость подключается к лобби по сокету
        val session = manager.getOrCreateSession(roomId)!!
        val guestConn = Uuid.random()
        session.handleClientConnected(guestUserId, "Guest", WatchRoomParticipantRole.MEMBER, guestConn)

        // Продвигаем время для дебаунса 100ms
        advanceTimeBy(150.milliseconds)
        runCurrent()

        // Стрим комнат должен автоматически выдать второй слепок с 1 онлайн
        assertEquals(2, emittedSnapshots.size)
        val updatedList = emittedSnapshots.last()
        assertEquals(1, updatedList.first().onlineParticipantsCount)
        assertTrue(updatedList.first().participants.first { it.userId == guestUserId }.isOnline, "Гость должен стать онлайн в стриме комнат")
        assertFalse(updatedList.first().participants.first { it.userId == hostUserId }.isOnline, "Хост все еще оффлайн")

        // 3. Гость закрывает лобби (дисконнект сокета)
        session.handleClientDisconnected(guestUserId, guestConn)
        advanceTimeBy(150.milliseconds)
        runCurrent()

        // Стрим комнат должен автоматически выдать третий слепок с 0 онлайн
        assertEquals(3, emittedSnapshots.size)
        val finalList = emittedSnapshots.last()
        assertEquals(0, finalList.first().onlineParticipantsCount)
        assertFalse(finalList.first().participants.first { it.userId == guestUserId }.isOnline, "Гость должен стать оффлайн")

        job.cancel()
    }
}
