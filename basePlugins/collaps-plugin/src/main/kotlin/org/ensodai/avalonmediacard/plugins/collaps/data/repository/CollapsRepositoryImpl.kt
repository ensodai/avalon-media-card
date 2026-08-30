package org.ensodai.avalonmediacard.plugins.collaps.data.repository

import org.ensodai.avalonmediacard.plugins.collaps.data.network.CollapsApiClient
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsEmbedParseResult
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsHlsResolved
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsSearchResult
import org.ensodai.avalonmediacard.plugins.collaps.domain.repository.CollapsRepository

class CollapsRepositoryImpl(
    private val apiClient: CollapsApiClient
) : CollapsRepository {

    override suspend fun searchMedia(query: String): List<CollapsSearchResult> {
        return apiClient.searchMedia(query)
    }

    override suspend fun searchMediaByImdb(imdbId: String): List<CollapsSearchResult> {
        return apiClient.searchMediaByImdb(imdbId)
    }

    override suspend fun getEmbedParseResult(iframeUrl: String): CollapsEmbedParseResult? {
        return apiClient.fetchEmbedPage(iframeUrl)
    }

    override suspend fun resolveHlsPlaylist(rawHlsUrl: String): CollapsHlsResolved {
        return apiClient.resolveHlsPlaylist(rawHlsUrl)
    }
}
