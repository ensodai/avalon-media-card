package org.ensodai.avalonmediacard.routes

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
import org.ensodai.avalonmediacard.security.StreamTokenService
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import kotlin.uuid.Uuid

private val logger = LoggerFactory.getLogger("StreamProxyRoute")

fun Route.streamProxyRoutes(
    tokenService: StreamTokenService,
    proxyHttpClient: HttpClient
) {
    // 1. Основной безопасный эндпоинт с AEAD Playback Tickets
    route("/api/stream-proxy/{token}/{filename...}") {
        handle {
            val token = call.parameters["token"] ?: return@handle call.respond(HttpStatusCode.BadRequest, "Missing token")
            val payload = tokenService.decryptAndValidate(token)
            val requestedPath = call.parameters.getAll("filename")?.joinToString("/")?.takeIf { it.isNotBlank() }

            logger.debug("[STREAM_PROXY] Incoming request: method={}, path={}, targetUrl={}", call.request.httpMethod.value, requestedPath, payload?.targetUrl)

            if (payload == null) {
                logger.warn("[STREAM_PROXY] Token decryption failed or token expired for: {}", token.take(20))
                return@handle call.respond(HttpStatusCode.Forbidden, "Invalid or expired playback token")
            }

            handleProxyRequest(
                call = call,
                baseTargetUrl = payload.targetUrl,
                requestedSubPath = requestedPath,
                userId = payload.userId,
                flags = payload.flags,
                customHeaders = payload.headers,
                authHeader = payload.authHeader,
                tokenService = tokenService,
                proxyHttpClient = proxyHttpClient
            )
        }
    }
}

