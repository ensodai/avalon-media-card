package org.ensodai.avalonmediacard

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.KrpcRoute
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.rpc.ActionRpcService
import org.ensodai.avalonmediacard.contract.rpc.AdminRpcService
import org.ensodai.avalonmediacard.contract.rpc.AuthRpcService
import org.ensodai.avalonmediacard.contract.rpc.PlaybackRpcService
import org.ensodai.avalonmediacard.contract.rpc.SduiRpcService
import org.ensodai.avalonmediacard.contract.rpc.TelemetryRpcService
import org.ensodai.avalonmediacard.contract.rpc.UserSettingsRpcService
import org.ensodai.avalonmediacard.database.DatabaseFactory
import org.ensodai.avalonmediacard.di.koinPlugin
import org.ensodai.avalonmediacard.plugin.PluginManager
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.ensodai.avalonmediacard.repository.UserRepository
import org.ensodai.avalonmediacard.routes.imageProxyRoutes
import org.ensodai.avalonmediacard.routes.streamProxyRoutes
import org.ensodai.avalonmediacard.rpc.*
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.ensodai.avalonmediacard.security.StreamTokenService
import org.ensodai.avalonmediacard.sync.SyncWorker
import org.ensodai.avalonmediacard.tmdb.MediaKeywordsEnrichmentWorker
import org.ensodai.avalonmediacard.utils.EnvHelper
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import java.io.File

fun main() {
    val port = EnvHelper.getEnv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

val RpcSessionContextKey = AttributeKey<RpcSessionContext>("RpcSessionContext")

fun KrpcRoute.getOrCreateSessionContext(): RpcSessionContext {
    return call.attributes.computeIfAbsent(RpcSessionContextKey) {
        RpcSessionContext()
    }
}

fun Application.module() {
    // Инициализация базы данных и запуск Flyway
    DatabaseFactory.init()

    // Инициализация Koin
    koinPlugin()

    val userRepository by inject<UserRepository>()
    val systemSettingsRepository by inject<SystemSettingsRepository>()
    val pluginManager by inject<PluginManager>()
    val syncWorker by inject<SyncWorker>()
    val keywordsWorker by inject<MediaKeywordsEnrichmentWorker>()
    val streamTokenService by inject<StreamTokenService>()
    val safeProxyHttpClient by inject<HttpClient>(named("safeProxyHttpClient"))

    val adminUsername = EnvHelper.getEnv("ADMIN_USERNAME") ?: "admin"
    val adminPassword = EnvHelper.getEnv("ADMIN_PASSWORD") ?: "admin"
    if (adminUsername.isNotBlank() && adminPassword.isNotBlank()) {
        runBlocking {
            try {
                userRepository.createAdminIfNotExists(adminUsername, adminPassword)
            } catch (e: Exception) {
                log.error("Не удалось инициализировать администратора: ${e.message}", e)
            }
        }
    }

    // Настройка CORS
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Range)
        allowHeader(HttpHeaders.IfRange)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Head)
        exposeHeader(HttpHeaders.ContentRange)
        exposeHeader(HttpHeaders.AcceptRanges)
        exposeHeader(HttpHeaders.ContentLength)
        maxAgeInSeconds = 86400 // Кеш preflight на 24ч, чтобы не слать OPTIONS на каждый чанк
    }

    // Поддержка Range Requests (HTTP 206 Partial Content)
    install(PartialContent)

    install(WebSockets) {
        pingPeriodMillis = 60_000L
        timeoutMillis = 300_000L
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(Krpc)

    routing {
        imageProxyRoutes(safeProxyHttpClient)
        streamProxyRoutes(streamTokenService, safeProxyHttpClient)

        get("/") {
            call.respondText("Avalon Media Card Server API (RPC is running)")
        }

        rpc("/api/rpc") {
            val pm = application.getKoin().get<PluginManager>()
            rpcConfig {
                serialization {
                    json(Json {
                        serializersModule = pm.serializersModule
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    })
                }
            }
            val sessionContext = getOrCreateSessionContext()

            registerService<AuthRpcService> {
                call.application.getKoin().get<AuthRpcServiceImpl> { parametersOf(sessionContext) }
            }
            registerService<SduiRpcService> {
                call.application.getKoin().get<SduiRpcServiceImpl> { parametersOf(sessionContext) }
            }
            registerService<ActionRpcService> {
                call.application.getKoin().get<ActionRpcServiceImpl> { parametersOf(sessionContext) }
            }
            registerService<AdminRpcService> {
                call.application.getKoin().get<AdminRpcServiceImpl> { parametersOf(sessionContext) }
            }
            registerService<TelemetryRpcService> {
                call.application.getKoin().get<TelemetryRpcServiceImpl> { parametersOf(sessionContext) }
            }
            registerService<PlaybackRpcService> {
                call.application.getKoin().get<PlaybackRpcServiceImpl> { parametersOf(sessionContext) }
            }
            registerService<UserSettingsRpcService> {
                call.application.getKoin().get<UserSettingsRpcServiceImpl> { parametersOf(sessionContext) }
            }
        }
    }

    // Запуск фоновой синхронизации
    syncWorker.start()
    keywordsWorker.start()
    monitor.subscribe(ApplicationStopped) {
        syncWorker.stop()
        keywordsWorker.stop()
        pluginManager.destroyAll()
        DatabaseFactory.close()
    }
}