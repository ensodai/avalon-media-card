package org.ensodai.avalonmediacard.core.player.watchparty

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.ensodai.avalonmediacard.contract.model.ClockSyncPing
import org.ensodai.avalonmediacard.contract.model.ClockSyncPong
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.core.PlaybackState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ClientSyncControllerTest {

    private class TestPlaybackController(
        initialTime: Double = 0.0,
        initialPlaying: Boolean = false,
        initialDuration: Double = 100.0
    ) : PlaybackController {
        override val state = PlaybackState(
            currentTime = initialTime,
            isPlaying = initialPlaying,
            isBuffering = false,
            duration = initialDuration
        )

        val _isBufferingFlow = MutableStateFlow(false)
        override val isBufferingFlow: StateFlow<Boolean> get() = _isBufferingFlow.asStateFlow()

        fun setBuffering(buffering: Boolean) {
            state.isBuffering = buffering
            _isBufferingFlow.value = buffering
        }

        var lastSeekTime: Double? = null
        var lastPlaybackRate: Float = 1.0f
        var playCalledCount: Int = 0
        var pauseCalledCount: Int = 0

        override fun play() {
            playCalledCount++
            state.isPlaying = true
        }

        override fun pause() {
            pauseCalledCount++
            state.isPlaying = false
        }

        override fun togglePlayPause() {
            if (state.isPlaying) pause() else play()
        }

        override fun seek(time: Double) {
            lastSeekTime = time
            state.currentTime = time
        }

        override fun setMuted(muted: Boolean) {
            state.isMuted = muted
        }

        override fun setVolume(volume: Double) {
            state.volume = volume
        }

        override fun setPlaybackRate(rate: Float) {
            lastPlaybackRate = rate
        }
    }

    private class TestWatchPartyRpcService : WatchPartyRpcService {
        val sentCommands = mutableListOf<Pair<Uuid, RoomPlaybackCommand>>()
        val roomEventsFlow = MutableSharedFlow<WatchRoomEvent>(extraBufferCapacity = 16)

        override suspend fun sendPlaybackCommand(roomId: Uuid, command: RoomPlaybackCommand): Boolean {
            sentCommands.add(roomId to command)
            return true
        }

        override fun streamRoomEvents(roomId: Uuid): Flow<WatchRoomEvent> = roomEventsFlow

        override suspend fun syncClock(ping: ClockSyncPing): ClockSyncPong = ClockSyncPong(
            clientSendTime = ping.clientSendTime,
            serverReceiveTime = Instant.fromEpochMilliseconds(1000),
            serverTransmitTime = Instant.fromEpochMilliseconds(1000)
        )
        override suspend fun createRoom(request: CreateRoomRequest): WatchRoomDto = TODO()
        override suspend fun joinRoomByPin(pin: String): JoinRoomResult = TODO()
        override suspend fun joinRoomById(roomId: Uuid): JoinRoomResult = TODO()
        override suspend fun getSavedRoomsForMedia(mediaId: String): List<WatchRoomSummaryDto> = TODO()
        override fun streamSavedRoomsForMedia(mediaId: String): Flow<List<WatchRoomSummaryDto>> = kotlinx.coroutines.flow.emptyFlow()
        override suspend fun getUserRooms(): List<WatchRoomSummaryDto> = TODO()
        override suspend fun leaveRoom(roomId: Uuid): Boolean = TODO()
        override suspend fun closeRoom(roomId: Uuid): Boolean = TODO()
        override fun streamLobbyState(roomId: Uuid): Flow<LobbyEvent> = TODO()
        override fun streamUserRooms(): Flow<List<WatchRoomSummaryDto>> = kotlinx.coroutines.flow.emptyFlow()
        override suspend fun setLobbyStatus(roomId: Uuid, request: SetLobbyStatusRequest): Boolean = TODO()
        override suspend fun triggerStartPlayback(roomId: Uuid): Boolean = TODO()
        override suspend fun sendReaction(roomId: Uuid, emoji: String): Boolean = true
        override suspend fun updateRoomSource(request: org.ensodai.avalonmediacard.contract.model.UpdateRoomSourceRequest): Boolean = TODO()
        override suspend fun sendChatMessage(roomId: Uuid, text: String, playbackPositionMs: Long): Boolean = true
        override suspend fun getRoomChatHistory(roomId: Uuid, season: Int?, episode: Int?): List<org.ensodai.avalonmediacard.contract.model.WatchRoomChatMessageDto> = emptyList()
    }

    @Test
    fun testDeadbandSetsRateToOne() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 11.020) // 11,020 ms
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(2000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            // Anchor: position 10,000 ms at server time 1000 ms
            // At server time 2000 ms, target position = 10,000 + (2000 - 1000) = 11,000 ms
            // Player time = 11,020 ms -> delta = -20 ms (within deadband <= 30 ms)
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 10_000,
                    anchorServerTime = Instant.fromEpochMilliseconds(1000)
                )
            )

            player.lastPlaybackRate = 1.15f // simulate previous speed change
            controller.performDriftCorrectionStep()

            assertEquals(1.0f, player.lastPlaybackRate)
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testProportionalRateScaling() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 10.500) // 10,500 ms (behind by 500 ms)
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(2000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 10_000,
                    anchorServerTime = Instant.fromEpochMilliseconds(1000)
                )
            )

            // Target = 11,000 ms. Player = 10,500 ms. Delta = +500 ms.
            // rate = 1.0 + (0.0001 * 500) = 1.05f
            controller.performDriftCorrectionStep()
            assertEquals(1.05f, player.lastPlaybackRate)

            // Now test player ahead by 500 ms (player = 11,500 ms)
            player.state.currentTime = 11.500
            // Target = 11,000 ms. Delta = -500 ms.
            // rate = 1.0 + (0.0001 * -500) = 0.95f
            controller.performDriftCorrectionStep()
            assertEquals(0.95f, player.lastPlaybackRate)
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testRateClamping() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 8.000) // 8,000 ms (behind by 3,000 ms)
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(2000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 10_000,
                    anchorServerTime = Instant.fromEpochMilliseconds(1000)
                )
            )

            // Target = 11,000 ms. Player = 8,000 ms. Delta = +3,000 ms.
            // 1.0 + (0.0001 * 3000) = 1.30f -> clamped to MAX_PLAYBACK_RATE (1.25f)
            controller.performDriftCorrectionStep()
            assertEquals(ClientSyncController.MAX_PLAYBACK_RATE, player.lastPlaybackRate)
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testHardSeekOnLargeDrift() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 5.000) // 5,000 ms (behind by 6,000 ms)
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(2000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 10_000,
                    anchorServerTime = Instant.fromEpochMilliseconds(1000)
                )
            )

            // Target = 11,000 ms. Player = 5,000 ms. Delta = +6,000 ms > HARD_SEEK_THRESHOLD_MS (4000)
            controller.performDriftCorrectionStep()

            assertEquals(11.0, player.lastSeekTime)
            assertEquals(1.0f, player.lastPlaybackRate)
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testPrerollPhaseDelaysPlayback() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 0.0)
        val clockSync = ClockSyncService(rpc)
        var currentTime = Instant.fromEpochMilliseconds(1000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            // Scheduled to start 500 ms in the future
            val scheduledServerTime = Instant.fromEpochMilliseconds(1500)
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 5000,
                    anchorServerTime = scheduledServerTime
                )
            )

            assertTrue(controller.isPrerolling)
            assertEquals(5.0, player.lastSeekTime)
            assertFalse(player.state.isPlaying)

            // Advance clock to scheduled time
            currentTime = Instant.fromEpochMilliseconds(1500)
            advanceTimeBy(500.milliseconds)

            assertFalse(controller.isPrerolling)
            assertTrue(player.state.isPlaying)
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testPrerollSkipsRedundantSeekIfAlreadyAtTarget() = runTest {
        val rpc = TestWatchPartyRpcService()
        // Player is at 0.175s (175ms) from initial keyframe PTS, target is 0ms -> delta is 175ms <= 250ms threshold
        val player = TestPlaybackController(initialTime = 0.175)
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(1000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            // Target is 0ms. Delta is 175ms <= 250ms threshold.
            val scheduledServerTime = Instant.fromEpochMilliseconds(1500)
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 0,
                    anchorServerTime = scheduledServerTime
                )
            )

            assertTrue(controller.isPrerolling)
            // seek() should NOT have been called because player is already within 250ms of target!
            assertNull(player.lastSeekTime, "Redundant seek() should be skipped if player is already within 250ms of preroll target")
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testPrerollSeeksWhenExceedingThreshold() = runTest {
        val rpc = TestWatchPartyRpcService()
        // Player is at 3.0s (3000ms)
        val player = TestPlaybackController(initialTime = 3.0)
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(1000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            // Target is 0ms. Delta is 3000ms > 250ms threshold.
            val scheduledServerTime = Instant.fromEpochMilliseconds(1500)
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 0,
                    anchorServerTime = scheduledServerTime
                )
            )

            assertTrue(controller.isPrerolling)
            assertEquals(0.0, player.lastSeekTime, "Preroll must seek when difference exceeds 250ms")
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testPauseSnapsToAnchorIfDriftExceedsThreshold() = runTest {
        val rpc = TestWatchPartyRpcService()
        // Player is at 10.0s (10000ms)
        val player = TestPlaybackController(initialTime = 10.0)
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(1000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            // Target pause position is 10400ms (10.4s). Drift is 400ms > 250ms threshold.
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = false,
                    anchorPositionMs = 10400,
                    anchorServerTime = currentTime
                )
            )

            assertFalse(player.state.isPlaying)
            assertEquals(10.4, player.lastSeekTime, "Pause should seek to anchor when drift exceeds 250ms")
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testMonitorBufferingReportedDuringPreroll() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 0.0)
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(1000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.start()
            testScheduler.runCurrent()
            rpc.sentCommands.clear()

            // Переводим в состояние Preroll
            val scheduledServerTime = Instant.fromEpochMilliseconds(2500)
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 5000,
                    anchorServerTime = scheduledServerTime
                )
            )
            assertTrue(controller.isPrerolling)

            // Плеер буферизуется во время preroll
            player.setBuffering(true)
            testScheduler.runCurrent()

            // FSM-барьер: ReportBuffer(true) честно отправляется на сервер для отображения статуса участника
            val bufferCommands = rpc.sentCommands.filter { it.second is RoomPlaybackCommand.ReportBuffer }
            assertTrue(bufferCommands.any { (it.second as RoomPlaybackCommand.ReportBuffer).isBuffering }, "Во время Preroll команды ReportBuffer(true) должны честно отправляться на сервер")
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testPauseSyncState() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 20.0, initialPlaying = true)
        val clockSync = ClockSyncService(rpc)

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = false,
                    anchorPositionMs = 25_000,
                    anchorServerTime = Instant.fromEpochMilliseconds(1000)
                )
            )

            assertFalse(player.state.isPlaying)
            assertEquals(25.0, player.lastSeekTime)
            assertEquals(1.0f, player.lastPlaybackRate)
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testInterceptingControllerSendsRpcCommands() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 42.0, initialPlaying = true)
        val clockSync = ClockSyncService(rpc)
        val roomId = Uuid.random()

        val controller = ClientSyncController(
            roomId = roomId,
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            val intercepting = controller.createInterceptingController()

            intercepting.pause()
            testScheduler.advanceUntilIdle()

            assertEquals(1, rpc.sentCommands.size)
            assertEquals(roomId, rpc.sentCommands[0].first)
            val pauseCmd = rpc.sentCommands[0].second as RoomPlaybackCommand.Pause
            assertEquals(42_000L, pauseCmd.positionMs)

            intercepting.seek(120.0)
            testScheduler.advanceUntilIdle()

            assertEquals(2, rpc.sentCommands.size)
            val seekCmd = rpc.sentCommands[1].second as RoomPlaybackCommand.Seek
            assertEquals(120_000L, seekCmd.targetPositionMs)
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testDriftCorrectionSuppressedWhenBuffering() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 5.000)
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(2000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 10_000,
                    anchorServerTime = Instant.fromEpochMilliseconds(1000)
                )
            )

            // Huge drift (Target 11,000 ms, player 5,000 ms -> delta 6,000 ms)
            // But player is buffering!
            player.state.isBuffering = true
            controller.performDriftCorrectionStep()

            assertNull(player.lastSeekTime, "При isBuffering=true дрифт-коррекция не должна выполнять seek")
            assertEquals(1.0f, player.lastPlaybackRate, "При isBuffering=true скорость не должна меняться")

            // Плеер закончил буферизацию
            player.state.isBuffering = false
            controller.performDriftCorrectionStep()

            assertNotNull(player.lastSeekTime, "После завершения буферизации Hard Seek должен сработать")
            assertEquals(11.0, player.lastSeekTime)
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testHardSeekCooldownThrottling() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 5.000)
        val clockSync = ClockSyncService(rpc)
        var currentTime = Instant.fromEpochMilliseconds(2000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 10_000,
                    anchorServerTime = Instant.fromEpochMilliseconds(1000)
                )
            )

            // Первый Hard Seek (delta = 6000 ms)
            controller.performDriftCorrectionStep()
            assertEquals(11.0, player.lastSeekTime)

            // Сбрасываем lastSeekTime в моке
            player.lastSeekTime = null
            // Симулируем, что плеер все еще на 5.0 (качает чанк) и прошло 2 секунды (< 4 сек Cooldown)
            currentTime = Instant.fromEpochMilliseconds(4000)
            controller.performDriftCorrectionStep()

            // Seek не должен был вызваться повторно!
            assertNull(player.lastSeekTime, "Повторный Hard Seek должен блокироваться таймером Cooldown (4с)")

            // Прошло более 4 секунд (2000 + 4500 = 6500)
            currentTime = Instant.fromEpochMilliseconds(6500)
            controller.performDriftCorrectionStep()

            assertNotNull(player.lastSeekTime, "После истечения Cooldown Hard Seek должен сработать снова")
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testDriftCorrectionSuppressedWhenStreamNotLoaded() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 0.0, initialDuration = 0.0)
        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(2000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = true,
                    anchorPositionMs = 10_000,
                    anchorServerTime = Instant.fromEpochMilliseconds(1000)
                )
            )

            // Поток еще не загружен (длительность 0 и позиция 0)
            controller.performDriftCorrectionStep()
            assertNull(player.lastSeekTime, "Пока поток не загружен (длительность=0), seek не должен вызываться")

            // Метаданные потока загрузились
            player.state.duration = 100.0
            controller.performDriftCorrectionStep()
            assertNotNull(player.lastSeekTime, "После загрузки метаданных дрифт-коррекция должна сработать")
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testEventDrivenBufferingReportsImmediately() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 0.0, initialDuration = 100.0)
        player._isBufferingFlow.value = true
        player.state.isBuffering = true

        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(2000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.start()
            testScheduler.runCurrent()

            // Исходное состояние - буферизация
            assertTrue(rpc.sentCommands.any { it.second is RoomPlaybackCommand.ReportBuffer && (it.second as RoomPlaybackCommand.ReportBuffer).isBuffering })

            // Плеер мгновенно сообщает об окончании буферизации через Flow
            rpc.sentCommands.clear()
            player.setBuffering(false)
            testScheduler.runCurrent()

            // ReportBuffer(false) и ReportMediaReady отправлены мгновенно без задержек
            assertTrue(rpc.sentCommands.any { it.second is RoomPlaybackCommand.ReportBuffer && !(it.second as RoomPlaybackCommand.ReportBuffer).isBuffering }, "ReportBuffer(false) должен быть отправлен сразу")
            assertTrue(rpc.sentCommands.any { it.second is RoomPlaybackCommand.ReportMediaReady }, "ReportMediaReady должен быть отправлен сразу")
        } finally {
            controller.stop()
        }
    }

    @Test
    fun testMediaReadyWhileRoomPausedEnforcesPauseAndAlignsPosition() = runTest {
        val rpc = TestWatchPartyRpcService()
        val player = TestPlaybackController(initialTime = 0.0, initialPlaying = false, initialDuration = 0.0)
        player._isBufferingFlow.value = true
        player.state.isBuffering = true

        val clockSync = ClockSyncService(rpc)
        val currentTime = Instant.fromEpochMilliseconds(2000)
        clockSync.clockProvider = { currentTime }

        val controller = ClientSyncController(
            roomId = Uuid.random(),
            underlyingController = player,
            clockSync = clockSync,
            rpcService = rpc,
            coroutineScope = this
        )

        try {
            controller.start()
            testScheduler.runCurrent()

            // Сервер присылает состояние паузы комнаты на 292 секунде (4:52)
            controller.handleSyncState(
                WatchRoomEvent.SyncState(
                    isPlaying = false,
                    anchorPositionMs = 292000L,
                    anchorServerTime = currentTime
                )
            )
            testScheduler.runCurrent()

            // Поток завершил загрузку и буферизацию
            player.state.duration = 1000.0
            player.setBuffering(false)
            testScheduler.runCurrent()

            // Плеер должен остаться на паузе и быть выровнен на 292.0с
            assertFalse(player.state.isPlaying, "Плеер обязан оставаться на паузе, если комната на паузе")
            assertEquals(292.0, player.lastSeekTime, "Плеер должен выровнять позицию на anchorPositionMs")
            assertTrue(rpc.sentCommands.any { it.second is RoomPlaybackCommand.ReportMediaReady }, "Должен быть отправлен ReportMediaReady")
        } finally {
            controller.stop()
        }
    }
}

