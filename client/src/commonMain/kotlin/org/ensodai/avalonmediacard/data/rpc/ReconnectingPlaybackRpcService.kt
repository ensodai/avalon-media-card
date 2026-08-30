package org.ensodai.avalonmediacard.data.rpc

import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.rpc.PlaybackMetadataResult
import org.ensodai.avalonmediacard.contract.rpc.PlaybackRpcService
import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.rpc.StreamPlaybackResult

class ReconnectingPlaybackRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : PlaybackRpcService {

    private suspend fun getService(): PlaybackRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun getPlaybackMetadata(
        key: MediaKey,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): PlaybackMetadataResult = executor.execute("getPlaybackMetadata", getService = { getService() }) {
        getPlaybackMetadata(key, seasonNumber, episodeNumber)
    }

    override suspend fun getStreamUrl(
        key: MediaKey,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): StreamPlaybackResult = executor.execute("getStreamUrl", getService = { getService() }) {
        getStreamUrl(key, seasonNumber, episodeNumber)
    }

    override suspend fun selectSource(
        key: MediaKey,
        providerId: String,
        sourceId: String,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): SourceSelectionResult = executor.execute("selectSource", getService = { getService() }) {
        selectSource(key, providerId, sourceId, seasonNumber, episodeNumber)
    }

    override suspend fun searchSources(
        key: MediaKey,
        forceRefresh: Boolean
    ): Boolean = executor.execute("searchSources", getService = { getService() }) {
        searchSources(key, forceRefresh)
    }
}
