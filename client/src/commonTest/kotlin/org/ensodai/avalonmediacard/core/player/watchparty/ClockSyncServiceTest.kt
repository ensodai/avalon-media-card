package org.ensodai.avalonmediacard.core.player.watchparty

import kotlinx.coroutines.test.runTest
import org.ensodai.avalonmediacard.contract.model.ClockSyncPong
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class ClockSyncServiceTest {

    private fun createDummyRpcService(): WatchPartyRpcService {
        return object : WatchPartyRpcService {
            override suspend fun syncClock(ping: org.ensodai.avalonmediacard.contract.model.ClockSyncPing): ClockSyncPong {
                throw UnsupportedOperationException("Not implemented")
            }
            override suspend fun createRoom(request: org.ensodai.avalonmediacard.contract.model.CreateRoomRequest): org.ensodai.avalonmediacard.contract.model.WatchRoomDto = TODO()
            override suspend fun joinRoomByPin(pin: String): org.ensodai.avalonmediacard.contract.model.JoinRoomResult = TODO()
            override suspend fun joinRoomById(roomId: kotlin.uuid.Uuid): org.ensodai.avalonmediacard.contract.model.JoinRoomResult = TODO()
            override suspend fun getSavedRoomsForMedia(mediaId: String): List<org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto> = TODO()
            override fun streamSavedRoomsForMedia(mediaId: String): kotlinx.coroutines.flow.Flow<List<org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto>> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun getUserRooms(): List<org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto> = TODO()
            override suspend fun leaveRoom(roomId: kotlin.uuid.Uuid): Boolean = TODO()
            override suspend fun closeRoom(roomId: kotlin.uuid.Uuid): Boolean = TODO()
            override fun streamLobbyState(roomId: kotlin.uuid.Uuid): kotlinx.coroutines.flow.Flow<org.ensodai.avalonmediacard.contract.model.LobbyEvent> = TODO()
            override fun streamUserRooms(): kotlinx.coroutines.flow.Flow<List<org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto>> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun setLobbyStatus(roomId: kotlin.uuid.Uuid, request: org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest): Boolean = TODO()
            override suspend fun triggerStartPlayback(roomId: kotlin.uuid.Uuid): Boolean = TODO()
            override fun streamRoomEvents(roomId: kotlin.uuid.Uuid): kotlinx.coroutines.flow.Flow<org.ensodai.avalonmediacard.contract.model.WatchRoomEvent> = TODO()
            override suspend fun sendPlaybackCommand(roomId: kotlin.uuid.Uuid, command: org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand): Boolean = TODO()
            override suspend fun sendReaction(roomId: kotlin.uuid.Uuid, emoji: String): Boolean = TODO()
        }
    }

    @Test
    fun testMeasureSampleCalculation() = runTest {
        val service = ClockSyncService(createDummyRpcService())

        var currentTime = Instant.fromEpochMilliseconds(1000)
        service.clockProvider = { currentTime }

        service.pingServerOverride = { ping ->
            // Simulating network delay: t1 = 1000 ms, t2 = 1200 ms, t3 = 1205 ms, t4 = 1305 ms
            currentTime = Instant.fromEpochMilliseconds(1305)
            ClockSyncPong(
                clientSendTime = ping.clientSendTime,
                serverReceiveTime = Instant.fromEpochMilliseconds(1200),
                serverTransmitTime = Instant.fromEpochMilliseconds(1205)
            )
        }

        val sample = service.measureSample()

        // RTT = (t4 - t1) - (t3 - t2) = (1305 - 1000) - (1205 - 1200) = 305 - 5 = 300 ms
        assertEquals(300.milliseconds, sample.rtt)

        // Theta = ((t2 - t1) + (t3 - t4)) / 2 = ((1200 - 1000) + (1205 - 1305)) / 2 = (200 - 100) / 2 = 50 ms
        assertEquals(50.milliseconds, sample.theta)
    }

    @Test
    fun testSyncSelectsMinRttSample() = runTest {
        val service = ClockSyncService(createDummyRpcService())

        val pongs = listOf(
            // Sample 1: RTT = 200 ms, theta = 40 ms
            ClockSyncPong(
                clientSendTime = Instant.fromEpochMilliseconds(1000),
                serverReceiveTime = Instant.fromEpochMilliseconds(1140),
                serverTransmitTime = Instant.fromEpochMilliseconds(1140),
            ),
            // Sample 2: RTT = 50 ms, theta = 60 ms (best RTT)
            ClockSyncPong(
                clientSendTime = Instant.fromEpochMilliseconds(2000),
                serverReceiveTime = Instant.fromEpochMilliseconds(2085),
                serverTransmitTime = Instant.fromEpochMilliseconds(2085),
            ),
            // Sample 3: RTT = 120 ms, theta = 30 ms
            ClockSyncPong(
                clientSendTime = Instant.fromEpochMilliseconds(3000),
                serverReceiveTime = Instant.fromEpochMilliseconds(3090),
                serverTransmitTime = Instant.fromEpochMilliseconds(3090),
            )
        )

        val t4Times = listOf(
            Instant.fromEpochMilliseconds(1200),
            Instant.fromEpochMilliseconds(2050),
            Instant.fromEpochMilliseconds(3120)
        )

        var callCount = 0
        service.clockProvider = {
            if (callCount % 2 == 0) {
                // t1
                pongs[callCount / 2].clientSendTime
            } else {
                // t4
                t4Times[(callCount - 1) / 2]
            }
        }

        service.pingServerOverride = { ping ->
            val idx = callCount / 2
            callCount++ // for t4
            pongs[idx]
        }

        // Before sync
        assertFalse(service.isSynchronized)

        service.sync(samplesCount = 3)

        assertTrue(service.isSynchronized)
        // Best sample should be sample 2 with theta = 60 ms
        assertEquals(60.milliseconds, service.offset)
    }

    @Test
    fun testEmaSmoothing() = runTest {
        val service = ClockSyncService(createDummyRpcService())

        // Initial sync: offset = 100 ms
        var clientTime = Instant.fromEpochMilliseconds(1000)
        service.clockProvider = { clientTime }
        service.pingServerOverride = { ping ->
            clientTime = Instant.fromEpochMilliseconds(1100)
            ClockSyncPong(
                clientSendTime = ping.clientSendTime,
                serverReceiveTime = Instant.fromEpochMilliseconds(1150),
                serverTransmitTime = Instant.fromEpochMilliseconds(1150)
            )
        }

        service.sync(samplesCount = 1)
        // (1150 - 1000) + (1150 - 1100) = 150 + 50 = 200 / 2 = 100 ms
        assertEquals(100.milliseconds, service.offset)

        // Second sync: sample theta = 50 ms
        clientTime = Instant.fromEpochMilliseconds(2000)
        service.pingServerOverride = { ping ->
            clientTime = Instant.fromEpochMilliseconds(2100)
            ClockSyncPong(
                clientSendTime = ping.clientSendTime,
                serverReceiveTime = Instant.fromEpochMilliseconds(2100),
                serverTransmitTime = Instant.fromEpochMilliseconds(2100)
            )
        }
        // (2100 - 2000) + (2100 - 2100) = 100 + 0 = 100 / 2 = 50 ms
        service.sync(samplesCount = 1)

        // EMA: 0.8 * 100 + 0.2 * 50 = 80 + 10 = 90 ms
        assertEquals(90.milliseconds, service.offset)

        // Estimated server time
        clientTime = Instant.fromEpochMilliseconds(5000)
        assertEquals(Instant.fromEpochMilliseconds(5090), service.estimatedServerTime())

        // Reset
        service.reset()
        assertFalse(service.isSynchronized)
        assertEquals(null, service.offset)
    }
}
