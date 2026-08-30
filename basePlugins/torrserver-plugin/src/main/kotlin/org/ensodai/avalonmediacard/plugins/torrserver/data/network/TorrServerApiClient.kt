package org.ensodai.avalonmediacard.plugins.torrserver.data.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerAction
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerFile
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerFilesResponse
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerGstProbeInfo
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerResponse
import kotlin.time.Duration.Companion.milliseconds

class TorrServerApiClient(
    private val context: org.ensodai.avalonmediacard.contract.plugins.PluginContext,
    private val httpClient: HttpClient,
    private val logger: PluginLogger
) {
    private val json = Json { ignoreUnknownKeys = true }
    private suspend fun getTorrserverUrl(userId: kotlin.uuid.Uuid?): String {
        val resolved = context.integrationManager.getTorrServerHost(userId)
        if (resolved != null && resolved.value.isNotBlank()) {
            return resolved.value
        }
        val host = if (userId != null) {
            context.userSettings.getString(userId, "torrserver_host")
        } else {
            context.settings.getString("torrserver_host")
        }
        return host?.takeIf { it.isNotBlank() }
            ?: System.getenv("TORRSERVER_HOST")?.takeIf { it.isNotBlank() }
            ?: "http://127.0.0.1:8090"
    }

    private suspend fun getAuthHeader(userId: kotlin.uuid.Uuid?): String? {
        val auth = context.integrationManager.getTorrServerAuth(userId)
        if (!auth.isNullOrBlank()) {
            return auth
        }
        val login = if (userId != null) context.userSettings.getString(
            userId,
            "torrserver_login"
        ) else context.settings.getString("torrserver_login")
        val pass = if (userId != null) context.userSettings.getString(
            userId,
            "torrserver_password"
        ) else context.settings.getString("torrserver_password")
        if (!login.isNullOrBlank() && !pass.isNullOrBlank()) {
            return "Basic " + java.util.Base64.getEncoder().encodeToString("$login:$pass".toByteArray())
        }
        return null
    }

    suspend fun testConnection(host: String, login: String?, pass: String?): String {
        val url = if (host.startsWith("http://") || host.startsWith("https://")) host else "http://$host"
        val auth = if (!login.isNullOrBlank() && !pass.isNullOrBlank()) {
            "Basic " + java.util.Base64.getEncoder().encodeToString("$login:$pass".toByteArray())
        } else null
        return try {
            val response = httpClient.post("$url/settings") {
                contentType(ContentType.Application.Json)
                setBody(TorrServerAction(action = "get"))
                if (auth != null) header(HttpHeaders.Authorization, auth)
                timeout { requestTimeoutMillis = 5000 }
            }
            if (response.status == HttpStatusCode.OK) {
                "OK"
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                "AUTH_ERROR"
            } else {
                "ERROR"
            }
        } catch (e: Exception) {
            "ERROR"
        }
    }

    suspend fun testGstConnection(host: String, login: String?, pass: String?): String {
        val url = if (host.startsWith("http://") || host.startsWith("https://")) host else "http://$host"
        val auth = if (!login.isNullOrBlank() && !pass.isNullOrBlank()) {
            "Basic " + java.util.Base64.getEncoder().encodeToString("$login:$pass".toByteArray())
        } else null
        return try {
            val response = httpClient.get("$url/gst/settings") {
                if (auth != null) header(HttpHeaders.Authorization, auth)
                timeout { requestTimeoutMillis = 5000 }
            }
            if (response.status == HttpStatusCode.OK) {
                "OK"
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                "AUTH_ERROR"
            } else if (response.status == HttpStatusCode.NotFound) {
                "NOT_FOUND" // No GST support
            } else {
                "ERROR"
            }
        } catch (e: Exception) {
            "ERROR"
        }
    }

    suspend fun addTorrent(urlOrMagnet: String, fileBytes: ByteArray? = null, userId: kotlin.uuid.Uuid?): String? {
        return try {
            val torrserverUrl = getTorrserverUrl(userId)
            val auth = getAuthHeader(userId)
            val addResponse = if (fileBytes != null && fileBytes.isNotEmpty()) {
                logger.info("Отправляем .torrent файл (${fileBytes.size} байт) в TorrServer")
                httpClient.post("$torrserverUrl/torrent/upload") {
                    if (auth != null) header(HttpHeaders.Authorization, auth)
                    setBody(
                        MultiPartFormDataContent(
                        formData {
                            append("save", "true")
                            append("file", fileBytes, Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"torrent.torrent\"")
                                append(HttpHeaders.ContentType, "application/x-bittorrent")
                            })
                        }
                    ))
                }
            } else {
                logger.info("Отправляем ссылку/магнет в TorrServer: $urlOrMagnet")
                httpClient.post("$torrserverUrl/torrents") {
                    if (auth != null) header(HttpHeaders.Authorization, auth)
                    contentType(ContentType.Application.Json)
                    setBody(TorrServerAction(action = "add", link = urlOrMagnet, saveToDb = true))
                }
            }

            if (addResponse.status == HttpStatusCode.OK) {
                val bodyStr = addResponse.body<String>()
                if (bodyStr.trim() == "null" || bodyStr.isBlank()) {
                    logger.error("TorrServer вернул null вместо данных торрента", null)
                    return null
                }
                val torrInfo = json.decodeFromString<TorrServerResponse>(bodyStr)
                torrInfo.hash
            } else {
                logger.error("Не удалось добавить торрент: ${addResponse.status}", null)
                null
            }
        } catch (e: java.net.SocketTimeoutException) {
            logger.error("Таймаут TorrServer (слишком долгий поиск пиров)", e)
            null
        } catch (e: Exception) {
            logger.error("Ошибка при добавлении торрента в TorrServer", e)
            null
        }
    }

    suspend fun getFiles(hash: String, userId: kotlin.uuid.Uuid?): List<TorrServerFile>? {
        for (i in 1..20) {
            try {
                val torrserverUrl = getTorrserverUrl(userId)
                val auth = getAuthHeader(userId)
                val getResponse = httpClient.get("$torrserverUrl/stream/video.mp4?link=$hash&stat") {
                    if (auth != null) header(HttpHeaders.Authorization, auth)
                    accept(ContentType.Application.Json)
                    timeout {
                        requestTimeoutMillis = 60000
                    }
                }

                if (getResponse.status == HttpStatusCode.OK) {
                    val bodyString = getResponse.body<String>()
                    val filesInfo = try {
                        json.decodeFromString<TorrServerFilesResponse>(bodyString)
                    } catch (e: Exception) {
                        null
                    }

                    val files = filesInfo?.fileStats ?: emptyList()
                    if (files.isNotEmpty()) {
                        return files
                    }
                }
            } catch (e: Exception) {
                logger.warn("Таймаут или ошибка при получении файлов торрента (попытка $i): ${e.message}")
            }
            delay(1000.milliseconds)
        }

        logger.error("Не удалось получить файлы торрента после 20 попыток (hash: $hash)", null)
        return null
    }

    suspend fun dropTorrent(hash: String, userId: kotlin.uuid.Uuid?) {
        try {
            val torrserverUrl = getTorrserverUrl(userId)
            val auth = getAuthHeader(userId)
            httpClient.post("$torrserverUrl/torrents") {
                if (auth != null) header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody(TorrServerAction(action = "rem", hash = hash, link = null))
            }
        } catch (e: Exception) {
            logger.error("Ошибка при удалении торрента", e)
        }
    }

    suspend fun buildStreamUrl(
        hash: String,
        fileIndex: Int?,
        filePath: String,
        userId: kotlin.uuid.Uuid?,
        useGst: Boolean = false
    ): String {
        val externalProxyUrl = context.settings.getString("server_url")?.takeIf { it.isNotBlank() } ?: ""
        val host = getTorrserverUrl(userId)
        val auth = getAuthHeader(userId)
        
        val ext = filePath.substringAfterLast('.', "mp4")
        val format = if (useGst) "m3u8" else ext
        val indexParam = if (fileIndex != null) {
            if (useGst) "?index=$fileIndex" else "&index=$fileIndex"
        } else ""

        val rawUrl = if (useGst) {
            "$host/gst/$hash/master.m3u8$indexParam"
        } else {
            "$host/stream/video.$ext?link=$hash$indexParam&play"
        }

        val encodedUrl = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rawUrl.toByteArray())

        return buildString {
            append(externalProxyUrl)
            append("/api/stream-proxy/video.$format?url=")
            append(encodedUrl)
            if (auth != null) {
                append("&auth=")
                append(java.net.URLEncoder.encode(auth, "UTF-8"))
            }
            append("&format=$format")
        }
    }

    suspend fun getGstProbe(hash: String, fileIndex: Int?, userId: kotlin.uuid.Uuid?): TorrServerGstProbeInfo? {
        try {
            val torrserverUrl = getTorrserverUrl(userId)
            val auth = getAuthHeader(userId)
            val indexStr = if (fileIndex != null) "?index=$fileIndex" else ""
            
            val response = httpClient.get("$torrserverUrl/gst/$hash/probe$indexStr") {
                if (auth != null) header(HttpHeaders.Authorization, auth)
                accept(ContentType.Application.Json)
                timeout { requestTimeoutMillis = 15000 }
            }
            
            if (response.status == HttpStatusCode.OK) {
                val bodyStr = response.body<String>()
                return try {
                    json.decodeFromString<TorrServerGstProbeInfo>(bodyStr)
                } catch (e: Exception) {
                    logger.error("Ошибка парсинга ответа /gst/probe", e)
                    null
                }
            }
        } catch (e: Exception) {
            logger.warn("Не удалось получить информацию о треках (probe) от TorrServer: ${e.message}")
        }
        return null
    }
}
