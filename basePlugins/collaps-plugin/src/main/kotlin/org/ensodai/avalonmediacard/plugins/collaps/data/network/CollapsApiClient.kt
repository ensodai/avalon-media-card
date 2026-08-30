package org.ensodai.avalonmediacard.plugins.collaps.data.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.parsers.HlsPlaylistParser
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsCcModel
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsEmbedParseResult
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsHlsResolved
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsRootSearch
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsSearchResult
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsSeasonData
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference

class CollapsApiClient(
    private val httpClient: HttpClient,
    private val logger: PluginLogger
) {
    private val baseUrl = "https://api.bhcesh.me"
    private val apiToken = "eedefb541aeba871dcfc756e6b31c02e"

    private val dynamicCdnHostCache = AtomicReference<String>("showvid.ws")
    @Volatile
    private var lastDnsResolveTime = 0L

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun searchMedia(query: String): List<CollapsSearchResult> {
        return try {
            val response: HttpResponse = httpClient.get("$baseUrl/list") {
                parameter("token", apiToken)
                parameter("name", query)
                headers {
                    append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                }
            }
            if (response.status.isSuccess()) {
                val text = response.bodyAsText()
                val root = json.decodeFromString<CollapsRootSearch>(text)
                root.results
            } else {
                logger.warn("Collaps: Search API returned status ${response.status} for query '$query'")
                emptyList()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error("Collaps: Error searching media for query '$query'", e)
            emptyList()
        }
    }

    suspend fun searchMediaByImdb(imdbId: String): List<CollapsSearchResult> {
        val cleanImdbId = imdbId.trim().removePrefix("tt")
        return try {
            val response: HttpResponse = httpClient.get("$baseUrl/list") {
                parameter("token", apiToken)
                parameter("imdb_id", cleanImdbId)
                headers {
                    append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                }
            }
            if (response.status.isSuccess()) {
                val text = response.bodyAsText()
                val root = json.decodeFromString<CollapsRootSearch>(text)
                root.results
            } else {
                logger.warn("Collaps: Search by IMDB returned status ${response.status} for '$imdbId'")
                emptyList()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error("Collaps: Error searching media by IMDB id '$imdbId'", e)
            emptyList()
        }
    }

    private val embedCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, CollapsEmbedParseResult>>()

    suspend fun fetchEmbedPage(iframeUrl: String): CollapsEmbedParseResult? {
        val cached = embedCache[iframeUrl]
        val now = System.currentTimeMillis()
        if (cached != null && now - cached.first < 10 * 60 * 1000L) {
            return cached.second
        }
        return try {
            val response: HttpResponse = httpClient.get(iframeUrl) {
                headers {
                    append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    append("Origin", "https://kinokrad.my")
                    append("Referer", "https://kinokrad.my/")
                }
            }
            if (!response.status.isSuccess()) {
                logger.warn("Collaps: Embed GET returned status ${response.status} for URL $iframeUrl")
                return null
            }
            val html = response.bodyAsText()
            val result = parseEmbedHtml(html)
            if (result != null) {
                embedCache[iframeUrl] = Pair(now, result)
            }
            result
        } catch (e: Exception) {
            logger.error("Collaps: Error fetching embed page for $iframeUrl", e)
            null
        }
    }

    suspend fun resolveHlsPlaylist(rawHlsUrl: String): CollapsHlsResolved {
        val cleanUrl = fixCdnHost(rawHlsUrl)
        val headers = mapOf(
            HttpHeaders.UserAgent to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Origin" to "https://kinokrad.my",
            "Referer" to "https://kinokrad.my/"
        )
        val resolved = HlsPlaylistParser.resolveHlsPlaylist(httpClient, cleanUrl, headers)
        return CollapsHlsResolved(
            primaryUrl = resolved.primaryUrl,
            qualityVariants = resolved.qualityVariants
        )
    }

    companion object {
        private val EXPLICIT_DOWNLOAD_REGEX = Regex("""download:\s*"(https?://[^"]+)"""", RegexOption.IGNORE_CASE)
        private val SEASONS_REGEX_1 = Regex("""seasons:\s*(\[\s*\{[\s\S]*?\}\s*\])\s*,\s*(?:audio|cc|poster|preview|stats|download)""", RegexOption.IGNORE_CASE)
        private val SEASONS_REGEX_2 = Regex("""seasons:\s*(\[\s*\{[\s\S]*?\}\s*\])""", RegexOption.IGNORE_CASE)
        private val HLS_REGEX_1 = Regex("""hls:\s*"(https?://[^"]+\.m3u[^"]+)"""", RegexOption.IGNORE_CASE)
        private val HLS_REGEX_2 = Regex("""hls:\s*'([^']+)'""", RegexOption.IGNORE_CASE)
        private val AUDIO_REGEX = Regex("""audio:\s*\{\s*"names":\s*\[([^\]]+)\]""", RegexOption.IGNORE_CASE)
        private val QUOTED_NAME_REGEX = Regex(""""([^"]+)"""")
        private val CC_REGEX = Regex("""cc:\s*(\[\s*\{[\s\S]*?\}\s*\])""", RegexOption.IGNORE_CASE)
        private val DURATION_REGEX = Regex("""duration:\s*(\d+)""", RegexOption.IGNORE_CASE)
    }

    private fun parseEmbedHtml(html: String): CollapsEmbedParseResult? {
        val explicitDownloadMatch = EXPLICIT_DOWNLOAD_REGEX.find(html)
        val explicitDownload = explicitDownloadMatch?.groupValues?.get(1)
            ?.replace("\\u0026", "&")
            ?.replace("\\/", "/")

        // 1. Try parsing TV show seasons block
        val seasonsMatch = SEASONS_REGEX_1.find(html) ?: SEASONS_REGEX_2.find(html)

        if (seasonsMatch != null) {
            val jsonText = seasonsMatch.groupValues[1]
            try {
                val seasonsList = json.decodeFromString<List<CollapsSeasonData>>(jsonText)
                if (seasonsList.isNotEmpty()) {
                    val fixedSeasons = seasonsList.map { season ->
                        season.copy(
                            episodes = season.episodes.map { ep ->
                                val fixedHls = ep.hls?.let { fixCdnHost(it) }
                                val epDl = fixedHls?.let { buildDownloadStreamUrl(it) }
                                ep.copy(
                                    hls = fixedHls,
                                    downloadUrl = epDl,
                                    cc = ep.cc?.map { cc -> cc.copy(url = cc.url?.let { u -> fixCdnHost(u) }) }
                                )
                            }
                        )
                    }
                    return CollapsEmbedParseResult(
                        seasons = fixedSeasons,
                        downloadUrl = explicitDownload
                    )
                }
            } catch (e: Exception) {
                // Not a TV series embed or no valid seasons, ignore and fall through to movie block
            }
        }

        // 2. Movie single player block
        val hlsMatch = HLS_REGEX_1.find(html) ?: HLS_REGEX_2.find(html)

        val rawHls = hlsMatch?.groupValues?.get(1)?.replace("\\/", "/")
        val hlsUrl = rawHls?.let { fixCdnHost(it) }
        val finalDownloadUrl = explicitDownload ?: hlsUrl?.let { buildDownloadStreamUrl(it) }

        val audioNames = mutableListOf<String>()
        val audioMatch = AUDIO_REGEX.find(html)
        if (audioMatch != null) {
            val rawNames = audioMatch.groupValues[1]
            val items = QUOTED_NAME_REGEX.findAll(rawNames).map { it.groupValues[1] }.toList()
            audioNames.addAll(items.filter { it.isNotBlank() && it != "delete" })
        }

        val subtitles = mutableListOf<CollapsCcModel>()
        val ccMatch = CC_REGEX.find(html)
        if (ccMatch != null) {
            try {
                val ccList = json.decodeFromString<List<CollapsCcModel>>(ccMatch.groupValues[1])
                subtitles.addAll(ccList.map { it.copy(url = it.url?.let { u -> fixCdnHost(u) }) })
            } catch (e: Exception) {
                // Ignore subtitle parsing errors
            }
        }

        val durationMatch = DURATION_REGEX.find(html)
        val durationSeconds = durationMatch?.groupValues?.get(1)?.toDoubleOrNull()

        if (finalDownloadUrl.isNullOrBlank() && hlsUrl.isNullOrBlank()) return null

        return CollapsEmbedParseResult(
            hlsUrl = hlsUrl,
            downloadUrl = finalDownloadUrl,
            durationSeconds = durationSeconds,
            audioNames = audioNames,
            subtitles = subtitles
        )
    }

    fun buildDownloadStreamUrl(hlsUrl: String): String {
        val clean = fixCdnHost(hlsUrl)
        val encoded = java.net.URLEncoder.encode(clean, "UTF-8")
        return "https://dl.showvid.ws/x-px?m=$encoded&x-cdn=10551403"
    }

    private fun getActiveCdnHost(): String {
        val now = System.currentTimeMillis()
        if (now - lastDnsResolveTime > 600_000L) {
            lastDnsResolveTime = now
            try {
                val canonical = InetAddress.getByName("interkh.com").canonicalHostName
                if (!canonical.isNullOrBlank() && !canonical.equals("interkh.com", ignoreCase = true)) {
                    val resolvedHost = canonical.trim().trimStart('.')
                    if (resolvedHost.isNotBlank()) {
                        logger.info("Collaps: Dynamically resolved interkh.com DNAME -> $resolvedHost via System DNS")
                        dynamicCdnHostCache.set(resolvedHost)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Collaps: Dynamic DNS resolution for interkh.com fallback to '${dynamicCdnHostCache.get()}': ${e.message}")
            }
        }
        return dynamicCdnHostCache.get()
    }

    private fun fixCdnHost(url: String): String {
        if (!url.contains(".interkh.com")) return url
        val activeHost = getActiveCdnHost()
        return url.replace(".interkh.com", ".$activeHost")
    }
}
