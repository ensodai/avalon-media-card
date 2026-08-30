package org.ensodai.avalonmediacard.tmdb

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.contract.model.TmdbMultiSearchDto
import org.ensodai.avalonmediacard.tmdb.responses.TmdbPersonDetailResponse
import org.koin.core.annotation.Single

interface TmdbRepository {
    suspend fun getTrendingMovies(page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun getTopRatedMovies(page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun getUpcomingMovies(page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun getTrendingShows(page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun getPopularShows(page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun getTopRatedShows(page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun getMovieDetails(mediaId: String, appendVideos: Boolean = true, language: String = "ru"): TmdbDetails?
    suspend fun getRecommendations(mediaId: String, page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun getSimilarMovies(mediaId: String, page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun searchMovies(query: String, page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun searchMulti(query: String, page: Int, language: String = "ru"): List<TmdbMultiSearchDto>
    suspend fun getPersonDetails(personId: String, language: String = "ru"): TmdbPersonDetailResponse?
    suspend fun getSeasonDetails(
        mediaId: String,
        seasonNumber: Int,
        language: String = "ru"
    ): org.ensodai.avalonmediacard.tmdb.responses.TmdbSeasonDetailResponse?

    suspend fun discoverMedia(genres: List<Int>, keywords: List<Int>, page: Int, isTv: Boolean, language: String = "ru"): List<TmdbMovieDto>
    suspend fun discoverMediaByParams(
        params: Map<String, String>,
        targetType: EntityType,
        page: Int,
        language: String = "ru"
    ): List<TmdbMovieDto>
}

@Single
class TmdbRepositoryImpl(
    private val client: TmdbApi
) : TmdbRepository {

    private var lastMovieDetails: Pair<String, TmdbDetails>? = null

    override suspend fun getTrendingMovies(page: Int, language: String): List<TmdbMovieDto> {
        return client.getTrendingMovies(page, language)
    }

    override suspend fun getTopRatedMovies(page: Int, language: String): List<TmdbMovieDto> {
        return client.getTopRatedMovies(page, language)
    }

    override suspend fun getUpcomingMovies(page: Int, language: String): List<TmdbMovieDto> {
        return client.getUpcomingMovies(page, language)
    }

    override suspend fun getTrendingShows(page: Int, language: String): List<TmdbMovieDto> {
        return client.getTrendingShows(page, language)
    }

    override suspend fun getPopularShows(page: Int, language: String): List<TmdbMovieDto> {
        return client.getPopularShows(page, language)
    }

    override suspend fun getTopRatedShows(page: Int, language: String): List<TmdbMovieDto> {
        return client.getTopRatedShows(page, language)
    }

    override suspend fun getMovieDetails(mediaId: String, appendVideos: Boolean, language: String): TmdbDetails? {
        val cached = lastMovieDetails
        if (cached != null && cached.first == "${mediaId}_$language") {
            return cached.second
        }
        val details = client.getMovieDetails(mediaId, appendVideos, language)
        if (details != null) {
            lastMovieDetails = "${mediaId}_$language" to details
        }
        return details
    }

    override suspend fun getRecommendations(mediaId: String, page: Int, language: String): List<TmdbMovieDto> {
        return client.getRecommendations(mediaId, page, language)
    }

    override suspend fun getSimilarMovies(mediaId: String, page: Int, language: String): List<TmdbMovieDto> {
        return client.getSimilarMovies(mediaId, page, language)
    }

    override suspend fun searchMovies(query: String, page: Int, language: String): List<TmdbMovieDto> {
        return client.searchMovies(query, page, language)
    }

    override suspend fun searchMulti(query: String, page: Int, language: String): List<TmdbMultiSearchDto> {
        return client.searchMulti(query, page, language)
    }

    override suspend fun getPersonDetails(personId: String, language: String): TmdbPersonDetailResponse? {
        return client.getPersonDetails(personId, language)
    }

    override suspend fun getSeasonDetails(
        mediaId: String,
        seasonNumber: Int,
        language: String
    ): org.ensodai.avalonmediacard.tmdb.responses.TmdbSeasonDetailResponse? {
        return client.getSeasonDetails(mediaId, seasonNumber, language)
    }

    override suspend fun discoverMedia(
        genres: List<Int>,
        keywords: List<Int>,
        page: Int,
        isTv: Boolean,
        language: String
    ): List<TmdbMovieDto> {
        return client.discoverMedia(genres, keywords, page, isTv, language)
    }

    override suspend fun discoverMediaByParams(
        params: Map<String, String>,
        targetType: EntityType,
        page: Int,
        language: String
    ): List<TmdbMovieDto> {
        return client.discoverMediaByParams(params, targetType, page, language)
    }
}
