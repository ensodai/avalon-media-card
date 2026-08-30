package org.ensodai.avalonmediacard.plugins.collaps.domain.repository

import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsEmbedParseResult
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsHlsResolved
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsSearchResult

interface CollapsRepository {
    suspend fun searchMedia(query: String): List<CollapsSearchResult>
    suspend fun searchMediaByImdb(imdbId: String): List<CollapsSearchResult>
    suspend fun getEmbedParseResult(iframeUrl: String): CollapsEmbedParseResult?
    suspend fun resolveHlsPlaylist(rawHlsUrl: String): CollapsHlsResolved
}
