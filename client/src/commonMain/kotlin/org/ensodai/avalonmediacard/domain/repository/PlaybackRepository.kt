package org.ensodai.avalonmediacard.domain.repository

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.rpc.PlaybackMetadataResult
import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.rpc.StreamPlaybackResult

interface PlaybackRepository {
    suspend fun getPlaybackMetadata(
        key: MediaKey,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): PlaybackMetadataResult

    suspend fun getStreamUrl(
        key: MediaKey,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): StreamPlaybackResult

    suspend fun selectSource(
        key: MediaKey,
        providerId: String,
        sourceId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): SourceSelectionResult

    suspend fun searchSources(
        key: MediaKey,
        forceRefresh: Boolean = false
    ): Boolean
}

