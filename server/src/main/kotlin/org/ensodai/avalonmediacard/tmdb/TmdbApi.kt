package org.ensodai.avalonmediacard.tmdb

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.contract.model.TmdbMultiSearchDto
import org.ensodai.avalonmediacard.contract.model.KeywordMetadata
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.ensodai.avalonmediacard.tmdb.responses.*
import org.ensodai.avalonmediacard.utils.EnvHelper
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory

@Single
class TmdbApi(
    private val client: HttpClient,
    private val systemSettingsRepository: SystemSettingsRepository
) {
    private val logger = LoggerFactory.getLogger(TmdbApi::class.java)

    suspend fun resetClient() {
        logger.info("TMDB client reset. (No cache in V2)")
    }

    private suspend fun getToken(): String? {
        return systemSettingsRepository.getSetting("tmdb_read_token") ?: EnvHelper.getEnv("TMDB_READ_TOKEN")
    }

    suspend fun validateToken(token: String): Boolean {
        return try {
            val response = client.get("https://api.themoviedb.org/3/authentication") {
                header("Authorization", "Bearer $token")
                timeout {
                    requestTimeoutMillis = 10000
                    connectTimeoutMillis = 3000
                    socketTimeoutMillis = 10000
                }
            }
            response.status.value == 200
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error("TMDB Token validation failed", e)
            false
        }
    }

    private suspend inline fun <reified T> getTmdbData(url: String, params: Map<String, Any> = emptyMap()): T? {
        val token = getToken() ?: throw TmdbAuthException("Токен TMDB отсутствует")
        return try {
            val response = client.get(url) {
                header("Authorization", "Bearer $token")
                timeout {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 5000
                    socketTimeoutMillis = 15000
                }
                if (!params.containsKey("language")) {
                    parameter("language", "ru")
                }
                params.forEach { (k, v) -> parameter(k, v) }
            }
            if (response.status.value == 401 || response.status.value == 403) {
                throw TmdbAuthException("Ошибка авторизации TMDB (неверный токен)")
            }
            if (response.status.value != 200) {
                val errorBody = response.bodyAsText()
                logger.warn("TMDB request failed: $url with status ${response.status}. Body: $errorBody")
                throw TmdbNetworkException("Ошибка TMDB: ${response.status.value}")
            }
            response.body<T>()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (e is TmdbException) throw e
            if (e is java.net.SocketTimeoutException || e is io.ktor.client.plugins.HttpRequestTimeoutException) {
                throw TmdbTimeoutException("Превышено время ожидания ответа от TMDB", e)
            }
            throw TmdbNetworkException("Ошибка сети при запросе к TMDB: ${e.message}", e)
        }
    }

    suspend fun getTrendingMovies(page: Int = 1, language: String = "ru"): List<TmdbMovieDto> {
        val response = getTmdbData<TmdbPageResultResponse<TmdbMovieResponse>>(
            "https://api.themoviedb.org/3/trending/movie/week",
            mapOf("page" to page, "language" to language)
        )
        return response?.results?.map { it.toDto() } ?: emptyList()
    }

    private fun extractNumericId(id: String): Int? = id.substringAfterLast(":").toIntOrNull()

    suspend fun getMovieDetails(mediaId: String, appendVideos: Boolean = true, language: String = "ru"): TmdbDetails? {
        val isTv = mediaId.startsWith("tv:") || mediaId.contains("tv")
        val cleanId = extractNumericId(mediaId) ?: return null

        val appendValue = if (appendVideos) "credits,videos,external_ids,images,translations" else "credits,external_ids,images,translations"
        val queryParams = mapOf(
            "append_to_response" to appendValue,
            "include_image_language" to "en,ru,null",
            "language" to language
        )

        return if (isTv) {
            val detail = getTmdbData<TmdbShowDetailResponse>(
                "https://api.themoviedb.org/3/tv/$cleanId",
                queryParams
            )
            if (detail != null) TmdbDetails.Show(detail) else null
        } else {
            val detail = getTmdbData<TmdbMovieDetailResponse>(
                "https://api.themoviedb.org/3/movie/$cleanId",
                queryParams
            )
            if (detail != null) TmdbDetails.Movie(detail) else null
        }
    }

    suspend fun getPersonDetails(personId: String, language: String = "ru"): TmdbPersonDetailResponse? {
        val cleanId = extractNumericId(personId) ?: return null
        return getTmdbData<TmdbPersonDetailResponse>(
            "https://api.themoviedb.org/3/person/$cleanId",
            mapOf("append_to_response" to "combined_credits", "language" to language)
        )
    }

    suspend fun getRecommendations(mediaId: String, page: Int = 1, language: String = "ru"): List<TmdbMovieDto> {
        val isTv = mediaId.startsWith("tv:") || mediaId.contains("tv")
        val cleanId = extractNumericId(mediaId) ?: return emptyList()
        val type = if (isTv) "tv" else "movie"

        val response = getTmdbData<TmdbPageResultResponse<TmdbDiscoverItemRaw>>(
            "https://api.themoviedb.org/3/$type/$cleanId/recommendations",
            mapOf("page" to page, "language" to language)
        )
        return response?.results?.map { if (isTv) it.toShowDto() else it.toMovieDto() } ?: emptyList()
    }

    suspend fun getSimilarMovies(mediaId: String, page: Int = 1, language: String = "ru"): List<TmdbMovieDto> {
        val isTv = mediaId.startsWith("tv:") || mediaId.contains("tv")
        val cleanId = extractNumericId(mediaId) ?: return emptyList()
        val type = if (isTv) "tv" else "movie"

        val response = getTmdbData<TmdbPageResultResponse<TmdbDiscoverItemRaw>>(
            "https://api.themoviedb.org/3/$type/$cleanId/similar",
            mapOf("page" to page, "language" to language)
        )
        return response?.results?.map { if (isTv) it.toShowDto() else it.toMovieDto() } ?: emptyList()
    }

    suspend fun searchMovies(query: String, page: Int = 1, language: String = "ru"): List<TmdbMovieDto> {
        val response = getTmdbData<TmdbPageResultResponse<TmdbMovieResponse>>(
            "https://api.themoviedb.org/3/search/movie",
            mapOf("query" to query, "page" to page, "include_adult" to false, "language" to language)
        )
        return response?.results?.map { it.toDto() } ?: emptyList()
    }

    suspend fun searchMulti(query: String, page: Int = 1, language: String = "ru"): List<TmdbMultiSearchDto> {
        val response = getTmdbData<TmdbPageResultResponse<TmdbMultiSearchItemRaw>>(
            "https://api.themoviedb.org/3/search/multi",
            mapOf("query" to query, "page" to page, "include_adult" to false, "language" to language)
        )
        return response?.results?.mapNotNull { it.toDto() } ?: emptyList()
    }

    suspend fun getTopRatedMovies(page: Int = 1, language: String = "ru"): List<TmdbMovieDto> {
        val response = getTmdbData<TmdbPageResultResponse<TmdbMovieResponse>>(
            "https://api.themoviedb.org/3/movie/top_rated",
            mapOf("page" to page, "language" to language)
        )
        return response?.results?.map { it.toDto() } ?: emptyList()
    }

    suspend fun getUpcomingMovies(page: Int = 1, language: String = "ru"): List<TmdbMovieDto> {
        val response = getTmdbData<TmdbPageResultResponse<TmdbMovieResponse>>(
            "https://api.themoviedb.org/3/movie/upcoming",
            mapOf("page" to page, "language" to language)
        )
        return response?.results?.map { it.toDto() } ?: emptyList()
    }

    suspend fun getTrendingShows(page: Int = 1, language: String = "ru"): List<TmdbMovieDto> {
        val response = getTmdbData<TmdbPageResultResponse<TmdbShowResponse>>(
            "https://api.themoviedb.org/3/trending/tv/week",
            mapOf("page" to page, "language" to language)
        )
        return response?.results?.map { it.toDto() } ?: emptyList()
    }

    suspend fun getPopularShows(page: Int = 1, language: String = "ru"): List<TmdbMovieDto> {
        val response = getTmdbData<TmdbPageResultResponse<TmdbShowResponse>>(
            "https://api.themoviedb.org/3/tv/popular",
            mapOf("page" to page, "language" to language)
        )
        return response?.results?.map { it.toDto() } ?: emptyList()
    }

    suspend fun getTopRatedShows(page: Int = 1, language: String = "ru"): List<TmdbMovieDto> {
        val response = getTmdbData<TmdbPageResultResponse<TmdbShowResponse>>(
            "https://api.themoviedb.org/3/tv/top_rated",
            mapOf("page" to page, "language" to language)
        )
        return response?.results?.map { it.toDto() } ?: emptyList()
    }

    suspend fun getSeasonDetails(mediaId: String, seasonNumber: Int, language: String = "ru"): TmdbSeasonDetailResponse? {
        val cleanId = extractNumericId(mediaId) ?: return null
        return getTmdbData<TmdbSeasonDetailResponse>(
            "https://api.themoviedb.org/3/tv/$cleanId/season/$seasonNumber",
            mapOf("language" to language)
        )
    }

    suspend fun getEnglishKeywords(mediaId: String): List<KeywordMetadata> {
        val isTv = mediaId.startsWith("tv:") || mediaId.contains("tv")
        val cleanId = extractNumericId(mediaId) ?: return emptyList()
        val type = if (isTv) "tv" else "movie"

        val token = getToken() ?: return emptyList()
        return try {
            val response = client.get("https://api.themoviedb.org/3/$type/$cleanId/keywords") {
                header("Authorization", "Bearer $token")
                parameter("language", "en-US")
            }
            if (response.status.value != 200) return emptyList()
            val body = response.body<TmdbKeywordsResponseRaw>()
            val list = body.keywords ?: body.results ?: emptyList()
            list.map { KeywordMetadata(id = it.id, name = it.name) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error("Error fetching English keywords for $mediaId", e)
            emptyList()
        }
    }

    suspend fun discoverMedia(
        genres: List<Int>,
        keywords: List<Int>,
        page: Int = 1,
        isTv: Boolean = false,
        language: String = "ru"
    ): List<TmdbMovieDto> {
        val type = if (isTv) "tv" else "movie"
        val token = getToken() ?: return emptyList()
        return try {
            val response = client.get("https://api.themoviedb.org/3/discover/$type") {
                header("Authorization", "Bearer $token")
                parameter("language", language)
                parameter("page", page)
                parameter("sort_by", "popularity.desc")
                if (genres.isNotEmpty()) {
                    parameter("with_genres", genres.joinToString("|"))
                }
                if (keywords.isNotEmpty()) {
                    parameter("with_keywords", keywords.joinToString("|"))
                }
            }
            if (response.status.value != 200) return emptyList()
            val body = response.body<TmdbDiscoverResponseRaw>()
            body.results?.map { if (isTv) it.toShowDto() else it.toMovieDto() } ?: emptyList()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error("Error discovering $type", e)
            emptyList()
        }
    }

    suspend fun discoverMediaByParams(
        params: Map<String, String>,
        targetType: EntityType,
        page: Int = 1,
        language: String = "ru"
    ): List<TmdbMovieDto> {
        val type = if (targetType == EntityType.TV) "tv" else "movie"
        val token = getToken() ?: return emptyList()
        return try {
            val response = client.get("https://api.themoviedb.org/3/discover/$type") {
                header("Authorization", "Bearer $token")
                parameter("language", language)
                parameter("page", page)
                params.forEach { (k, v) -> parameter(k, v) }
            }
            if (response.status.value != 200) return emptyList()
            val body = response.body<TmdbDiscoverResponseRaw>()
            val isTv = targetType == EntityType.TV
            body.results?.map { if (isTv) it.toShowDto() else it.toMovieDto() } ?: emptyList()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error("Error discovering by params $type", e)
            emptyList()
        }
    }

    internal suspend fun getGenres(isTv: Boolean, language: String): List<TmdbGenreResponse> {
        val type = if (isTv) "tv" else "movie"
        val token = getToken() ?: return emptyList()
        return try {
            val response = client.get("https://api.themoviedb.org/3/genre/$type/list") {
                header("Authorization", "Bearer $token")
                parameter("language", language)
            }
            if (response.status.value != 200) return emptyList()
            val body = response.body<TmdbGenresResponseRaw>()
            body.genres ?: emptyList()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error("Error fetching genres for $type ($language)", e)
            emptyList()
        }
    }
}

@kotlinx.serialization.Serializable
internal data class TmdbDiscoverItemRaw(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @kotlinx.serialization.SerialName("original_title") val originalTitle: String? = null,
    @kotlinx.serialization.SerialName("original_name") val originalName: String? = null,
    @kotlinx.serialization.SerialName("poster_path") val posterPath: String? = null,
    @kotlinx.serialization.SerialName("backdrop_path") val backdropPath: String? = null,
    @kotlinx.serialization.SerialName("release_date") val releaseDate: String? = null,
    @kotlinx.serialization.SerialName("first_air_date") val firstAirDate: String? = null,
    @kotlinx.serialization.SerialName("vote_average") val voteAverage: Double? = null,
    val overview: String? = null
) {
    fun toMovieDto() = TmdbMovieDto(
        id = id,
        title = title,
        name = null,
        originalTitle = originalTitle,
        originalName = null,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage ?: 0.0,
        overview = overview
    )

    fun toShowDto() = TmdbMovieDto(
        id = id,
        title = null,
        name = name,
        originalTitle = null,
        originalName = originalName,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = firstAirDate,
        voteAverage = voteAverage ?: 0.0,
        overview = overview
    )
}

@kotlinx.serialization.Serializable
internal data class TmdbMultiSearchItemRaw(
    val id: Int,
    @kotlinx.serialization.SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @kotlinx.serialization.SerialName("original_title") val originalTitle: String? = null,
    @kotlinx.serialization.SerialName("original_name") val originalName: String? = null,
    @kotlinx.serialization.SerialName("poster_path") val posterPath: String? = null,
    @kotlinx.serialization.SerialName("backdrop_path") val backdropPath: String? = null,
    @kotlinx.serialization.SerialName("release_date") val releaseDate: String? = null,
    @kotlinx.serialization.SerialName("first_air_date") val firstAirDate: String? = null,
    @kotlinx.serialization.SerialName("vote_average") val voteAverage: Double? = null,
    val overview: String? = null
) {
    fun toDto(): TmdbMultiSearchDto? {
        if (mediaType != "movie" && mediaType != "tv" && mediaType != "person") return null
        return TmdbMultiSearchDto(
            id = id,
            mediaType = mediaType,
            title = title,
            name = name,
            originalTitle = originalTitle,
            originalName = originalName,
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseDate = releaseDate,
            firstAirDate = firstAirDate,
            voteAverage = voteAverage,
            overview = overview
        )
    }
}

@kotlinx.serialization.Serializable
internal data class TmdbDiscoverResponseRaw(
    val results: List<TmdbDiscoverItemRaw>? = null
)

@kotlinx.serialization.Serializable
internal data class TmdbKeywordRawDto(val id: Int, val name: String)

@kotlinx.serialization.Serializable
internal data class TmdbKeywordsResponseRaw(
    val keywords: List<TmdbKeywordRawDto>? = null,
    val results: List<TmdbKeywordRawDto>? = null
)

@kotlinx.serialization.Serializable
internal data class TmdbGenresResponseRaw(
    val genres: List<TmdbGenreResponse>? = null
)

internal fun TmdbMovieResponse.toDto(): TmdbMovieDto = TmdbMovieDto(
    id = id,
    title = title,
    name = null,
    originalTitle = originalTitle,
    originalName = null,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage ?: 0.0,
    overview = overview
)

internal fun TmdbShowResponse.toDto(): TmdbMovieDto = TmdbMovieDto(
    id = id,
    title = null,
    name = name,
    originalTitle = null,
    originalName = originalName,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = firstAirDate,
    voteAverage = voteAverage ?: 0.0,
    overview = overview
)
