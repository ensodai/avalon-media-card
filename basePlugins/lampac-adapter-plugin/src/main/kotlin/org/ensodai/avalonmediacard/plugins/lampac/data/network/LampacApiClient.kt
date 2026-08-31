package org.ensodai.avalonmediacard.plugins.lampac.data.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.parsers.HlsPlaylistParser
import org.ensodai.avalonmediacard.contract.parsers.HlsResolved
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.JacRedTorrentDto
import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.LampacBalancerDto
import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.LampacResponseDto

/**
 * **Lampac Gateway REST Client**
 *
 * Communicates with the local or remote Lampac NextGen instance over HTTP.
 *
 * @property httpClient The host platform Ktor HTTP client.
 * @property logger The plugin-isolated logger.
 * @property baseUrl The root API hostname (defaults to `http://localhost:9118`).
 */
class LampacApiClient(
    private val httpClient: HttpClient,
    private val logger: PluginLogger,
    private val baseUrl: String = "http://localhost:9118"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Resolves master HLS playlist quality variants.
     */
    suspend fun resolveHls(rawHlsUrl: String): HlsResolved {
        return HlsPlaylistParser.resolveHlsPlaylist(httpClient, rawHlsUrl)
    }

    /**
     * Checks if the Lampac Gateway is online and responding.
     */
    suspend fun ping(): Boolean {
        return try {
            val response = httpClient.get("$baseUrl/reqinfo")
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.warn("Lampac: Gateway ping failed at $baseUrl: ${e.message}")
            false
        }
    }

    /**
     * Discovers all available balancers for a given media title.
     */
    suspend fun getAvailableBalancers(
        title: String,
        originalTitle: String? = null,
        year: Int? = null,
        tmdbId: Long? = null,
        imdbId: String? = null,
        kinopoiskId: Long? = null,
        isSerial: Boolean = false,
        isAnime: Boolean = false,
        originalLanguage: String? = null
    ): List<LampacBalancerDto> {
        logger.info("Lampac >> [GET /lite/events] Query: title='$title', originalTitle='$originalTitle', year=$year, tmdbId=$tmdbId, imdbId=$imdbId, kinopoiskId=$kinopoiskId, isSerial=$isSerial, isAnime=$isAnime, originalLang=$originalLanguage")
        return try {
            val response = httpClient.get("$baseUrl/lite/events") {
                parameter("title", title)
                if (!originalTitle.isNullOrBlank()) parameter("original_title", originalTitle)
                year?.let { parameter("year", it) }
                tmdbId?.let {
                    parameter("tmdb_id", it)
                    parameter("id", it)
                    parameter("source", "tmdb")
                }
                if (!imdbId.isNullOrBlank()) parameter("imdb_id", imdbId)
                kinopoiskId?.let { parameter("kinopoisk_id", it) }
                parameter("serial", if (isSerial) 1 else 0)
                if (isAnime) parameter("anime", 1)
                if (!originalLanguage.isNullOrBlank()) parameter("original_language", originalLanguage)
                parameter("rchtype", "cors")
                header(HttpHeaders.Accept, "application/json")
            }

            val body = response.bodyAsText().trim()
            logger.info("Lampac << [GET /lite/events] Status: ${response.status.value}, Raw Response: $body")

            if (response.status.isSuccess()) {
                if (body.startsWith("[")) {
                    json.decodeFromString<List<LampacBalancerDto>>(body)
                } else {
                    emptyList()
                }
            } else {
                logger.warn("Lampac: /lite/events failed with status ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.warn("Lampac: /lite/events unavailable at $baseUrl: ${e.message}")
            emptyList()
        }
    }

    /**
     * Queries a specific balancer with `rjson=true`.
     */
    suspend fun fetchFromBalancer(
        balancer: String,
        title: String? = null,
        originalTitle: String? = null,
        externalId: String? = null,
        year: Int? = null,
        tmdbId: Long? = null,
        imdbId: String? = null,
        kinopoiskId: Long? = null,
        season: Int? = null,
        episode: Int? = null,
        translationId: String? = null,
        isSerial: Boolean = false
    ): List<LampacResponseDto> {
        val cleanBalancer = balancer.removePrefix("lite/").removePrefix("/")
        logger.info("Lampac >> [GET /lite/$cleanBalancer] Query: title='$title', originalTitle='$originalTitle', id='$externalId', year=$year, tmdbId=$tmdbId, imdbId=$imdbId, kinopoiskId=$kinopoiskId, s=$season, e=$episode, t=$translationId, isSerial=$isSerial, rjson=true")
        return try {
            val response = httpClient.get("$baseUrl/lite/$cleanBalancer") {
                title?.let { parameter("title", it) }
                if (!originalTitle.isNullOrBlank()) parameter("original_title", originalTitle)
                val resolvedId = externalId ?: tmdbId?.toString()
                resolvedId?.let { parameter("id", it) }
                year?.let { parameter("year", it) }
                tmdbId?.let { parameter("tmdb_id", it) }
                if (!imdbId.isNullOrBlank()) parameter("imdb_id", imdbId)
                kinopoiskId?.let { parameter("kinopoisk_id", it) }
                season?.let { parameter("s", it) }
                episode?.let { parameter("e", it) }
                translationId?.let { parameter("t", it) }
                parameter("serial", if (isSerial) 1 else 0)
                parameter("source", "tmdb")
                parameter("rchtype", "cors")
                parameter("rjson", "true")
                header(HttpHeaders.Accept, "application/json")
            }

            val body = response.bodyAsText().trim()
            logger.info("Lampac << [GET /lite/$cleanBalancer] Status: ${response.status.value}, Raw Response: $body")

            if (response.status.isSuccess()) {
                if (body.startsWith("[")) {
                    json.decodeFromString<List<LampacResponseDto>>(body)
                } else if (body.startsWith("{")) {
                    listOf(json.decodeFromString<LampacResponseDto>(body))
                } else {
                    emptyList()
                }
            } else {
                logger.warn("Lampac: Balancer $balancer failed with status ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.warn("Lampac: Balancer $balancer error at $baseUrl: ${e.message}")
            emptyList()
        }
    }

    /**
     * Executes a raw request against a full Lampac URL or relative endpoint,
     * ensuring `rjson=true` is present.
     */
    suspend fun fetchUrl(
        url: String,
        season: Int? = null,
        episode: Int? = null,
        translationId: String? = null
    ): List<LampacResponseDto> {
        val targetUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "$baseUrl/${url.removePrefix("/")}"
        }

        logger.info("Lampac >> [GET URL] $targetUrl (s=$season, e=$episode, t=$translationId, rjson=true)")

        return try {
            val response = httpClient.get(targetUrl) {
                if (!targetUrl.contains("rjson=")) parameter("rjson", "true")
                season?.let { if (!targetUrl.contains("s=")) parameter("s", it) }
                episode?.let { if (!targetUrl.contains("e=")) parameter("e", it) }
                translationId?.let { if (!targetUrl.contains("t=")) parameter("t", it) }
                header(HttpHeaders.Accept, "application/json")
            }

            val body = response.bodyAsText().trim()
            logger.info("Lampac << [GET URL] Status: ${response.status.value}, Raw Response: $body")

            if (response.status.isSuccess()) {
                if (body.startsWith("[")) {
                    json.decodeFromString<List<LampacResponseDto>>(body)
                } else if (body.startsWith("{")) {
                    listOf(json.decodeFromString<LampacResponseDto>(body))
                } else {
                    emptyList()
                }
            } else {
                logger.warn("Lampac: Request to $targetUrl failed with status ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.warn("Lampac: Error fetching URL $targetUrl: ${e.message}")
            emptyList()
        }
    }

    /**
     * Searches for torrent releases via JacRed aggregator.
     */
    suspend fun searchTorrents(title: String, year: Int? = null): List<JacRedTorrentDto> {
        logger.info("Lampac >> [GET /api/v2/torrents] Query: title='$title', year=$year")
        return try {
            val response = httpClient.get("$baseUrl/api/v2/torrents") {
                parameter("title", title)
                year?.let { parameter("year", it) }
                header(HttpHeaders.Accept, "application/json")
            }

            val body = response.bodyAsText().trim()
            logger.info("Lampac << [GET /api/v2/torrents] Status: ${response.status.value}, Raw Response: $body")

            if (response.status.isSuccess()) {
                if (body.startsWith("[")) {
                    json.decodeFromString<List<JacRedTorrentDto>>(body)
                } else {
                    emptyList()
                }
            } else {
                logger.warn("Lampac: JacRed torrent search failed with status ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.warn("Lampac: Error searching torrents for $title: ${e.message}")
            emptyList()
        }
    }
}
