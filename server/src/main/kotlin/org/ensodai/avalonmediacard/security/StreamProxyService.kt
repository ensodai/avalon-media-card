package org.ensodai.avalonmediacard.security

import io.ktor.http.HttpHeaders
import io.ktor.http.parseQueryString
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.util.Base64
import kotlin.uuid.Uuid

/**
 * Централизованный сервис нормализации и защиты потоков медиакарты.
 * Гарантирует, что клиенты получают исключительно безопасные прокси-ссылки
 * для основного потока, вариантов качества (VideoQuality) и субтитров (SubtitleTrack).
 */
@Single
class StreamProxyService(
    private val streamTokenService: StreamTokenService
) {
    private val logger = LoggerFactory.getLogger(StreamProxyService::class.java)

    /**
     * Преобразует все медиа-ссылки в объекте [MediaStream] (основной URL, варианты качества, субтитры)
     * в защищенные токенизированные прокси-маршруты (/api/stream-proxy/{token}/{filename}).
     */
    fun sanitizeStream(
        stream: MediaStream,
        userId: Uuid?,
        defaultHeaders: Map<String, String> = emptyMap()
    ): MediaStream {
        val effectiveHeaders = if (stream.headers.isNotEmpty()) stream.headers else defaultHeaders

        val safeUrl = sanitizeUrl(
            rawUrl = stream.url,
            userId = userId,
            streamType = stream.type,
            streamHeaders = effectiveHeaders
        )

        val safeVariants = stream.qualityVariants.map { variant ->
            variant.copy(
                url = sanitizeUrl(
                    rawUrl = variant.url,
                    userId = userId,
                    streamType = stream.type,
                    streamHeaders = effectiveHeaders
                )
            )
        }

        val safeSubtitles = stream.subtitleTracks.map { sub ->
            val subUrl = sub.url
            if (subUrl.isNullOrBlank()) {
                sub
            } else {
                sub.copy(
                    url = sanitizeUrl(
                        rawUrl = subUrl,
                        userId = userId,
                        streamType = StreamType.DirectUrl,
                        streamHeaders = effectiveHeaders,
                        filename = "subtitles.vtt"
                    )
                )
            }
        }

        return stream.copy(
            url = safeUrl,
            qualityVariants = safeVariants,
            subtitleTracks = safeSubtitles
        )
    }

    /**
     * Пакетно нормализует список медиа-потоков (плейлист серий или вариантов).
     */
    fun sanitizePlaylist(
        streams: List<MediaStream>,
        userId: Uuid?,
        defaultHeaders: Map<String, String> = emptyMap()
    ): List<MediaStream> {
        return streams.map { sanitizeStream(it, userId, defaultHeaders) }
    }

    /**
     * Нормализует отдельный URL потока или ресурса.
     */
    fun sanitizeUrl(
        rawUrl: String,
        userId: Uuid?,
        streamType: StreamType? = null,
        streamHeaders: Map<String, String> = emptyMap(),
        filename: String? = null
    ): String {
        if (rawUrl.isBlank()) return rawUrl
        if (rawUrl.startsWith("/gst/")) return rawUrl
        if (rawUrl.startsWith("/api/stream-proxy/") && !rawUrl.contains("?url=")) return rawUrl

        // Обработка устаревшего query-формата /api/stream-proxy?url=base64... для обратной совместимости
        if (rawUrl.startsWith("/api/stream-proxy") && rawUrl.contains("?url=")) {
            try {
                val queryParams = parseQueryString(rawUrl.substringAfter("?"))
                val encodedUrl = queryParams["url"] ?: return rawUrl
                val decodedTarget = String(Base64.getUrlDecoder().decode(encodedUrl), Charsets.UTF_8)
                val customHeaders = streamHeaders.toMutableMap()

                queryParams["referer"]?.let {
                    val dec = runCatching { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8) }.getOrDefault(it)
                    customHeaders[HttpHeaders.Referrer] = dec
                }
                queryParams["origin"]?.let {
                    val dec = runCatching { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8) }.getOrDefault(it)
                    customHeaders["Origin"] = dec
                }
                queryParams["userAgent"]?.let {
                    val dec = runCatching { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8) }.getOrDefault(it)
                    customHeaders[HttpHeaders.UserAgent] = dec
                }
                val authHeader = queryParams["auth"]?.let {
                    if (it.startsWith("Basic ")) it
                    else runCatching { String(Base64.getUrlDecoder().decode(it)) }.getOrDefault(it)
                }

                return streamTokenService.wrapUrl(
                    targetUrl = decodedTarget,
                    userId = userId,
                    filename = filename,
                    streamType = streamType,
                    headers = customHeaders,
                    authHeader = authHeader
                )
            } catch (e: Exception) {
                logger.warn("Не удалось преобразовать старый URL стрим-прокси: {}", e.message)
            }
        }

        // Прямой внешний URL (http/https)
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return streamTokenService.wrapUrl(
                targetUrl = rawUrl,
                userId = userId,
                filename = filename,
                streamType = streamType,
                headers = streamHeaders
            )
        }

        return rawUrl
    }
}
