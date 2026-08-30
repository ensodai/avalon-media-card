package org.ensodai.avalonmediacard.plugins.anilibria.data.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.anilibria.data.network.dto.AniLibriaReleaseDetailsDto
import org.ensodai.avalonmediacard.plugins.anilibria.data.network.dto.AniLibriaSearchReleaseDto

/**
 * **AniLibria REST API Client**
 *
 * Handles HTTP communication with the public AniLibria API endpoints using Ktor Client.
 *
 * @property httpClient The host platform Ktor HTTP client.
 * @property logger The plugin-isolated logger.
 * @property baseUrl The root API hostname (defaults to official `https://anilibria.top`).
 */
class AniLibriaApiClient(
    private val httpClient: HttpClient,
    private val logger: PluginLogger,
    private val baseUrl: String = "https://anilibria.top"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Searches for anime releases matching the given text query.
     *
     * @param query The search term (e.g. Russian or English title).
     * @return List of matching [AniLibriaSearchReleaseDto] or empty list on failure.
     */
    suspend fun searchReleases(query: String): List<AniLibriaSearchReleaseDto> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/app/search/releases") {
                parameter("query", query)
                header(HttpHeaders.Accept, "application/json")
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                json.decodeFromString<List<AniLibriaSearchReleaseDto>>(body)
            } else {
                logger.warn("AniLibria search failed with status: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error("Error searching AniLibria releases for query: $query", e)
            emptyList()
        }
    }

    /**
     * Fetches complete details, episodes, and stream manifests for a specific release ID.
     *
     * @param releaseId The unique AniLibria release identifier.
     * @return [AniLibriaReleaseDetailsDto] with episode list, or `null` on failure.
     */
    suspend fun getReleaseDetails(releaseId: Long): AniLibriaReleaseDetailsDto? {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/anime/releases/$releaseId") {
                header(HttpHeaders.Accept, "application/json")
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                json.decodeFromString<AniLibriaReleaseDetailsDto>(body)
            } else {
                logger.warn("AniLibria get release $releaseId failed with status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error("Error getting AniLibria release details for id: $releaseId", e)
            null
        }
    }
}
