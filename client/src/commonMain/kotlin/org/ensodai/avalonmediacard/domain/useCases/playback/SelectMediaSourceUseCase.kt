package org.ensodai.avalonmediacard.domain.useCases.playback

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.domain.repository.PlaybackRepository
import org.koin.core.annotation.Factory

@Factory
class SelectMediaSourceUseCase(
    private val repository: PlaybackRepository
) {
    suspend operator fun invoke(
        key: MediaKey,
        providerId: String,
        sourceId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): SourceSelectionResult {
        return repository.selectSource(key, providerId, sourceId, seasonNumber, episodeNumber)
    }
}

