package org.ensodai.avalonmediacard.domain.useCases.playback

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.rpc.PlaybackMetadataResult
import org.ensodai.avalonmediacard.domain.repository.PlaybackRepository
import org.koin.core.annotation.Factory

@Factory
class GetPlaybackMetadataUseCase(
    private val repository: PlaybackRepository
) {
    suspend operator fun invoke(
        key: MediaKey,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): PlaybackMetadataResult {
        return repository.getPlaybackMetadata(key, seasonNumber, episodeNumber)
    }
}
