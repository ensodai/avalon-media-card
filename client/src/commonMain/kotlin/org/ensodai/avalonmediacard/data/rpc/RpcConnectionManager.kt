package org.ensodai.avalonmediacard.data.rpc

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.rpc.RpcClient
import kotlinx.rpc.krpc.client.KrpcClient
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.rpc.AuthRpcService
import org.ensodai.avalonmediacard.data.TokenStorage
import org.ensodai.avalonmediacard.data.platformServerUrl
import org.ensodai.avalonmediacard.data.serialization.SduiClientSerializersModule

open class RpcConnectionManager(
    private val tokenStorage: TokenStorage,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val logger = AppLogging.logger("RpcConnectionManager")

    private val serverUrl: String
        get() = tokenStorage.cachedServerUrl?.takeIf { it.isNotBlank() } ?: platformServerUrl

    private val httpClient = HttpClient {
        installKrpc()
        install(WebSockets) {
            pingIntervalMillis = 15_000L
        }
    }

    private var rpcClient: RpcClient? = null
    private val mutex = Mutex()

    protected val _connectionState = MutableStateFlow<RpcConnectionState>(RpcConnectionState.Idle)
    open val connectionState: StateFlow<RpcConnectionState> = _connectionState.asStateFlow()

    val reconnectSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        scope.launch {
            runCatching { getActiveClient() }
        }
    }

    suspend fun getClient(): RpcClient = getActiveClient()

    /**
     * Возвращает активный RPC-клиент или мгновенно создает новое подключение (SingleFlight).
     */
    open suspend fun getActiveClient(): RpcClient {
        // Fast-path без блокировки мьютекса: если сокет уже подключен и активен
        val current = rpcClient
        if (current != null &&
            (current as? CoroutineScope)?.isActive != false &&
            _connectionState.value is RpcConnectionState.Connected
        ) {
            return current
        }

        return mutex.withLock {
            val existing = rpcClient
            val isAlive = existing != null &&
                (existing as? CoroutineScope)?.isActive != false &&
                _connectionState.value is RpcConnectionState.Connected

            if (isAlive) {
                return@withLock existing
            }

            if (existing != null) {
                closeClient(existing)
                rpcClient = null
            }

            _connectionState.value = RpcConnectionState.Connecting
            val client: RpcClient
            try {
                client = createRpcClient()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.e(e) { "🔴 Failed to open WebSocket connection: ${e.message}" }
                _connectionState.value = RpcConnectionState.Error(e.message ?: "Connection error")
                throw e
            }

            val token = tokenStorage.cachedToken
            if (!token.isNullOrBlank()) {
                _connectionState.value = RpcConnectionState.Authenticating
                logger.d { "Token found locally. Authenticating inside channel..." }
                try {
                    val authService = client.withService<AuthRpcService>()
                    val authResponse = authService.authenticate(token)
                    if (authResponse == null || !authResponse.success) {
                        logger.w { "Token rejected by server as invalid. Clearing stored token." }
                        tokenStorage.clearToken()
                    } else {
                        tokenStorage.saveToken(
                            token = token,
                            role = authResponse.role,
                            userId = authResponse.userId,
                            username = authResponse.username
                        )
                        logger.d { "Channel successfully authenticated!" }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.w(e) { "Transient network error during authentication: ${e.message}. Keeping stored token." }
                    closeClient(client)
                    _connectionState.value = RpcConnectionState.Error(e.message ?: "Auth error")
                    throw e
                }
            } else {
                logger.d { "Connected as guest (anonymous RPC channel)." }
            }

            // Автоматический сброс rpcClient при закрытии корутинного скоупа сокета
            if (client is CoroutineScope) {
                client.coroutineContext.job.invokeOnCompletion { cause ->
                    scope.launch {
                        mutex.withLock {
                            if (rpcClient === client) {
                                logger.w { "Active RPC client closed (${cause?.message ?: "normal"}). Resetting rpcClient." }
                                rpcClient = null
                                _connectionState.value = RpcConnectionState.Idle
                            }
                        }
                    }
                }
            }

            val old = rpcClient
            rpcClient = client
            if (old != null && old !== client) {
                closeClient(old)
            }

            _connectionState.value = RpcConnectionState.Connected
            logger.i { "🟢 RPC Connection ESTABLISHED and READY! (hasToken=${!token.isNullOrBlank()})" }
            client
        }
    }

    fun start() {
        logger.d { "🚀 Start called -> warming up connection..." }
        scope.launch {
            runCatching { getActiveClient() }
        }
    }

    fun disconnect() {
        logger.d { "🛑 Disconnect called -> clearing connection..." }
        invalidate()
    }

    fun triggerInstantReconnect() {
        logger.d { "⚡ Triggering instant 0ms reconnect!" }
        invalidate()
        reconnectSignal.tryEmit(Unit)
        scope.launch {
            runCatching { getActiveClient() }
        }
    }

    fun forceReconnect() {
        logger.d { "🔄 Forcing full socket reconnect..." }
        triggerInstantReconnect()
    }

    fun onAppForeground() {
        logger.d { "📱 App entered FOREGROUND -> forcing instant reconnect..." }
        triggerInstantReconnect()
    }

    fun onNetworkAvailable() {
        logger.d { "📶 Network AVAILABLE -> forcing instant reconnect..." }
        triggerInstantReconnect()
    }

    fun notifyStreamFailure(e: Throwable) {
        logger.w(e) { "⚠️ Stream failure reported: ${e.message}. Invalidating dead connection and triggering reconnect..." }
        triggerInstantReconnect()
    }

    fun invalidate() {
        val old = rpcClient
        rpcClient = null
        _connectionState.value = RpcConnectionState.Idle
        closeClient(old)
    }

    fun clearConnection() {
        invalidate()
    }

    protected open suspend fun createRpcClient(): RpcClient {
        logger.d { "Connecting RPC socket to $serverUrl" }
        return httpClient.rpc {
            url(serverUrl)
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
    }

    private fun closeClient(client: RpcClient?) {
        if (client == null) return
        try {
            if (client is AutoCloseable) client.close()
            (client as? KrpcClient)?.close()
        } catch (e: Exception) {
            logger.w(e) { "Failed to close RPC client: ${e.message}" }
        }
    }
}
