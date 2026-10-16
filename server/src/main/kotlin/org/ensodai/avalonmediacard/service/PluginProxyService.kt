package org.ensodai.avalonmediacard.service

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator as OkHttpAuthenticator
import okhttp3.Credentials
import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.contract.admin.PluginProxySettingDto
import org.ensodai.avalonmediacard.contract.admin.ProxyType
import org.ensodai.avalonmediacard.contract.admin.TestPluginProxyRequest
import org.ensodai.avalonmediacard.contract.admin.UpdatePluginProxyRequest
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.net.Authenticator as JavaAuthenticator
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Serializable
data class StoredPluginProxyConfig(
    val isEnabled: Boolean = false,
    val type: ProxyType = ProxyType.HTTP,
    val host: String = "",
    val port: Int = 8080,
    val username: String? = null,
    val password: String? = null
)

internal object PluginProxyAuthenticator : JavaAuthenticator() {
    private data class ProxyKey(val host: String, val port: Int)

    private val credentials = ConcurrentHashMap<String, PasswordAuthentication>()
    private val registeredProxies = ConcurrentHashMap.newKeySet<ProxyKey>()

    fun register(host: String, port: Int, username: String, password: String) {
        val cleanHost = sanitizeHost(host).lowercase()
        val auth = PasswordAuthentication(username, password.toCharArray())
        registeredProxies.add(ProxyKey(cleanHost, port))
        credentials["$cleanHost:$port"] = auth
        runCatching {
            val address = InetAddress.getByName(cleanHost)
            val ip = address.hostAddress
            if (!ip.isNullOrBlank() && ip != cleanHost) {
                credentials["$ip:$port"] = auth
            }
        }
    }

    fun unregister(host: String, port: Int) {
        val cleanHost = sanitizeHost(host).lowercase()
        registeredProxies.remove(ProxyKey(cleanHost, port))
        credentials.remove("$cleanHost:$port")
        runCatching {
            val address = InetAddress.getByName(cleanHost)
            val ip = address.hostAddress
            if (!ip.isNullOrBlank()) {
                credentials.remove("$ip:$port")
            }
        }
    }

    override fun getPasswordAuthentication(): PasswordAuthentication? {
        val host = requestingHost?.lowercase()?.trim()
        val port = requestingPort
        val siteAddress = requestingSite?.hostAddress
        val siteName = requestingSite?.hostName?.lowercase()?.trim()

        val candidates = listOfNotNull(
            host?.let { "$it:$port" },
            siteAddress?.let { "$it:$port" },
            siteName?.let { "$it:$port" }
        )

        for (candidate in candidates) {
            credentials[candidate]?.let { return it }
        }

        val isSocks = requestingProtocol?.equals("SOCKS5", ignoreCase = true) == true ||
            requestingPrompt?.contains("SOCKS", ignoreCase = true) == true
        if (isSocks) {
            val byPort = credentials.entries.filter { it.key.endsWith(":$port") }
            if (byPort.size == 1) {
                return byPort.first().value
            }
            if (registeredProxies.size == 1) {
                return credentials.values.firstOrNull()
            }
        }

        return null
    }

    fun sanitizeHost(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("socks5://")
            .removePrefix("socks5h://")
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        return if (!trimmed.startsWith("[") && trimmed.count { it == ':' } == 1 && trimmed.substringAfter(':').all { it.isDigit() }) {
            trimmed.substringBefore(':')
        } else {
            trimmed
        }
    }
}

