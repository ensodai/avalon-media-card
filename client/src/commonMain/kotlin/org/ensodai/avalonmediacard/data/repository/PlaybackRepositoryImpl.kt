package org.ensodai.avalonmediacard.data.repository

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.rpc.PlaybackMetadataResult
import org.ensodai.avalonmediacard.contract.rpc.PlaybackRpcService
import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.rpc.StreamPlaybackResult
import org.ensodai.avalonmediacard.domain.repository.PlaybackRepository
import org.koin.core.annotation.Single

@Single
class PlaybackRepositoryImpl(
    private val playbackRpcService: PlaybackRpcService
) : PlaybackRepository {

    override suspend fun getPlaybackMetadata(
        key: MediaKey,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): PlaybackMetadataResult {
        return playbackRpcService.getPlaybackMetadata(key, seasonNumber, episodeNumber)
    }

    override suspend fun getStreamUrl(
        key: MediaKey,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): StreamPlaybackResult {
        return playbackRpcService.getStreamUrl(key, seasonNumber, episodeNumber)
    }

    override suspend fun selectSource(
        key: MediaKey,
        providerId: String,
        sourceId: String,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): SourceSelectionResult {
        return playbackRpcService.selectSource(key, providerId, sourceId, seasonNumber, episodeNumber)
    }

    override suspend fun searchSources(
        key: MediaKey,
        forceRefresh: Boolean
    ): Boolean {
        return playbackRpcService.searchSources(key, forceRefresh)
    }
}

