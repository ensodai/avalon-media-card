package org.ensodai.avalonmediacard

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
class ChaosReconnectTestSuite {

    private val roomId = Uuid.parse("51ecc142-499e-463a-aec1-7851bbdfa7c2")
    private val signalDir = run {
        val userDir = File(System.getProperty("user.dir", ".")).canonicalFile
        val root = if (userDir.name == "client") userDir.parentFile else userDir
        File(root, "логи").apply { mkdirs() }
    }

    private fun signalOfflineReq(): File = File(signalDir, "signal_offline.req")
    private fun signalOfflineAck(): File = File(signalDir, "signal_offline.ack")
    private fun signalReconnectReq(): File = File(signalDir, "signal_reconnect.req")
    private fun signalReconnectAck(): File = File(signalDir, "signal_reconnect.ack")

    private fun cleanSignals() {
        signalOfflineReq().delete()
        signalOfflineAck().delete()
        signalReconnectReq().delete()
        signalReconnectAck().delete()
    }

    private suspend fun waitForSignal(file: File, timeoutMs: Long = 30000L): Boolean {
        val start = Clock.System.now()
        while ((Clock.System.now() - start).inWholeMilliseconds < timeoutMs) {
            if (file.exists()) {
                file.delete()
                return true
            }
            delay(100)
        }
        return false
    }

    private suspend fun waitForCondition(timeoutMs: Long = 20000L, predicate: suspend () -> Boolean): Boolean {
        val start = Clock.System.now()
        while ((Clock.System.now() - start).inWholeMilliseconds < timeoutMs) {
            if (predicate()) return true
            delay(100)
        }
        return false
    }

    @Test
    fun executeChaosReconnectTest() = runBlocking {
        println("\n=======================================================")
        println("🔥 STARTING TRUESYNC CHAOS NETWORK & HOT RECONNECT TEST")
        println("=======================================================\n")
        println("📁 Signal directory path: ${signalDir.absolutePath}")

        cleanSignals()

        val client = HttpClient(OkHttp) {
            install(WebSockets)
            installKrpc()
        }

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

            println("🔑 Step 0: Authenticating as 'admin' (Host)...")
            authService.login(LoginRequest("admin", "admin"))
            println("✅ Authenticated.")

            var latestSyncState: WatchRoomEvent.SyncState? = null
            val eventsScope = CoroutineScope(Dispatchers.Default)

            watchPartyService.streamRoomEvents(roomId)
                .onEach { event ->
                    if (event is WatchRoomEvent.SyncState) {
                        latestSyncState = event
                        println("📡 [RPC EVENT] SyncState: isPlaying=${event.isPlaying}, anchorPos=${event.anchorPositionMs}ms, anchorServerTime=${event.anchorServerTime}")
                    }
                }
                .launchIn(eventsScope)

            delay(1000)

            // 1. Старт воспроизведения
            println("🎬 Step 1: Triggering room playback and initial Play(0)...")
            watchPartyService.triggerStartPlayback(roomId)
            delay(1500)
            watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Play(positionMs = 0L))

            // 2. Ожидание стабильного воспроизведения (Steady State)
            println("⏱️ Step 2: Waiting for steady playback (isPlaying=true)...")
            val playingStarted = waitForCondition(timeoutMs = 15000L) {
                latestSyncState?.isPlaying == true
            }
            assertTrue(playingStarted, "Комната должна перейти в режим воспроизведения (isPlaying=true)")
            println("▶️ Playback active. Letting it run for 5 seconds...")
            delay(5000)
            println("✅ Steady playback confirmed. Host position: ${latestSyncState?.anchorPositionMs}ms")

            // 3. Фаза 1: Сигнал на отключение зрителя (Offline Drop)
            println("🚨 Step 3: Signaling Playwright to disconnect Firefox (OFFLINE DROP)...")
            signalOfflineReq().createNewFile()
            val offlineAckReceived = waitForSignal(signalOfflineAck(), timeoutMs = 20000L)
            assertTrue(offlineAckReceived, "Playwright должен подтвердить отключение сети у Firefox")
            println("💥 Firefox is now OFFLINE!")

            // 4. Фаза 2: Действия хоста в изоляции
            println("⚡ Step 4: Host actions while viewer is isolated...")
            delay(2000)
            println("⏩ Host performs SEEK forward to 35.0 seconds!")
            watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Seek(targetPositionMs = 35_000L))
            delay(4000)
            println("▶️ Host continues playing from position: ${latestSyncState?.anchorPositionMs}ms")

            // 5. Фаза 3: Сигнал на горячий реконнект зрителя
            println("🌐 Step 5: Signaling Playwright to reconnect Firefox (HOT RECONNECT)...")
            signalReconnectReq().createNewFile()
            val reconnectAckReceived = waitForSignal(signalReconnectAck(), timeoutMs = 20000L)
            assertTrue(reconnectAckReceived, "Playwright должен подтвердить включение сети у Firefox")
            println("🎉 Firefox is now back ONLINE!")

            // 6. Фаза 4: Наблюдение за догоном (Hard Seek & Drift Recovery)
            println("🎯 Step 6: Observing TrueSync drift recovery post-reconnect (10 seconds)...")
            delay(10000)

            val currentSync = latestSyncState
            assertTrue(currentSync != null && currentSync.isPlaying, "Воспроизведение должно продолжаться после реконнекта")
            println("🏁 Final room sync state: isPlaying=${currentSync.isPlaying}, anchorPos=${currentSync.anchorPositionMs}ms")

            // Мягкая остановка
            watchPartyService.sendPlaybackCommand(roomId, RoomPlaybackCommand.Pause(positionMs = currentSync.anchorPositionMs))
            delay(1000)

        } finally {
            cleanSignals()
            client.close()
            println("✅ ChaosReconnectTestSuite finished.")
        }
    }
}