@Single
class PluginProxyService(
    private val systemSettingsRepository: SystemSettingsRepository,
    private val sharedHttpClient: HttpClient
) {
    init {
        JavaAuthenticator.setDefault(PluginProxyAuthenticator)
    }

    private val logger = LoggerFactory.getLogger(PluginProxyService::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val pluginClients = ConcurrentHashMap<String, HttpClient>()

    private fun settingKey(pluginId: String): String = "plugin:proxy:$pluginId"

    suspend fun getProxySetting(
        pluginId: String,
        pluginName: String,
        pluginVersion: String
    ): PluginProxySettingDto {
        val storedJson = systemSettingsRepository.getSetting(settingKey(pluginId))
        val config = if (!storedJson.isNullOrBlank()) {
            runCatching { json.decodeFromString<StoredPluginProxyConfig>(storedJson) }
                .getOrElse { StoredPluginProxyConfig() }
        } else {
            StoredPluginProxyConfig()
        }

        return PluginProxySettingDto(
            pluginId = pluginId,
            pluginName = pluginName,
            pluginVersion = pluginVersion,
            isEnabled = config.isEnabled,
            type = config.type,
            host = config.host,
            port = config.port,
            username = config.username,
            password = config.password
        )
    }

    suspend fun saveProxySetting(request: UpdatePluginProxyRequest) {
        val cleanHost = PluginProxyAuthenticator.sanitizeHost(request.host)
        val username = request.username?.trim()?.takeIf { it.isNotBlank() }
        val password = request.password?.takeIf { it.isNotBlank() }

        val config = StoredPluginProxyConfig(
            isEnabled = request.isEnabled,
            type = request.type,
            host = cleanHost,
            port = request.port,
            username = username,
            password = password
        )
        val serialized = json.encodeToString(config)
        systemSettingsRepository.saveSetting(settingKey(request.pluginId), serialized)
        invalidateClient(request.pluginId)

        if (config.isEnabled && config.type == ProxyType.SOCKS5 && username != null && password != null) {
            PluginProxyAuthenticator.register(cleanHost, config.port, username, password)
        } else {
            PluginProxyAuthenticator.unregister(cleanHost, config.port)
        }

        logger.info("Saved proxy setting for plugin {}: enabled={}, type={}, host={}:{}",
            request.pluginId, config.isEnabled, config.type, config.host, config.port)
    }

    suspend fun getHttpClientForPlugin(pluginId: String): HttpClient {
        val storedJson = systemSettingsRepository.getSetting(settingKey(pluginId)) ?: return sharedHttpClient
        val config = runCatching { json.decodeFromString<StoredPluginProxyConfig>(storedJson) }.getOrNull()
            ?: return sharedHttpClient

        if (!config.isEnabled || config.host.isBlank() || config.port <= 0) {
            return sharedHttpClient
        }

        return pluginClients.computeIfAbsent(pluginId) {
            createConfiguredHttpClient(config)
        }
    }

    fun invalidateClient(pluginId: String) {
        pluginClients.remove(pluginId)?.let { client ->
            runCatching { client.close() }
        }
    }

    private fun createConfiguredHttpClient(config: StoredPluginProxyConfig): HttpClient {
        val cleanHost = PluginProxyAuthenticator.sanitizeHost(config.host)
        val javaProxyType = if (config.type == ProxyType.SOCKS5) Proxy.Type.SOCKS else Proxy.Type.HTTP
        val javaProxy = Proxy(javaProxyType, InetSocketAddress(cleanHost, config.port))

        val username = config.username?.trim()?.takeIf { it.isNotBlank() }
        val password = config.password?.takeIf { it.isNotBlank() }
        val hasAuth = username != null && password != null

        if (hasAuth && config.type == ProxyType.SOCKS5) {
            PluginProxyAuthenticator.register(cleanHost, config.port, username, password)
        }

        return HttpClient(OkHttp) {
            engine {
                proxy = javaProxy
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(60, TimeUnit.SECONDS)
                    if (hasAuth && config.type == ProxyType.HTTP) {
                        proxyAuthenticator(OkHttpAuthenticator { _, response ->
                            val credential = Credentials.basic(username, password)
                            response.request.newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build()
                        })
                    }
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000L
                connectTimeoutMillis = 15_000L
                socketTimeoutMillis = 60_000L
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }
    }

    suspend fun testProxy(request: TestPluginProxyRequest): AdminActionResponse {
        val cleanHost = PluginProxyAuthenticator.sanitizeHost(request.host)
        if (cleanHost.isBlank()) {
            return AdminActionResponse(success = false, error = "Хост прокси не может быть пустым")
        }
        if (request.port !in 1..65535) {
            return AdminActionResponse(success = false, error = "Недопустимый порт: ${request.port} (ожидается 1..65535)")
        }

        val username = request.username?.trim()?.takeIf { it.isNotBlank() }
        val password = request.password?.takeIf { it.isNotBlank() }

        val tempConfig = StoredPluginProxyConfig(
            isEnabled = true,
            type = request.type,
            host = cleanHost,
            port = request.port,
            username = username,
            password = password
        )

        val hasSocksAuth = request.type == ProxyType.SOCKS5 && username != null && password != null
        if (hasSocksAuth) {
            PluginProxyAuthenticator.register(cleanHost, request.port, username, password)
        }

        var testClient: HttpClient? = null
        return try {
            testClient = createConfiguredHttpClient(tempConfig)
            val startTime = System.currentTimeMillis()

            val response = runCatching {
                testClient.get("https://api.ipify.org?format=json") {
                    timeout {
                        requestTimeoutMillis = 15_000L
                        connectTimeoutMillis = 12_000L
                        socketTimeoutMillis = 15_000L
                    }
                }
            }.recoverCatching {
                testClient.get("https://icanhazip.com") {
                    timeout {
                        requestTimeoutMillis = 15_000L
                        connectTimeoutMillis = 12_000L
                        socketTimeoutMillis = 15_000L
                    }
                }
            }.getOrThrow()

            val elapsed = System.currentTimeMillis() - startTime
            if (response.status.isSuccess()) {
                val body = response.bodyAsText().trim()
                val externalIp = Regex("""\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b""").find(body)?.value ?: body
                AdminActionResponse(
                    success = true,
                    error = "Прокси работает! Внешний IP: $externalIp (пинг: ${elapsed}мс)"
                )
            } else {
                AdminActionResponse(
                    success = false,
                    error = "Прокси ответил ошибкой HTTP ${response.status.value}: ${response.status.description}"
                )
            }
        } catch (e: Exception) {
            val rootMsg = e.cause?.message ?: e.message ?: "Неизвестная ошибка"
            logger.warn("Proxy connection test failed for {}:{}: {}", cleanHost, request.port, rootMsg)
            AdminActionResponse(
                success = false,
                error = "Ошибка подключения через прокси: $rootMsg"
            )
        } finally {
            runCatching { testClient?.close() }
        }
    }
}
