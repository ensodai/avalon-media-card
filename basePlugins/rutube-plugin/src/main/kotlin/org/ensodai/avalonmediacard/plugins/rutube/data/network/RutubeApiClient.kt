package org.ensodai.avalonmediacard.plugins.rutube.data.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.rutube.data.network.dto.RutubePlayOptionsDto
import org.ensodai.avalonmediacard.plugins.rutube.data.network.dto.RutubeSearchResponseDto
import org.ensodai.avalonmediacard.plugins.rutube.data.network.dto.RutubeVideoDto

/**
 * **Rutube REST API Client**
 *
 * Handles HTTP communication with public Rutube API endpoints using Ktor Client.
 *
 * @property httpClient The host platform Ktor HTTP client.
 * @property logger The plugin-isolated logger.
 * @property baseUrl The root API hostname (defaults to official `https://rutube.ru`).
 */
class RutubeApiClient(
    private val httpClient: HttpClient,
    private val logger: PluginLogger,
    private val baseUrl: String = "https://rutube.ru"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Searches for videos matching the given text query.
     *
     * @param query Search query string (e.g. "Интерстеллар 2014").
     * @param duration Optional duration filter (e.g. "movie", "long", "medium", or null).
     * @param limit Maximum number of videos to return (default 50).
     * @param page Page index (1-based).
     * @return List of matching [RutubeVideoDto] items.
     */
    suspend fun searchVideos(
        query: String,
        duration: String? = null,
        limit: Int = 50,
        page: Int = 1
    ): List<RutubeVideoDto> {
        return try {
            val response = httpClient.get("$baseUrl/api/search/video/") {
                parameter("query", query)
                parameter("content_type", "video")
                parameter("limit", limit)
                parameter("page", page)
                if (!duration.isNullOrBlank()) {
                    parameter("duration", duration)
                }
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val parsed = json.decodeFromString<RutubeSearchResponseDto>(body)
                parsed.results
            } else {
                logger.warn("Rutube: Search failed with status: ${response.status} for query: $query")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Rutube: Error searching videos for query: $query", e)
            emptyList()
        }
    }

    /**
     * Fetches all video uploads from a specific author / channel (Person API).
     *
     * @param authorId The unique author ID string or number.
     * @param limit Maximum number of videos to return (default 100).
     * @param page Page index (1-based).
     * Fetches video uploads by a specific author / channel (Person API endpoint).
     * Automatically pages through up to [maxPages] if more videos exist.
     *
     * @param authorId The unique author ID string (e.g. "68323876").
     * @param limit Maximum results limit per page (default 100).
     * @param maxPages Maximum number of pages to fetch (default 3 = up to 300 videos).
     * @return List of [RutubeVideoDto] items uploaded by this author.
     */
    suspend fun getAuthorVideos(
        authorId: String,
        limit: Int = 100,
        maxPages: Int = 3
    ): List<RutubeVideoDto> {
        val allVideos = mutableListOf<RutubeVideoDto>()
        var currentPage = 1
        var hasMore = true

        while (hasMore && currentPage <= maxPages) {
            try {
                val response = httpClient.get("$baseUrl/api/video/person/$authorId/") {
                    parameter("limit", limit)
                    parameter("page", currentPage)
                    header(HttpHeaders.Accept, "application/json")
                    header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                }

                if (response.status.isSuccess()) {
                    val body = response.bodyAsText()
                    val parsed = json.decodeFromString<RutubeSearchResponseDto>(body)
                    allVideos.addAll(parsed.results)
                    hasMore = parsed.hasNext && parsed.results.isNotEmpty()
                    currentPage++
                } else {
                    logger.warn("Rutube: Get author videos failed with status: ${response.status} for authorId: $authorId (page $currentPage)")
                    break
                }
            } catch (e: Exception) {
                logger.error("Rutube: Error fetching videos for authorId: $authorId (page $currentPage)", e)
                break
            }
        }
        return allVideos
    }

    /**
     * Retrieves video play options and master HLS stream URLs for a specific video ID.
     *
     * @param videoId The unique video identifier hash (e.g. "17465fc541700b94ebd5648423675100").
     * @return [RutubePlayOptionsDto] with HLS stream info, or null on failure.
     */
    suspend fun getPlayOptions(videoId: String): RutubePlayOptionsDto? {
        return try {
            val response = httpClient.get("$baseUrl/api/play/options/$videoId/") {
                parameter("no_404", "true")
                parameter("pver", "v2")
                parameter("client", "wdp")
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                json.decodeFromString<RutubePlayOptionsDto>(body)
            } else {
                logger.warn("Rutube: Get play options failed with status: ${response.status} for videoId: $videoId")
                null
            }
        } catch (e: Exception) {
            logger.error("Rutube: Error getting play options for videoId: $videoId", e)
            null
        }
    }

    suspend fun fetchMasterPlaylistText(url: String): String? {
        return try {
            val response = httpClient.get(url) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                header(HttpHeaders.Referrer, "https://rutube.ru/")
                header("Origin", "https://rutube.ru")
            }
            if (response.status.isSuccess()) {
                response.bodyAsText()
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Rutube: Error fetching master playlist from $url: ${e.message}")
            null
        }
    }
}