private suspend fun handleProxyRequest(
    call: ApplicationCall,
    baseTargetUrl: String,
    requestedSubPath: String?,
    userId: Uuid?,
    flags: Byte,
    customHeaders: Map<String, String>,
    authHeader: String?,
    tokenService: StreamTokenService,
    proxyHttpClient: HttpClient
) {
    val isHead = call.request.httpMethod == HttpMethod.Head
    val clientRange = call.request.header(HttpHeaders.Range)
    val clientIfRange = call.request.header(HttpHeaders.IfRange)

    // Резолвинг фактического URL для DASH/HLS субсегментов
    val targetUrl = if (requestedSubPath != null &&
        !baseTargetUrl.substringBefore("?").endsWith("/$requestedSubPath", ignoreCase = true) &&
        !baseTargetUrl.substringBefore("?").endsWith(requestedSubPath, ignoreCase = true) &&
        requestedSubPath != "playlist.m3u8" &&
        requestedSubPath != "manifest.mpd" &&
        requestedSubPath != "video.mp4" &&
        requestedSubPath != "meta" &&
        requestedSubPath != "segment.ts" &&
        requestedSubPath != "segment.m4s"
    ) {
        try {
            val baseUri = URI(baseTargetUrl)
            val resolvedUri = baseUri.resolve(requestedSubPath)
            if (resolvedUri.scheme != baseUri.scheme || 
                resolvedUri.host != baseUri.host || 
                resolvedUri.port != baseUri.port) {
                call.respond(HttpStatusCode.Forbidden, "Cross-origin subpath resolution forbidden")
                return
            }
            resolvedUri.toString()
        } catch (_: Exception) {
            baseTargetUrl
        }
    } else {
        baseTargetUrl
    }

    // Резолвинг эффективных заголовков для защиты от Hotlink Protection (Rutube, VK и др.)
    val targetUri = runCatching { URI(targetUrl) }.getOrNull()
    val targetHost = targetUri?.host?.lowercase() ?: ""
    val effectiveHeaders = customHeaders.toMutableMap()

    if (!effectiveHeaders.containsKey(HttpHeaders.Referrer) && !effectiveHeaders.containsKey("Referer")) {
        if (targetHost.contains("rutube.ru") || targetHost.contains("rtbcdn.ru")) {
            effectiveHeaders[HttpHeaders.Referrer] = "https://rutube.ru/"
        } else if (targetUri != null && !targetUri.scheme.isNullOrBlank() && !targetUri.host.isNullOrBlank()) {
            effectiveHeaders[HttpHeaders.Referrer] = "${targetUri.scheme}://${targetUri.host}/"
        }
    }

    if (!effectiveHeaders.containsKey("Origin")) {
        if (targetHost.contains("rutube.ru") || targetHost.contains("rtbcdn.ru")) {
            effectiveHeaders["Origin"] = "https://rutube.ru"
        }
    }

    if (!effectiveHeaders.containsKey(HttpHeaders.UserAgent) && !effectiveHeaders.containsKey("User-Agent")) {
        effectiveHeaders[HttpHeaders.UserAgent] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    try {
        val statement = proxyHttpClient.prepareRequest(targetUrl) {
            this.method = if (isHead) HttpMethod.Head else HttpMethod.Get

            call.request.headers.forEach { key, values ->
                val lower = key.lowercase()
                if (lower != "host" &&
                    lower != "authorization" &&
                    lower != "if-none-match" &&
                    lower != "if-modified-since" &&
                    lower != "connection" &&
                    lower != "range" &&
                    lower != "if-range" &&
                    lower != "referer" &&
                    lower != "origin" &&
                    lower != "accept-encoding" &&
                    !lower.startsWith("sec-")
                ) {
                    values.forEach { header(key, it) }
                }
            }

            clientRange?.let { header(HttpHeaders.Range, it) }
            clientIfRange?.let { header(HttpHeaders.IfRange, it) }

            effectiveHeaders.forEach { (key, value) ->
                header(key, value)
            }

            authHeader?.let { header(HttpHeaders.Authorization, it) }

            timeout {
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = 60_000L
                connectTimeoutMillis = 15_000L
            }
        }

        statement.execute { proxyResponse ->
            // Ручная обработка редиректов (SSRF защита работает на уровне SafeDnsResolver)
            if (proxyResponse.status.value in 300..308) {
                val location = proxyResponse.headers[HttpHeaders.Location]
                if (location != null) {
                    val absoluteLocation = try {
                        URI(targetUrl).resolve(location).toString()
                    } catch (_: Exception) {
                        location
                    }
                    val ext = if (absoluteLocation.substringBefore("?").endsWith(".mpd", ignoreCase = true)) {
                        "manifest.mpd"
                    } else "playlist.m3u8"

                    val newToken = tokenService.generateToken(
                        targetUrl = absoluteLocation,
                        userId = userId,
                        flags = flags,
                        headers = effectiveHeaders,
                        authHeader = authHeader
                    )
                    call.respondRedirect("/api/stream-proxy/$newToken/$ext")
                    return@execute
                }
            }

            if (isHead) {
                proxyResponse.headers.forEach { name, values ->
                    val lowerName = name.lowercase()
                    if (lowerName != HttpHeaders.ContentType.lowercase() &&
                        lowerName != HttpHeaders.ContentLength.lowercase() &&
                        lowerName != HttpHeaders.TransferEncoding.lowercase() &&
                        lowerName != HttpHeaders.Connection.lowercase() &&
                        !lowerName.startsWith("access-control-")
                    ) {
                        values.forEach { call.response.headers.append(name, it) }
                    }
                }
                call.response.headers.append(HttpHeaders.AcceptRanges, "bytes")
                call.response.headers.append("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges, Content-Type, ETag, Last-Modified")
                proxyResponse.contentLength()?.let { call.response.headers.append(HttpHeaders.ContentLength, it.toString()) }
                proxyResponse.contentType()?.let { call.response.headers.append(HttpHeaders.ContentType, it.toString()) }
                call.respond(proxyResponse.status)
                return@execute
            }

            logger.debug("[STREAM_PROXY] Upstream response: status={}, contentType={}, targetUrl={}", proxyResponse.status, proxyResponse.contentType(), targetUrl)

            if (!proxyResponse.status.isSuccess() && proxyResponse.status != HttpStatusCode.PartialContent) {
                val errorBody = proxyResponse.bodyAsText()
                logger.warn("[STREAM_PROXY] Upstream error (status={}): {}", proxyResponse.status, errorBody.take(500))
                proxyResponse.headers.forEach { name, values ->
                    val lowerName = name.lowercase()
                    if (lowerName != HttpHeaders.TransferEncoding.lowercase() &&
                        lowerName != HttpHeaders.Connection.lowercase() &&
                        !lowerName.startsWith("access-control-")
                    ) {
                        values.forEach { call.response.headers.append(name, it) }
                    }
                }
                call.response.headers.append("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges")
                call.respond(proxyResponse.status, errorBody)
                return@execute
            }

            val contentTypeStr = proxyResponse.contentType()?.toString() ?: ""
            val isM3u8 = contentTypeStr.contains("mpegurl", ignoreCase = true) ||
                contentTypeStr.contains("x-mpegurl", ignoreCase = true) ||
                (requestedSubPath != null && requestedSubPath.endsWith(".m3u8", ignoreCase = true)) ||
                (targetUrl.substringBefore("?").endsWith(".m3u8", ignoreCase = true) && !contentTypeStr.startsWith("video/")) ||
                (targetUrl.contains("m3u8", ignoreCase = true) && !contentTypeStr.startsWith("video/"))
            val isDash = contentTypeStr.contains("dash+xml", ignoreCase = true) ||
                contentTypeStr.contains("vnd.mpeg.dash", ignoreCase = true) ||
                (requestedSubPath != null && requestedSubPath.endsWith(".mpd", ignoreCase = true)) ||
                (targetUrl.substringBefore("?").endsWith(".mpd", ignoreCase = true) && !contentTypeStr.startsWith("video/"))

            if (isM3u8) {
                val rawBytes = proxyResponse.bodyAsBytes()
                val isGzip = rawBytes.size >= 2 && rawBytes[0] == 0x1f.toByte() && rawBytes[1] == 0x8b.toByte()
                val decompressedBytes = if (isGzip) {
                    try {
                        java.util.zip.GZIPInputStream(rawBytes.inputStream()).use { it.readBytes() }
                    } catch (_: Exception) {
                        rawBytes
                    }
                } else {
                    rawBytes
                }
                val m3u8Text = String(decompressedBytes, Charsets.UTF_8)

                logger.debug("[STREAM_PROXY] Raw M3U8 (gzip={}, length={}, preview={})", isGzip, m3u8Text.length, m3u8Text.take(250).replace("\n", "\\n"))
                val rewrittenM3u8 = rewriteM3u8Playlist(
                    content = m3u8Text,
                    baseUrlStr = targetUrl,
                    userId = userId,
                    flags = flags,
                    customHeaders = effectiveHeaders,
                    authHeader = authHeader,
                    tokenService = tokenService
                )
                logger.debug("[STREAM_PROXY] Rewritten M3U8 (length={}, preview={})", rewrittenM3u8.length, rewrittenM3u8.take(250).replace("\n", "\\n"))

                call.response.headers.append("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges")
                call.respondText(rewrittenM3u8, ContentType.parse("application/vnd.apple.mpegurl"), proxyResponse.status)
            } else if (isDash) {
                val rawBytes = proxyResponse.bodyAsBytes()
                val isGzip = rawBytes.size >= 2 && rawBytes[0] == 0x1f.toByte() && rawBytes[1] == 0x8b.toByte()
                val decompressedBytes = if (isGzip) {
                    try {
                        java.util.zip.GZIPInputStream(rawBytes.inputStream()).use { it.readBytes() }
                    } catch (_: Exception) {
                        rawBytes
                    }
                } else {
                    rawBytes
                }
                val mpdText = String(decompressedBytes, Charsets.UTF_8)

                val rewrittenMpd = rewriteMpdManifest(
                    content = mpdText,
                    baseUrlStr = targetUrl,
                    userId = userId,
                    flags = flags,
                    customHeaders = effectiveHeaders,
                    authHeader = authHeader,
                    tokenService = tokenService
                )

                call.response.headers.append("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges")
                call.respondText(rewrittenMpd, ContentType.parse("application/dash+xml"), proxyResponse.status)
            } else {
                proxyResponse.headers.forEach { name, values ->
                    val lowerName = name.lowercase()
                    if (lowerName != HttpHeaders.ContentType.lowercase() &&
                        lowerName != HttpHeaders.ContentLength.lowercase() &&
                        lowerName != HttpHeaders.TransferEncoding.lowercase() &&
                        lowerName != HttpHeaders.Connection.lowercase() &&
                        !lowerName.startsWith("access-control-")
                    ) {
                        values.forEach { call.response.headers.append(name, it) }
                    }
                }
                call.response.headers.append(HttpHeaders.AcceptRanges, "bytes")
                call.response.headers.append("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges, Content-Type, ETag, Last-Modified")

                val length = proxyResponse.contentLength()
                val effectiveStatus = if (clientRange != null && proxyResponse.status == HttpStatusCode.OK && length != null && length > 0) {
                    call.response.headers.append(HttpHeaders.ContentRange, "bytes 0-${length - 1}/$length")
                    HttpStatusCode.PartialContent
                } else {
                    proxyResponse.status
                }

                call.respondBytesWriter(
                    status = effectiveStatus,
                    contentType = proxyResponse.contentType(),
                    contentLength = length
                ) {
                    try {
                        proxyResponse.bodyAsChannel().copyTo(this)
                    } catch (e: Exception) {
                        val cause = e.cause
                        if (e is ChannelWriteException ||
                            e is CancellationException ||
                            e is IOException ||
                            (cause != null && cause::class.simpleName == "StacklessClosedChannelException")
                        ) {
                            // Плеер закрыл соединение при перемотке или выходе — нормальное поведение
                        } else {
                            logger.debug("Stream proxy channel disconnected: {}", e.message)
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        val cause = e.cause
        if (e is ChannelWriteException ||
            e is CancellationException ||
            e is IOException ||
            (cause != null && cause::class.simpleName == "StacklessClosedChannelException")
        ) {
            // Клиент отключился
        } else {
            logger.error("Stream proxy error for target {}: {}", targetUrl, e.message)
            try {
                call.respond(HttpStatusCode.BadGateway, "Stream proxy upstream error")
            } catch (_: Exception) {}
        }
    }
}

/**
 * Высокопроизводительный потоковый рерайтер M3U8 (HLS) с поддержкой скользящего TTL (Sliding TTL).
 */
fun rewriteM3u8Playlist(
    content: String,
    baseUrlStr: String,
    userId: Uuid?,
    flags: Byte,
    customHeaders: Map<String, String>,
    authHeader: String?,
    tokenService: StreamTokenService
): String {
    val baseUrl = try { URI(baseUrlStr) } catch (_: Exception) { null }
    val sb = StringBuilder(content.length + 2048)
    val lines = content.lineSequence()
    var isStreamInfPlaylist = false
    var isSegmentInf = false

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue

        // 1. Атрибуты URI внутри композитных тегов (#EXT-X-MEDIA, #EXT-X-KEY, #EXT-X-MAP)
        if (trimmed.startsWith("#EXT-X-MEDIA:") ||
            trimmed.startsWith("#EXT-X-KEY:") ||
            trimmed.startsWith("#EXT-X-MAP:")
        ) {
            val uriIndex = trimmed.indexOf("URI=\"")
            if (uriIndex != -1) {
                val start = uriIndex + 5
                val end = trimmed.indexOf("\"", start)
                if (end != -1) {
                    val rawUri = trimmed.substring(start, end)
                    val resolved = if (baseUrl != null) {
                        try { baseUrl.resolve(rawUri).toString() } catch (_: Exception) { rawUri }
                    } else rawUri

                    val newToken = tokenService.generateToken(
                        targetUrl = resolved,
                        userId = userId,
                        flags = flags,
                        headers = customHeaders,
                        authHeader = authHeader
                    )
                    val ext = if (trimmed.startsWith("#EXT-X-MEDIA:") && !resolved.contains(".vtt", ignoreCase = true) && !resolved.contains(".webvtt", ignoreCase = true)) {
                        "playlist.m3u8"
                    } else if (resolved.contains(".m3u8", ignoreCase = true)) {
                        "playlist.m3u8"
                    } else {
                        "meta"
                    }
                    val newUri = "/api/stream-proxy/$newToken/$ext"
                    sb.append(trimmed.substring(0, start))
                        .append(newUri)
                        .append(trimmed.substring(end))
                        .append('\n')
                    continue
                }
            }
        }

        // 2. Позиционные теги
        if (trimmed.startsWith("#EXT-X-STREAM-INF")) {
            isStreamInfPlaylist = true
            isSegmentInf = false
            sb.append(trimmed).append('\n')
            continue
        }
        if (trimmed.startsWith("#EXTINF")) {
            isSegmentInf = true
            isStreamInfPlaylist = false
            sb.append(trimmed).append('\n')
            continue
        }

        // 3. Сам URI, следующий после тега
        if (!trimmed.startsWith("#") && (isStreamInfPlaylist || isSegmentInf)) {
            val resolved = if (baseUrl != null) {
                try { baseUrl.resolve(trimmed).toString() } catch (_: Exception) { trimmed }
            } else trimmed

            val newToken = tokenService.generateToken(
                targetUrl = resolved,
                userId = userId,
                flags = flags,
                headers = customHeaders,
                authHeader = authHeader
            )
            val ext = if (isStreamInfPlaylist) {
                "playlist.m3u8"
            } else {
                if (resolved.substringBefore("?").endsWith(".m4s", ignoreCase = true) || resolved.contains(".m4s", ignoreCase = true)) {
                    "segment.m4s"
                } else if (resolved.substringBefore("?").endsWith(".m3u8", ignoreCase = true)) {
                    "playlist.m3u8"
                } else {
                    "segment.ts"
                }
            }
            sb.append("/api/stream-proxy/").append(newToken).append('/').append(ext).append('\n')
            isStreamInfPlaylist = false
            isSegmentInf = false
        } else {
            sb.append(trimmed).append('\n')
        }
    }

    return sb.toString()
}

/**
 * Рерайтер для манифестов MPEG-DASH (.mpd).
 * Перезаписывает теги <BaseURL> и <Location>, маршрутизируя чанки через защищенный прокси.
 */
fun rewriteMpdManifest(
    content: String,
    baseUrlStr: String,
    userId: Uuid?,
    flags: Byte,
    customHeaders: Map<String, String>,
    authHeader: String?,
    tokenService: StreamTokenService
): String {
    val baseUrl = try { URI(baseUrlStr) } catch (_: Exception) { null }

    var result = content

    // 1. Перезапись <BaseURL>...</BaseURL>
    if (result.contains("<BaseURL>")) {
        result = result.replace(Regex("<BaseURL>([^<]+)</BaseURL>")) { match ->
            val rawUri = match.groupValues[1].trim()
            val resolved = if (baseUrl != null) {
                try { baseUrl.resolve(rawUri).toString() } catch (_: Exception) { rawUri }
            } else rawUri

            val newToken = tokenService.generateToken(
                targetUrl = resolved,
                userId = userId,
                flags = flags,
                headers = customHeaders,
                authHeader = authHeader
            )
            "<BaseURL>/api/stream-proxy/$newToken/</BaseURL>"
        }
    } else if (baseUrl != null) {
        // Если в MPD нет <BaseURL>, вставляем <BaseURL> сразу после тега <MPD ...> для явной привязки сегментов к прокси
        val baseToken = tokenService.generateToken(
            targetUrl = baseUrlStr,
            userId = userId,
            flags = flags,
            headers = customHeaders,
            authHeader = authHeader
        )
        val proxyBaseTag = "\n  <BaseURL>/api/stream-proxy/$baseToken/</BaseURL>"
        val mpdTagIndex = result.indexOf("<MPD")
        if (mpdTagIndex != -1) {
            val closeIndex = result.indexOf(">", mpdTagIndex)
            if (closeIndex != -1) {
                result = result.substring(0, closeIndex + 1) + proxyBaseTag + result.substring(closeIndex + 1)
            }
        }
    }

    // 2. Перезапись <Location>...</Location>
    if (result.contains("<Location>")) {
        result = result.replace(Regex("<Location>([^<]+)</Location>")) { match ->
            val rawUri = match.groupValues[1].trim()
            val resolved = if (baseUrl != null) {
                try { baseUrl.resolve(rawUri).toString() } catch (_: Exception) { rawUri }
            } else rawUri

            val newToken = tokenService.generateToken(
                targetUrl = resolved,
                userId = userId,
                flags = flags,
                headers = customHeaders,
                authHeader = authHeader
            )
            "<Location>/api/stream-proxy/$newToken/manifest.mpd</Location>"
        }
    }

    return result
}
