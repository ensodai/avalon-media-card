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
                ?: return@handle call.respond(HttpStatusCode.Forbidden, "Invalid or expired playback token")

            val requestedPath = call.parameters.getAll("filename")?.joinToString("/")?.takeIf { it.isNotBlank() }

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
        requestedSubPath != "segment.ts"
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
                    lower != "if-range"
                ) {
                    values.forEach { header(key, it) }
                }
            }

            clientRange?.let { header(HttpHeaders.Range, it) }
            clientIfRange?.let { header(HttpHeaders.IfRange, it) }

            customHeaders.forEach { (key, value) ->
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
                        headers = customHeaders,
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
                call.response.headers.append("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges")
                proxyResponse.contentType()?.let { call.response.headers.append(HttpHeaders.ContentType, it.toString()) }
                call.respond(proxyResponse.status)
                return@execute
            }

            val contentTypeStr = proxyResponse.contentType()?.toString() ?: ""
            val isM3u8 = contentTypeStr.contains("mpegurl", ignoreCase = true) ||
                (targetUrl.substringBefore("?").endsWith(".m3u8", ignoreCase = true) && !contentTypeStr.startsWith("video/"))
            val isDash = contentTypeStr.contains("dash+xml", ignoreCase = true) ||
                contentTypeStr.contains("vnd.mpeg.dash", ignoreCase = true) ||
                (targetUrl.substringBefore("?").endsWith(".mpd", ignoreCase = true) && !contentTypeStr.startsWith("video/"))

            if (isM3u8) {
                val m3u8Text = proxyResponse.bodyAsText()
                val rewrittenM3u8 = rewriteM3u8Playlist(
                    content = m3u8Text,
                    baseUrlStr = targetUrl,
                    userId = userId,
                    flags = flags,
                    customHeaders = customHeaders,
                    authHeader = authHeader,
                    tokenService = tokenService
                )

                call.response.headers.append("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges")
                call.respondText(rewrittenM3u8, ContentType.parse("application/vnd.apple.mpegurl"), proxyResponse.status)
            } else if (isDash) {
                val mpdText = proxyResponse.bodyAsText()
                val rewrittenMpd = rewriteMpdManifest(
                    content = mpdText,
                    baseUrlStr = targetUrl,
                    userId = userId,
                    flags = flags,
                    customHeaders = customHeaders,
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
                call.response.headers.append("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges")

                val length = proxyResponse.contentLength()
                call.respondBytesWriter(
                    status = proxyResponse.status,
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
    var expectUriOnNextLine = false

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
                    val newUri = "/api/stream-proxy/$newToken/meta"
                    sb.append(trimmed.substring(0, start))
                        .append(newUri)
                        .append(trimmed.substring(end))
                        .append('\n')
                    continue
                }
            }
        }

        // 2. Позиционные теги, за которыми следует URI на следующей строке (#EXT-X-STREAM-INF, #EXTINF)
        if (trimmed.startsWith("#EXT-X-STREAM-INF") || trimmed.startsWith("#EXTINF")) {
            expectUriOnNextLine = true
            sb.append(trimmed).append('\n')
            continue
        }

        // 3. Сам URI, следующий после тега
        if (!trimmed.startsWith("#") && expectUriOnNextLine) {
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
            val ext = if (resolved.substringBefore("?").endsWith(".m3u8", ignoreCase = true)) "playlist.m3u8" else "segment.ts"
            sb.append("/api/stream-proxy/").append(newToken).append('/').append(ext).append('\n')
            expectUriOnNextLine = false
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
