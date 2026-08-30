package org.ensodai.avalonmediacard.domain.useCases.playback

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.rpc.StreamPlaybackResult
import org.ensodai.avalonmediacard.domain.repository.PlaybackRepository
import org.koin.core.annotation.Factory

@Factory
class GetPlaybackStreamUseCase(
    private val repository: PlaybackRepository
) {
    suspend operator fun invoke(
        key: MediaKey,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): StreamPlaybackResult {
        return repository.getStreamUrl(key, seasonNumber, episodeNumber)
    }
}
