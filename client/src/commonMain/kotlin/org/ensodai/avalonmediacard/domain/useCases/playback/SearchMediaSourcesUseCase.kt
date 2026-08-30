package org.ensodai.avalonmediacard.domain.useCases.playback

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.domain.repository.PlaybackRepository
import org.koin.core.annotation.Factory

@Factory
class SearchMediaSourcesUseCase(
    private val repository: PlaybackRepository
) {
    suspend operator fun invoke(
        key: MediaKey,
        forceRefresh: Boolean = false
    ): Boolean {
        return repository.searchSources(key, forceRefresh)
    }
}
