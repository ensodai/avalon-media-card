package org.ensodai.avalonmediacard.service

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.ensodai.avalonmediacard.repository.UserIntegrationSettingsRepository
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.util.Base64
import kotlin.uuid.Uuid

@Single
class TorrServerKeepAlivePinger(
    @Named("safeProxyHttpClient") private val httpClient: HttpClient,
    private val systemSettingsRepository: SystemSettingsRepository,
    private val userIntegrationSettingsRepository: UserIntegrationSettingsRepository
) {
    private val logger = LoggerFactory.getLogger(TorrServerKeepAlivePinger::class.java)

    suspend fun getTorrServerHost(userId: Uuid?): String {
        if (userId != null) {
            val userUse = userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "use_torrserver")?.toBooleanStrictOrNull() ?: false
            if (userUse) {
                val userHost = userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "torrserver_host")
                if (!userHost.isNullOrBlank()) {
                    return userHost.trimEnd('/')
                }
            }
        }
        val allowSharedTorr = systemSettingsRepository.getSetting("torrserver_share_with_users")?.toBooleanStrictOrNull() ?: false
        if (allowSharedTorr) {
            val globalHost = systemSettingsRepository.getSetting("torrserver_host")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:torrserver_host")
            if (!globalHost.isNullOrBlank()) {
                return globalHost.trimEnd('/')
            }
        }
        val envHost = System.getenv("TORRSERVER_HOST")?.takeIf { it.isNotBlank() }
        if (envHost != null) {
            return envHost.trimEnd('/')
        }
        val localSetting = systemSettingsRepository.getSetting("torrserver_host")
            ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:torrserver_host")
        if (!localSetting.isNullOrBlank()) {
            return localSetting.trimEnd('/')
        }
        return "http://127.0.0.1:8090"
    }

    suspend fun getAuthHeader(userId: Uuid?): String? {
        val (login, pass) = if (userId != null && userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "use_torrserver")?.toBooleanStrictOrNull() == true) {
            val l = userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "torrserver_login")
            val p = userIntegrationSettingsRepository.getSetting(userId, "torrserver-plugin", "torrserver_password")
            l to p
        } else {
            val l = systemSettingsRepository.getSetting("torrserver_login")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:torrserver_login")
            val p = systemSettingsRepository.getSetting("torrserver_password")
                ?: systemSettingsRepository.getSetting("plugin:torrserver-plugin:torrserver_password")
            l to p
        }

        if (!login.isNullOrBlank() && !pass.isNullOrBlank()) {
            return "Basic " + Base64.getEncoder().encodeToString("$login:$pass".toByteArray())
        }
        return null
    }

    suspend fun ping(hash: String, userId: Uuid?): Boolean {
        if (hash.isBlank()) return false
        val cleanHash = if (hash.startsWith("magnet:", ignoreCase = true)) {
            val match = Regex("xt=urn:btih:([a-zA-Z0-9]+)", RegexOption.IGNORE_CASE).find(hash)
            match?.groupValues?.get(1) ?: hash
        } else {
            hash
        }

        return try {
            val host = getTorrServerHost(userId)
            val auth = getAuthHeader(userId)
            val url = "$host/stream/video.mp4?link=$cleanHash&stat"

            val response = httpClient.get(url) {
                if (auth != null) {
                    header(HttpHeaders.Authorization, auth)
                }
                timeout {
                    requestTimeoutMillis = 5000L
                    connectTimeoutMillis = 5000L
                    socketTimeoutMillis = 5000L
                }
            }
            if (response.status == HttpStatusCode.OK) {
                logger.debug("[TorrServerKeepAlive] Ping OK for hash={} on {}", cleanHash, host)
                true
            } else {
                logger.warn("[TorrServerKeepAlive] Ping non-OK ({}) for hash={} on {}", response.status, cleanHash, host)
                false
            }
        } catch (e: Exception) {
            logger.warn("[TorrServerKeepAlive] Ping failed for hash={}: {}", cleanHash, e.message)
            false
        }
    }
}
