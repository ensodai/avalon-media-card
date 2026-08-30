package org.ensodai.avalonmediacard.plugins.mediadetails.domain

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.contract.plugins.PluginContext

interface MediaDetailsRepository {
    suspend fun getRecommendations(key: MediaKey, page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun getSimilar(key: MediaKey, page: Int, language: String = "ru"): List<TmdbMovieDto>
    suspend fun getMediaDetails(
        key: MediaKey,
        requireSeasons: Boolean = true,
        requireVideos: Boolean = true,
        language: String = "ru"
    ): MediaMetadata

    suspend fun getSeasonDetails(
        key: MediaKey,
        seasonNumber: Int,
        language: String = "ru"
    ): List<org.ensodai.avalonmediacard.contract.slot.EpisodeItem>
}

class MediaDetailsRepositoryImpl(
    private val context: PluginContext
) : MediaDetailsRepository {
    override suspend fun getRecommendations(key: MediaKey, page: Int, language: String): List<TmdbMovieDto> =
        context.catalog.getRecommendations(key, page, language)

    override suspend fun getSimilar(key: MediaKey, page: Int, language: String): List<TmdbMovieDto> =
        context.catalog.getSimilar(key, page, language)

    override suspend fun getMediaDetails(
        key: MediaKey,
        requireSeasons: Boolean,
        requireVideos: Boolean,
        language: String
    ): MediaMetadata = context.catalog.getMediaDetails(key, requireSeasons, requireVideos, language)

    override suspend fun getSeasonDetails(
        key: MediaKey,
        seasonNumber: Int,
        language: String
    ): List<org.ensodai.avalonmediacard.contract.slot.EpisodeItem> = context.catalog.getSeasonDetails(key, seasonNumber, language)
}
