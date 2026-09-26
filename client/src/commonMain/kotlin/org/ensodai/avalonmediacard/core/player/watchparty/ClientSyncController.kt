package org.ensodai.avalonmediacard.core.player.watchparty

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.core.PlaybackState
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Клиентский контроллер алгоритма TrueSync для режима совместного просмотра (Watch Party).
 * Реализует:
 * - Управление фазой предстартовой буферизации (Preroll / PlayAt)
 * - Пропорциональное микрорегулирование скорости воспроизведения (Deadband, P-controller)
 * - Жесткий Seek при критическом расхождении (>4000 мс)
 * - Сбор и отправку телеметрии буферизации участников
 * - Делегирующий PlaybackController для прозрачной интеграции с UI плеера
 */
class ClientSyncController(
    val roomId: Uuid,
    val underlyingController: PlaybackController,
    val clockSync: ClockSyncService,
    val rpcService: WatchPartyRpcService,
    val coroutineScope: CoroutineScope
) {
    private val logger = AppLogging.logger("ClientSyncController")

    private var eventSubscriptionJob: Job? = null
    private var driftLoopJob: Job? = null
    private var prerollJob: Job? = null
    private var bufferMonitorJob: Job? = null

    var latestSyncState: WatchRoomEvent.SyncState? = null
        private set

    var isPrerolling: Boolean = false
        private set

    var isAttached: Boolean = false
        private set

    private var lastReportedBuffering: Boolean? = null
    private var hasReportedMediaReady: Boolean = false

    private val _roomEvents = MutableSharedFlow<WatchRoomEvent>(extraBufferCapacity = 64)
    val roomEvents: SharedFlow<WatchRoomEvent> = _roomEvents.asSharedFlow()

    var lastHardSeekTime: Instant? = null
        internal set

    companion object {
        const val DEADBAND_MS = 30L
        const val PREROLL_SEEK_THRESHOLD_MS = 250L
        const val PAUSE_SEEK_THRESHOLD_MS = 250L
        const val HARD_SEEK_THRESHOLD_MS = 4000L
        const val HARD_SEEK_COOLDOWN_MS = 4000L
        const val KP = 0.0001f
        const val MIN_PLAYBACK_RATE = 0.85f
        const val MAX_PLAYBACK_RATE = 1.25f
        val HARD_SEEK_COOLDOWN: Duration = 4000.milliseconds
        val SYNC_LOOP_INTERVAL: Duration = 500.milliseconds
    }

    /**
     * Запуск синхронизации: фоновый опрос часов, подписка на события комнаты и мониторинг буфера.
     */
    fun start() {
        if (isAttached) return
        isAttached = true

        clockSync.startPeriodicSync(coroutineScope)
        subscribeToRoomEvents()
        monitorBuffering()
        logger.i { "TrueSync started for room $roomId" }
    }

    /**
     * Остановка синхронизации и сброс скорости плеера в 1.0x.
     */
    fun stop() {
        isAttached = false

        eventSubscriptionJob?.cancel()
        eventSubscriptionJob = null

        stopDriftLoop()
        prerollJob?.cancel()
        prerollJob = null

        bufferMonitorJob?.cancel()
        bufferMonitorJob = null

        clockSync.stopPeriodicSync()
        lastHardSeekTime = null
        hasReportedMediaReady = false
        lastReportedBuffering = null
        underlyingController.setPlaybackRate(1.0f)
        logger.i { "TrueSync stopped for room $roomId" }
    }

    /**
     * Обработка события синхронизации состояния комнаты от сервера.
     */
    fun handleSyncState(syncState: WatchRoomEvent.SyncState) {
        val isEpisodeChanged = latestSyncState != null &&
            (latestSyncState?.season != syncState.season || latestSyncState?.episode != syncState.episode)
        if (isEpisodeChanged) {
            hasReportedMediaReady = false
            lastReportedBuffering = null
        }
        latestSyncState = syncState
        val serverNow = clockSync.estimatedServerTime()
        val localNow = clockSync.clockProvider()
        val offsetMs = clockSync.offset?.inWholeMilliseconds

        logger.i {
            "[TrueSync:Client] Received SyncState for room $roomId: isPlaying=${syncState.isPlaying}, " +
                "anchorPos=${syncState.anchorPositionMs} ms, anchorServerTime=${syncState.anchorServerTime} | " +
                "localNow=$localNow, estimatedServerNow=$serverNow, clockOffset=${offsetMs}ms"
        }

        if (!syncState.isPlaying) {
            // Режим паузы
            stopDriftLoop()
            prerollJob?.cancel()
            prerollJob = null
            isPrerolling = false

            underlyingController.pause()
            val currentPosMs = underlyingController.getCurrentPositionMs()
            val drift = syncState.anchorPositionMs - currentPosMs
            logger.i {
                "[TrueSync:Client] PAUSE applied. Current player pos = $currentPosMs ms, target = ${syncState.anchorPositionMs} ms (drift = ${drift} ms)"
            }
            if (abs(drift) > PAUSE_SEEK_THRESHOLD_MS) {
                safeSeek(syncState.anchorPositionMs / 1000.0, "Pause drift correction")
            }
            underlyingController.setPlaybackRate(1.0f)
        } else {
            // Режим воспроизведения
            if (syncState.anchorServerTime > serverNow) {
                // Фаза Preroll: старт запланирован на будущее время
                startPreroll(syncState)
            } else {
                // Старт в прошлом: немедленное воспроизведение с регулированием дрифта
                val diffMs = (serverNow - syncState.anchorServerTime).inWholeMilliseconds
                logger.i {
                    "[TrueSync:Client] Immediate PLAY (anchorServerTime is $diffMs ms in the past). Starting drift loop."
                }
                isPrerolling = false
                prerollJob?.cancel()
                prerollJob = null
                underlyingController.play()
                startDriftLoop()
            }
        }
    }

    private fun safeSeek(targetPosSec: Double, reason: String) {
        val now = clockSync.clockProvider()
        val lastSeek = lastHardSeekTime
        val currentSec = underlyingController.getCurrentPositionMs() / 1000.0
        val diffSec = abs(currentSec - targetPosSec)

        if (diffSec < 0.25) {
            return
        }

        if (underlyingController.state.isBuffering) {
            // Во время буферизации мелкие корректировки (< 2с) игнорируются,
            // чтобы не срывать (abort) загрузку сегментов в HLS/MSE веб-воркере.
            if (diffSec < 2.0) {
                logger.d {
                    "[TrueSync:Client] Seek skipped ($reason): player is buffering and target is close (${diffSec}s < 2.0s)"
                }
                return
            }
            if (lastSeek != null && (now - lastSeek) < HARD_SEEK_COOLDOWN) {
                logger.d {
                    "[TrueSync:Client] Seek skipped ($reason): player is buffering and seek cooldown is active (${(now - lastSeek).inWholeMilliseconds} ms < ${HARD_SEEK_COOLDOWN_MS} ms)"
                }
                return
            }
        }

        lastHardSeekTime = now
        logger.i { "[TrueSync:Client] Seek applied ($reason): ${currentSec}s -> ${targetPosSec}s" }
        underlyingController.seek(targetPosSec)
    }

    private fun startPreroll(syncState: WatchRoomEvent.SyncState) {
        stopDriftLoop()
        prerollJob?.cancel()
        isPrerolling = true

        val startPosMs = syncState.anchorPositionMs
        val startPosSec = startPosMs / 1000.0
        val currentPosMs = underlyingController.getCurrentPositionMs()
        underlyingController.pause()
        if (abs(currentPosMs - startPosMs) > PREROLL_SEEK_THRESHOLD_MS) {
            safeSeek(startPosSec, "Preroll target seek")
        } else {
            logger.i { "[TrueSync:Client] Preroll: already at position $currentPosMs ms (~$startPosMs ms), skipping redundant seek" }
        }
        underlyingController.setPlaybackRate(1.0f)

        prerollJob = coroutineScope.launch {
            val serverNow = clockSync.estimatedServerTime()
            val waitDuration = syncState.anchorServerTime - serverNow
            logger.i {
                "[TrueSync:Client] Preroll START: seek to $startPosSec s (${syncState.anchorPositionMs} ms). " +
                    "Waiting ${waitDuration.inWholeMilliseconds} ms until anchorServerTime=${syncState.anchorServerTime} (serverNow=$serverNow)"
            }
            if (waitDuration.isPositive()) {
                delay(waitDuration)
            }
            isPrerolling = false
            val wakeServerNow = clockSync.estimatedServerTime()
            val wakeLocalNow = clockSync.clockProvider()
            val actualPosMs = underlyingController.getCurrentPositionMs()
            val stillBuffering = underlyingController.state.isBuffering
            logger.i {
                "[TrueSync:Client] Preroll FINISHED -> localNow=$wakeLocalNow, serverNow=$wakeServerNow, " +
                    "playerPos=$actualPosMs ms, targetAnchorPos=${syncState.anchorPositionMs} ms, stillBuffering=$stillBuffering"
            }
            if (stillBuffering) {
                // Преролл завершен, но видеопоток еще не готов (буферизуется в MSE/TorrServer).
                // Немедленно уведомляем сервер, чтобы комната встала в PARTIAL_BUFFERING и не убегала по времени!
                lastReportedBuffering = true
                try {
                    rpcService.sendPlaybackCommand(
                        roomId,
                        RoomPlaybackCommand.ReportBuffer(isBuffering = true)
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.w(e) { "Failed to report buffer stall after preroll" }
                }
            } else if (underlyingController.state.duration > 0.0) {
                underlyingController.play()
                startDriftLoop()
            }
        }
    }

    private fun startDriftLoop() {
        if (driftLoopJob?.isActive == true) return
        driftLoopJob = coroutineScope.launch {
            while (isActive) {
                delay(SYNC_LOOP_INTERVAL)
                performDriftCorrectionStep()
            }
        }
    }

    private fun stopDriftLoop() {
        driftLoopJob?.cancel()
        driftLoopJob = null
    }

    /**
     * Вычисляет расхождение и применяет корректировку (Deadband, P-rate или Hard Seek).
     */
    internal fun performDriftCorrectionStep() {
        val state = latestSyncState ?: return
        if (!state.isPlaying || isPrerolling) return

        // Инвариант TrueSync: не корректировать дрейф во время буферизации/перемотки
        if (underlyingController.state.isBuffering) {
            return
        }

        // Если поток еще не загружен (длительность нулевая и позиция нулевая), ожидаем готовности плеера
        val currentPositionMs = underlyingController.getCurrentPositionMs()
        if (underlyingController.state.duration <= 0.0 && currentPositionMs <= 0L) {
            return
        }

        val serverNow = clockSync.estimatedServerTime()
        val timePassedMs = (serverNow - state.anchorServerTime).inWholeMilliseconds
        val targetPositionMs = state.anchorPositionMs + timePassedMs

        val deltaMs = targetPositionMs - currentPositionMs

        when {
            // Критическое расхождение: мгновенный Seek с защитой от Seek Thrashing (Cooldown 4с)
            abs(deltaMs) > HARD_SEEK_THRESHOLD_MS -> {
                val now = clockSync.clockProvider()
                val lastSeek = lastHardSeekTime
                if (lastSeek != null && (now - lastSeek) < HARD_SEEK_COOLDOWN) {
                    logger.d {
                        "[TrueSync:Client] Hard seek throttled (cooldown active, delta=$deltaMs ms, elapsed=${(now - lastSeek).inWholeMilliseconds} ms)"
                    }
                    return
                }

                logger.w { "[TrueSync:Client] Drift critical ($deltaMs ms > $HARD_SEEK_THRESHOLD_MS ms). Hard seek to $targetPositionMs ms (rate=1.0x)" }
                safeSeek(targetPositionMs / 1000.0, "Drift critical ($deltaMs ms)")
                underlyingController.setPlaybackRate(1.0f)
            }
            // Мертвая зона: расхождение незаметно человеку
            abs(deltaMs) <= DEADBAND_MS -> {
                underlyingController.setPlaybackRate(1.0f)
            }
            // Пропорциональное микрорегулирование скорости
            else -> {
                val computedRate = 1.0f + (KP * deltaMs)
                val clampedRate = computedRate.coerceIn(MIN_PLAYBACK_RATE, MAX_PLAYBACK_RATE)
                logger.i {
                    "[TrueSync:Client] Drift active: delta=$deltaMs ms (target=$targetPositionMs ms, current=$currentPositionMs ms) -> rate=${clampedRate}x"
                }
                underlyingController.setPlaybackRate(clampedRate)
            }
        }
    }

    private fun subscribeToRoomEvents() {
        eventSubscriptionJob?.cancel()
        eventSubscriptionJob = coroutineScope.launch {
            try {
                rpcService.streamRoomEvents(roomId).collect { event ->
                    _roomEvents.emit(event)
                    when (event) {
                        is WatchRoomEvent.SyncState -> handleSyncState(event)
                        is WatchRoomEvent.ParticipantsUpdated,
                        is WatchRoomEvent.ReactionTriggered,
                        is WatchRoomEvent.SystemNotice -> {
                            // Передаются подписчикам через flow roomEvents
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w(e) { "Error in room events subscription for room $roomId" }
            }
        }
    }

    private fun monitorBuffering() {
        bufferMonitorJob?.cancel()
        bufferMonitorJob = coroutineScope.launch {
            underlyingController.isBufferingFlow.collect { isBuffering ->
                // Во время Preroll изоляция: не отправляем ReportBuffer(true) при штатной подготовке чанков
                if (isPrerolling && isBuffering) return@collect

                val isDurationValid = underlyingController.state.duration > 0.0
                if (isBuffering != lastReportedBuffering) {
                    lastReportedBuffering = isBuffering
                    try {
                        rpcService.sendPlaybackCommand(
                            roomId,
                            RoomPlaybackCommand.ReportBuffer(isBuffering = isBuffering)
                        )
                        if (!isBuffering && isDurationValid && !hasReportedMediaReady) {
                            hasReportedMediaReady = true
                            val currentMs = underlyingController.getCurrentPositionMs()
                            rpcService.sendPlaybackCommand(
                                roomId,
                                RoomPlaybackCommand.ReportMediaReady(positionMs = currentMs)
                            )
                            val state = latestSyncState
                            if (state != null && state.isPlaying && !isPrerolling) {
                                underlyingController.play()
                                startDriftLoop()
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.w(e) { "Failed to send buffer telemetry to room $roomId" }
                    }
                } else if (!isBuffering && !hasReportedMediaReady && isDurationValid) {
                    hasReportedMediaReady = true
                    try {
                        val currentMs = underlyingController.getCurrentPositionMs()
                        rpcService.sendPlaybackCommand(
                            roomId,
                            RoomPlaybackCommand.ReportMediaReady(positionMs = currentMs)
                        )
                        val state = latestSyncState
                        if (state != null && state.isPlaying && !isPrerolling) {
                            underlyingController.play()
                            startDriftLoop()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.w(e) { "Failed to send media ready telemetry to room $roomId" }
                    }
                }
            }
        }
    }

    // === Пользовательские действия (отправка команд на сервер) ===

    suspend fun reportMediaReady(positionMs: Long = 0L) {
        hasReportedMediaReady = true
        rpcService.sendPlaybackCommand(roomId, RoomPlaybackCommand.ReportMediaReady(positionMs))
    }

    suspend fun sendPlay() {
        val currentMs = underlyingController.getCurrentPositionMs()
        logger.i { "[TrueSync:Client] Sending Play command to room $roomId at pos $currentMs ms" }
        rpcService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Play(currentMs))
    }

    suspend fun sendPause() {
        val currentMs = underlyingController.getCurrentPositionMs()
        logger.i { "[TrueSync:Client] Sending Pause command to room $roomId at pos $currentMs ms" }
        rpcService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Pause(currentMs))
    }

    suspend fun sendSeek(targetSeconds: Double) {
        val targetMs = (targetSeconds * 1000).toLong().coerceAtLeast(0L)
        logger.i { "[TrueSync:Client] Sending Seek command to room $roomId to $targetSeconds s ($targetMs ms)" }
        rpcService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Seek(targetMs))
    }

    suspend fun sendReaction(emoji: String) {
        rpcService.sendReaction(roomId, emoji)
    }

    /**
     * Создает обертку PlaybackController, перехватывающую play, pause и seek
     * для маршрутизации через RPC в режиме Watch Party.
     */
    fun createInterceptingController(): PlaybackController {
        return object : PlaybackController {
            override val state: PlaybackState
                get() = underlyingController.state

            override val isBufferingFlow: StateFlow<Boolean>
                get() = underlyingController.isBufferingFlow

            override fun getCurrentPositionMs(): Long {
                return underlyingController.getCurrentPositionMs()
            }

            override fun play() {
                coroutineScope.launch { sendPlay() }
            }

            override fun pause() {
                coroutineScope.launch { sendPause() }
            }

            override fun togglePlayPause() {
                if (state.isPlaying) pause() else play()
            }

            override fun seek(time: Double) {
                coroutineScope.launch { sendSeek(time) }
            }

            override fun setMuted(muted: Boolean) {
                underlyingController.setMuted(muted)
            }

            override fun setVolume(volume: Double) {
                underlyingController.setVolume(volume)
            }

            override fun setPlaybackRate(rate: Float) {
                underlyingController.setPlaybackRate(rate)
            }

            override fun sendKeyPress(key: String) {
                underlyingController.sendKeyPress(key)
            }

            override fun stop() {
                underlyingController.stop()
            }

            override val audioTracks: List<AudioTrack>
                get() = underlyingController.audioTracks

            override val selectedAudioTrack: AudioTrack?
                get() = underlyingController.selectedAudioTrack

            override fun selectAudioTrack(track: AudioTrack) {
                underlyingController.selectAudioTrack(track)
            }

            override val subtitleTracks: List<SubtitleTrack>
                get() = underlyingController.subtitleTracks

            override val selectedSubtitleTrack: SubtitleTrack?
                get() = underlyingController.selectedSubtitleTrack

            override fun selectSubtitleTrack(track: SubtitleTrack?) {
                underlyingController.selectSubtitleTrack(track)
            }

            override fun setTracks(audioTracks: List<AudioTrack>, subtitleTracks: List<SubtitleTrack>) {
                underlyingController.setTracks(audioTracks, subtitleTracks)
            }
        }
    }
}
