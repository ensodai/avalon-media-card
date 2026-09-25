package org.ensodai.avalonmediacard

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.auth.LoginRequest
import org.ensodai.avalonmediacard.contract.rpc.AuthRpcService
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import kotlin.test.Test
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi
import io.ktor.client.request.url

@OptIn(ExperimentalUuidApi::class)
class TriggerWatchPartyE2E {
    @Test
    fun triggerStart() = runBlocking {
        println("Starting E2E Trigger script...")
        val client = HttpClient(OkHttp) {
            install(WebSockets)
            installKrpc()
        }
        
        try {
            val rpc = client.rpc {
                url("ws://localhost:8080/api/rpc")
                rpcConfig {
                    serialization {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
            }
            
            val authService = rpc.withService<AuthRpcService>()
            println("Attempting login as admin...")
            val loginRes = authService.login(LoginRequest("admin", "admin"))
            println("Login completed. Result type: ${loginRes::class.simpleName}")
            
            val watchPartyService = rpc.withService<WatchPartyRpcService>()
            val roomId = Uuid.parse("51ecc142-499e-463a-aec1-7851bbdfa7c2")
            println("Triggering playback for room $roomId...")
            
            val result = watchPartyService.triggerStartPlayback(roomId)
            println("Trigger result: $result")
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            client.close()
            println("E2E Trigger script finished.")
        }
    }
}
