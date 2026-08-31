package org.ensodai.avalonmediacard.plugins.vk.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkAnonymTokenResponse
import org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkMethodAnonymTokenResponse
import org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkRootResponseDto
import org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkVideoDto
import kotlin.time.Clock

/**
 * High-performance API client for VK Video search and stream extraction.
 * Handles automatic anonymous token acquisition and search requests.
 */
class VkApiClient(
    private val httpClient: HttpClient,
    private val logger: PluginLogger
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val tokenMutex = Mutex()
    private var cachedToken: String? = null
    private var tokenExpiryEpochMs: Long = 0L

    companion object {
        private const val CLIENT_ID = "52461373"
        private const val CLIENT_SECRET = "o557NLIkAErNhakXrQ7A"
        private const val APP_ID = "6287487"
        private const val API_VERSION = "5.264"
        private const val PRIMARY_TOKEN_URL = "https://api.vkvideo.ru/method/auth.getAnonymToken"
        private const val FALLBACK_TOKEN_URL = "https://login.vk.com/?act=get_anonym_token"
        private const val SEARCH_URL_VKVIDEO = "https://api.vkvideo.ru/method/catalog.getVideoSearchWeb2"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    /**
     * Ensures a valid anonymous access token exists, fetching a new one if expired.
     */
    suspend fun getAnonymousToken(): String? {
        val now = Clock.System.now().toEpochMilliseconds()
        if (cachedToken != null && now < tokenExpiryEpochMs) {
            return cachedToken
        }

        return tokenMutex.withLock {
            val currentNow = Clock.System.now().toEpochMilliseconds()
            if (cachedToken != null && currentNow < tokenExpiryEpochMs) {
                return@withLock cachedToken
            }

            // 1. Try primary fast endpoint on dedicated api.vkvideo.ru CDN
            try {
                val fullUrl = "$PRIMARY_TOKEN_URL?v=$API_VERSION&client_id=$CLIENT_ID"
                val response = httpClient.submitForm(
                    url = fullUrl,
                    formParameters = Parameters.build {
                        append("client_secret", CLIENT_SECRET)
                        append("app_id", APP_ID)
                    }
                ) {
                    timeout {
                        requestTimeoutMillis = 8_000L
                        connectTimeoutMillis = 5_000L
                    }
                    header("User-Agent", USER_AGENT)
                    header("Origin", "https://vkvideo.ru")
                    header("Referer", "https://vkvideo.ru/")
                }

                val body = response.bodyAsText()
                val parsed = json.decodeFromString<VkMethodAnonymTokenResponse>(body)
                val token = parsed.response?.token
                if (!token.isNullOrBlank()) {
                    cachedToken = token
                    val expiresSec = parsed.response.expiredAt ?: 0L
                    tokenExpiryEpochMs = if (expiresSec > 0) {
                        (expiresSec * 1000) - (60 * 60 * 1000)
                    } else {
                        currentNow + (10 * 3600 * 1000)
                    }
                    logger.info("VK Video: Anonymous token successfully acquired from api.vkvideo.ru")
                    return@withLock token
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.warn("VK Video: Primary token endpoint failed (${e.message}), trying fallback...")
            }

            // 2. Fallback to login.vk.com
            try {
                val response = httpClient.submitForm(
                    url = FALLBACK_TOKEN_URL,
                    formParameters = Parameters.build {
                        append("client_secret", CLIENT_SECRET)
                        append("client_id", CLIENT_ID)
                        append("scopes", "audio_anonymous,video_anonymous,photos_anonymous,profile_anonymous")
                        append("isApiOauthAnonymEnabled", "false")
                        append("version", "1")
                        append("app_id", APP_ID)
                    }
                ) {
                    timeout {
                        requestTimeoutMillis = 8_000L
                        connectTimeoutMillis = 5_000L
                    }
                    header("User-Agent", USER_AGENT)
                    header("Origin", "https://vkvideo.ru")
                    header("Referer", "https://vkvideo.ru/")
                }

                val body = response.bodyAsText()
                val tokenResponse = json.decodeFromString<VkAnonymTokenResponse>(body)
                val token = tokenResponse.data?.accessToken

                if (!token.isNullOrBlank()) {
                    cachedToken = token
                    val expiresSec = tokenResponse.data.expires ?: tokenResponse.data.expiredAt ?: 0L
                    tokenExpiryEpochMs = if (expiresSec > 0) {
                        (expiresSec * 1000) - (60 * 60 * 1000)
                    } else {
                        currentNow + (10 * 3600 * 1000)
                    }
                    logger.info("VK Video: Anonymous token acquired via fallback login.vk.com")
                    return@withLock token
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.error("VK Video: Error acquiring anonymous token from fallback: ${e.message}")
            }

            cachedToken
        }
    }

    /**
     * Searches VK Video catalog for videos matching the query.
     */
    suspend fun searchVideos(query: String): List<VkVideoDto> {
        val token = getAnonymousToken() ?: return emptyList()

        try {
            val fullUrl = "$SEARCH_URL_VKVIDEO?v=$API_VERSION&client_id=$CLIENT_ID"
            val response = httpClient.submitForm(
                url = fullUrl,
                formParameters = Parameters.build {
                    append("screen_ref", "search_video_service")
                    append("input_method", "keyboard_search_button")
                    append("q", query)
                    append("access_token", token)
                    append("content_type", "video")
                    append("hd", "1")
                    append("sort", "2")
                    append("extended", "1")
                }
            ) {
                timeout {
                    requestTimeoutMillis = 10_000L
                    connectTimeoutMillis = 5_000L
                }
                header("User-Agent", USER_AGENT)
                header("Origin", "https://vkvideo.ru")
                header("Referer", "https://vkvideo.ru/")
            }

            val body = response.bodyAsText()
            val root = json.decodeFromString<VkRootResponseDto>(body)
            val items = root.response?.catalogVideos?.mapNotNull { it.video } ?: emptyList()
            if (items.isNotEmpty()) {
                return items
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.warn("VK Video search failed for query '$query': ${e.message}")
        }

        return emptyList()
    }

    /**
     * Searches VK Video catalog for playlists/albums matching the query.
     */
    suspend fun searchAlbums(query: String): List<org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkAlbumDto> {
        val token = getAnonymousToken() ?: return emptyList()

        try {
            val fullUrl = "$SEARCH_URL_VKVIDEO?v=$API_VERSION&client_id=$CLIENT_ID"
            val response = httpClient.submitForm(
                url = fullUrl,
                formParameters = Parameters.build {
                    append("screen_ref", "search_video_service")
                    append("input_method", "keyboard_search_button")
                    append("q", query)
                    append("access_token", token)
                    append("extended", "1")
                }
            ) {
                timeout {
                    requestTimeoutMillis = 10_000L
                    connectTimeoutMillis = 5_000L
                }
                header("User-Agent", USER_AGENT)
                header("Origin", "https://vkvideo.ru")
                header("Referer", "https://vkvideo.ru/")
            }

            val body = response.bodyAsText()
            val root = json.decodeFromString<VkRootResponseDto>(body)
            return root.response?.albums ?: emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.warn("VK Video album search failed for query '$query': ${e.message}")
        }

        return emptyList()
    }

    /**
     * Fetches all videos belonging to a specific VK Video album / playlist.
     */
    suspend fun getAlbumVideos(ownerId: Long, albumId: Long): List<VkVideoDto> {
        val token = getAnonymousToken() ?: return emptyList()

        try {
            val fullUrl = "https://api.vkvideo.ru/method/video.get?v=$API_VERSION&client_id=$CLIENT_ID"
            val response = httpClient.submitForm(
                url = fullUrl,
                formParameters = Parameters.build {
                    append("owner_id", ownerId.toString())
                    append("album_id", albumId.toString())
                    append("count", "100")
                    append("extended", "1")
                    append("access_token", token)
                }
            ) {
                timeout {
                    requestTimeoutMillis = 10_000L
                    connectTimeoutMillis = 5_000L
                }
                header("User-Agent", USER_AGENT)
                header("Origin", "https://vkvideo.ru")
                header("Referer", "https://vkvideo.ru/")
            }

            val body = response.bodyAsText()
            val getResponse = json.decodeFromString<org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkVideoGetResponseDto>(body)
            return getResponse.response?.items ?: emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("VK Video failed to fetch album videos for owner=$ownerId album=$albumId: ${e.message}")
        }

        return emptyList()
    }

    /**
     * Fetches details and direct media streams for a specific VK video.
     */
    suspend fun getVideo(ownerId: Long, videoId: Long): VkVideoDto? {
        val token = getAnonymousToken() ?: return null

        try {
            val fullUrl = "https://api.vkvideo.ru/method/video.get?v=$API_VERSION&client_id=$CLIENT_ID"
            val response = httpClient.submitForm(
                url = fullUrl,
                formParameters = Parameters.build {
                    append("videos", "${ownerId}_${videoId}")
                    append("extended", "1")
                    append("access_token", token)
                }
            ) {
                timeout {
                    requestTimeoutMillis = 10_000L
                    connectTimeoutMillis = 5_000L
                }
                header("User-Agent", USER_AGENT)
                header("Origin", "https://vkvideo.ru")
                header("Referer", "https://vkvideo.ru/")
            }

            val body = response.bodyAsText()
            val getResponse = json.decodeFromString<org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkVideoGetResponseDto>(body)
            return getResponse.response?.items?.firstOrNull()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("VK Video failed to fetch video ${ownerId}_${videoId}: ${e.message}")
        }

        return null
    }
}
