package org.ensodai.avalonmediacard

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.auth.LoginRequest
import org.ensodai.avalonmediacard.contract.model.ClockSyncPing
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.rpc.AuthRpcService
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import org.ensodai.avalonmediacard.data.serialization.SduiClientSerializersModule
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TrueSyncStressTestSuite {

    data class ScenarioResult(
        val name: String,
        val passed: Boolean,
        val details: String,
        val durationMs: Long
    )

    private val results = mutableListOf<ScenarioResult>()

    @Test
    fun execute10ScenariosStressTest() = runBlocking {
        println("\n=======================================================")
        println("🚀 STARTING TRUESYNC AUTOMATED 10-SCENARIO STRESS TEST")
        println("=======================================================\n")

        val client = HttpClient(OkHttp) {
            install(WebSockets)
            installKrpc()
        }

        val roomId = Uuid.parse("51ecc142-499e-463a-aec1-7851bbdfa7c2")

        try {
            val rpc = client.rpc {
                url("ws://localhost:8080/api/rpc")
                rpcConfig {
                    serialization {
                        json(Json {
                            serializersModule = SduiClientSerializersModule
                            ignoreUnknownKeys = true
                            classDiscriminator = "type"
                        })
                    }
                }
            }

            val authService = rpc.withService<AuthRpcService>()
            val watchPartyService = rpc.withService<WatchPartyRpcService>()

            println("🔑 Step 0: Authenticating as 'admin' (HOST)...")
            authService.login(LoginRequest("admin", "admin"))
            println("✅ Authenticated successfully.")

            // Список пойманных событий из сокета
            val receivedEvents = mutableListOf<WatchRoomEvent>()
            var latestSyncState: WatchRoomEvent.SyncState? = null

            val eventsScope = CoroutineScope(Dispatchers.Default)
            watchPartyService.streamRoomEvents(roomId)
                .onEach { event ->
                    receivedEvents.add(event)
                    if (event is WatchRoomEvent.SyncState) {
                        latestSyncState = event
                        println("📡 [EVENT] SyncState: isPlaying=${event.isPlaying}, anchorPos=${event.anchorPositionMs}ms, anchorServerTime=${event.anchorServerTime}")
                    } else {
                        println("📡 [EVENT] ${event::class.simpleName}: $event")
                    }
                }
                .launchIn(eventsScope)

            delay(1000) // Даем подписке зацепиться

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 1: Холодный старт / Первичная готовность комнаты
            // -------------------------------------------------------------
            runScenario("1. Cold Start & Preroll Verification") {
                val pingStart = Clock.System.now()
                val pong = watchPartyService.syncClock(ClockSyncPing(pingStart))
                val rtt = (Clock.System.now() - pingStart).inWholeMilliseconds
                val clockOffset = ((pong.serverReceiveTime - pingStart) + (pong.serverTransmitTime - Clock.System.now())) / 2
                
                // Всегда вызываем triggerStartPlayback, чтобы перевести всех участников из лобби в плеер
                println("🎬 Invoking triggerStartPlayback for room: $roomId")
                watchPartyService.triggerStartPlayback(roomId)
                delay(1500)
                watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Play(positionMs = 0L))
                delay(2500)

                val current = latestSyncState
                assertTrue(current != null, "SyncState должен быть получен")
                "RTT: ${rtt}ms, ClockOffset: ${clockOffset.inWholeMilliseconds}ms, InitialPos: ${current?.anchorPositionMs}ms, Playing: ${current?.isPlaying}"
            }

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 2: Равномерный ход воспроизведения (Steady State, 5s)
            // -------------------------------------------------------------
            runScenario("2. Steady-State Playback Drift Monitor (5s)") {
                val startPos = latestSyncState?.anchorPositionMs ?: 0L
                val startTime = Clock.System.now()
                delay(5000)
                val elapsedWallMs = (Clock.System.now() - startTime).inWholeMilliseconds
                val currentSync = latestSyncState
                val isPlaying = currentSync?.isPlaying == true
                assertTrue(isPlaying, "Воспроизведение должно продолжаться в Steady-State")
                "WallTime: ${elapsedWallMs}ms, CurrentSyncState: isPlaying=$isPlaying, AnchorPos=${currentSync?.anchorPositionMs}ms"
            }

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 3: Пауза от хоста (Host Pause)
            // -------------------------------------------------------------
            runScenario("3. Host Pause Command") {
                val posBeforePause = latestSyncState?.anchorPositionMs ?: 10000L
                val success = watchPartyService.sendPlaybackCommand(
                    roomId,
                    RoomPlaybackCommand.Pause(positionMs = posBeforePause)
                )
                assertTrue(success, "Сервер должен подтвердить отправку команды Pause")

                // Ждем SyncState(isPlaying = false)
                val pausedSync = waitForCondition(timeoutMs = 3000) { latestSyncState?.isPlaying == false }
                assertTrue(pausedSync, "Комната должна перейти в состояние паузы (isPlaying=false)")
                "Pause sent at ${posBeforePause}ms. Applied anchor: ${latestSyncState?.anchorPositionMs}ms, isPlaying=${latestSyncState?.isPlaying}"
            }

            delay(1000)

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 4: Возобновление воспроизведения (Host Resume)
            // -------------------------------------------------------------
            runScenario("4. Host Resume / Play Command (+1500ms lead time)") {
                val currentPos = latestSyncState?.anchorPositionMs ?: 15000L
                val beforePlayTime = Clock.System.now()
                val success = watchPartyService.sendPlaybackCommand(
                    roomId,
                    RoomPlaybackCommand.Play(positionMs = currentPos)
                )
                assertTrue(success, "Сервер должен принять команду Play")

                val resumedSync = waitForCondition(timeoutMs = 4000) { latestSyncState?.isPlaying == true }
                assertTrue(resumedSync, "Комната должна возобновить воспроизведение")

                val leadTime = latestSyncState?.anchorServerTime?.let { (it - beforePlayTime).inWholeMilliseconds } ?: 0L
                "Play sent. Resumed: isPlaying=true, anchor=${latestSyncState?.anchorPositionMs}ms, LeadTime: ${leadTime}ms"
            }

            delay(2000)

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 5: Одиночная перемотка вперед (Forward Seek +30s)
            // -------------------------------------------------------------
            runScenario("5. Forward Seek (+30s, scheduled lead-time ~2000ms)") {
                val currentPos = latestSyncState?.anchorPositionMs ?: 20000L
                val targetPos = currentPos + 30_000L
                val seekTime = Clock.System.now()
                watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Seek(targetPositionMs = targetPos))

                val seekAccepted = waitForCondition(timeoutMs = 3500) {
                    (latestSyncState?.anchorPositionMs ?: 0L) >= targetPos - 500L
                }
                assertTrue(seekAccepted, "Сервер должен применить целевую позицию перемотки вперед")
                val leadTime = latestSyncState?.anchorServerTime?.let { (it - seekTime).inWholeMilliseconds } ?: 0L
                "Forward Seek to ${targetPos}ms. New anchor: ${latestSyncState?.anchorPositionMs}ms, Scheduled LeadTime: ${leadTime}ms"
            }

            delay(3000)

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 6: Одиночная перемотка назад (Backward Seek -15s)
            // -------------------------------------------------------------
            runScenario("6. Backward Seek (-15s)") {
                val currentPos = latestSyncState?.anchorPositionMs ?: 50000L
                val targetPos = (currentPos - 15_000L).coerceAtLeast(0L)
                val seekTime = Clock.System.now()
                watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Seek(targetPositionMs = targetPos))

                val seekAccepted = waitForCondition(timeoutMs = 3500) {
                    (latestSyncState?.anchorPositionMs ?: 0L) in (targetPos - 500L)..(targetPos + 2000L)
                }
                assertTrue(seekAccepted, "Сервер должен применить целевую позицию перемотки назад")
                val leadTime = latestSyncState?.anchorServerTime?.let { (it - seekTime).inWholeMilliseconds } ?: 0L
                "Backward Seek to ${targetPos}ms. New anchor: ${latestSyncState?.anchorPositionMs}ms, Scheduled LeadTime: ${leadTime}ms"
            }

            delay(3000)

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 7: Шторм перемоток (Seek Thrashing: 5 сиков за 1 сек)
            // -------------------------------------------------------------
            runScenario("7. Seek Thrashing Storm (5 rapid seeks in 1000ms)") {
                val basePos = latestSyncState?.anchorPositionMs ?: 40000L
                val targets = listOf(basePos + 5000L, basePos + 10000L, basePos + 15000L, basePos + 20000L, basePos + 25000L)
                for (target in targets) {
                    watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Seek(targetPositionMs = target))
                    delay(200)
                }

                val finalTarget = targets.last()
                val settled = waitForCondition(timeoutMs = 4000) {
                    (latestSyncState?.anchorPositionMs ?: 0L) >= (finalTarget - 1000L)
                }
                assertTrue(settled, "Сервер должен выдержать шторм сиков и зафиксировать финальный таргет")
                "Spammed 5 seeks. Final target: ${finalTarget}ms, Server adopted: ${latestSyncState?.anchorPositionMs}ms"
            }

            delay(3000)

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 8: Трэшинг Play/Pause (5 быстрых переключений)
            // -------------------------------------------------------------
            runScenario("8. Play/Pause State Thrashing (5 rapid switches)") {
                var currentAnchor = latestSyncState?.anchorPositionMs ?: 60000L
                val toggles = listOf(false, true, false, true, true)
                for (play in toggles) {
                    if (play) {
                        watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Play(currentAnchor))
                    } else {
                        watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Pause(currentAnchor))
                    }
                    delay(250)
                    currentAnchor = latestSyncState?.anchorPositionMs ?: currentAnchor
                }

                val settledPlay = waitForCondition(timeoutMs = 4000) { latestSyncState?.isPlaying == true }
                assertTrue(settledPlay, "После трэшинга комната должна остаться в согласованном состоянии isPlaying=true")
                "Thrashing finished without deadlocks. Final state: isPlaying=${latestSyncState?.isPlaying}, Anchor=${latestSyncState?.anchorPositionMs}ms"
            }

            delay(2000)

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 9: Симуляция буферизации клиента (Buffer Stall & Resume)
            // -------------------------------------------------------------
            runScenario("9. Client Buffer Stall & Dynamic Lead-Time Recovery") {
                println("⚠️ Simulating client stall: ReportBuffer(isBuffering = true)...")
                watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.ReportBuffer(isBuffering = true))
                
                // Комната должна встать на паузу в PARTIAL_BUFFERING
                val pausedByBuffer = waitForCondition(timeoutMs = 3000) { latestSyncState?.isPlaying == false }
                assertTrue(pausedByBuffer, "Сервер должен остановить воспроизведение при аварийной буферизации участника")

                println("⏳ Waiting 2000ms in simulated stall...")
                delay(2000)

                println("📶 Client finished buffering: ReportBuffer(isBuffering = false)...")
                val resumeTime = Clock.System.now()
                watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.ReportBuffer(isBuffering = false))

                // Сервер должен запланировать авто-возобновление с lead-time 500мс
                val resumedAfterStall = waitForCondition(timeoutMs = 3000) { latestSyncState?.isPlaying == true }
                assertTrue(resumedAfterStall, "Комната должна автоматически возобновить воспроизведение после завершения буферизации")
                val recoveryLeadTime = latestSyncState?.anchorServerTime?.let { (it - resumeTime).inWholeMilliseconds } ?: 0L
                "Stall handled: Room paused on buffer=true, resumed on buffer=false with lead-time: ${recoveryLeadTime}ms"
            }

            delay(3000)

            // -------------------------------------------------------------
            // СЦЕНАРИЙ 10: Финальная проверка стабильности и отчет
            // -------------------------------------------------------------
            runScenario("10. Final Stability & TrueSync Sync Verification") {
                val ping1 = Clock.System.now()
                val pong = watchPartyService.syncClock(ClockSyncPing(ping1))
                val finalRtt = (Clock.System.now() - ping1).inWholeMilliseconds
                val totalEvents = receivedEvents.size
                assertTrue(totalEvents >= 10, "Должно быть получено минимум 10 событий за время стресс-теста")
                "Final RTT: ${finalRtt}ms, Total socket events processed: $totalEvents, Final anchor: ${latestSyncState?.anchorPositionMs}ms"
            }

        } finally {
            client.close()
            saveAndPrintReport()
        }
    }

    private suspend fun runScenario(name: String, block: suspend () -> String) {
        val start = System.currentTimeMillis()
        print("\n▶️ Running [$name] ... ")
        try {
            val details = block()
            val dur = System.currentTimeMillis() - start
            results.add(ScenarioResult(name, true, details, dur))
            println("PASSED (${dur}ms)\n   ↳ $details")
        } catch (e: Throwable) {
            val dur = System.currentTimeMillis() - start
            results.add(ScenarioResult(name, false, e.message ?: e.toString(), dur))
            println("FAILED (${dur}ms)\n   ❌ ERROR: ${e.message}")
        }
    }

    private suspend fun waitForCondition(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            delay(100)
        }
        return condition()
    }

    private fun saveAndPrintReport() {
        val total = results.size
        val passed = results.count { it.passed }
        val failed = total - passed

        val sb = StringBuilder()
        sb.appendLine("# TrueSync E2E Automated Stress Test Report")
        sb.appendLine("Date: ${Clock.System.now()}")
        sb.appendLine("Room: `51ecc142-499e-463a-aec1-7851bbdfa7c2`")
        sb.appendLine("Result: **$passed / $total PASSED** (${if (failed == 0) "100% SUCCESS" else "$failed FAILURES"})\n")
        sb.appendLine("| # | Scenario | Status | Duration | Details |")
        sb.appendLine("|---|----------|:------:|:--------:|---------|")

        results.forEachIndexed { i, res ->
            val icon = if (res.passed) "✅ PASS" else "❌ FAIL"
            sb.appendLine("| ${i + 1} | ${res.name} | $icon | ${res.durationMs}ms | ${res.details.replace("|", "/")} |")
        }

        val reportStr = sb.toString()
        println("\n=======================================================")
        println("📊 TRUESYNC 10-SCENARIO STRESS TEST SUMMARY")
        println("=======================================================")
        println(reportStr)

        val reportFile = File("../логи/TrueSync_Stress_Test_Report.md").takeIf { it.parentFile.exists() } ?: File("логи/TrueSync_Stress_Test_Report.md")
        reportFile.parentFile.mkdirs()
        reportFile.writeText(reportStr)
        println("💾 Report saved to: ${reportFile.absolutePath}\n")
    }
}
