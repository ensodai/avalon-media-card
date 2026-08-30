package org.ensodai.avalonmediacard.plugins.mediadetails.domain

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto

class GetRecommendationsUseCase(private val repository: MediaDetailsRepository) {
    suspend operator fun invoke(key: MediaKey, page: Int, language: String = "ru"): List<TmdbMovieDto> = repository.getRecommendations(key, page, language)
}

class GetSimilarUseCase(private val repository: MediaDetailsRepository) {
    suspend operator fun invoke(key: MediaKey, page: Int, language: String = "ru"): List<TmdbMovieDto> = repository.getSimilar(key, page, language)
}

class GetMediaDetailsUseCase(private val repository: MediaDetailsRepository) {
    suspend operator fun invoke(key: MediaKey, language: String = "ru"): MediaMetadata = repository.getMediaDetails(key, language = language)
}

class GetSeasonDetailsUseCase(private val repository: MediaDetailsRepository) {
    suspend operator fun invoke(
        key: MediaKey,
        seasonNumber: Int,
        language: String = "ru"
    ): List<org.ensodai.avalonmediacard.contract.slot.EpisodeItem> = repository.getSeasonDetails(key, seasonNumber, language)
}
