package org.ensodai.avalonmediacard.plugins.rutube.domain.repository

import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeMappedEpisode
import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeStreamInfo
import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeVideoItem

/**
 * **Rutube Domain Repository Interface**
 *
 * Defines operations for querying videos and extracting HLS video streams.
 */
interface RutubeRepository {

    /**
     * Searches for videos matching the given query and optional duration filter.
     *
     * @param query Search query text.
     * @param duration Optional duration filter (e.g. "movie", "long", "medium").
     * @param limit Maximum results limit (default 50).
     * @return List of matching [RutubeVideoItem] results.
     */
    suspend fun searchVideos(query: String, duration: String? = null, limit: Int = 50, page: Int = 1): List<RutubeVideoItem>

    /**
     * Fetches all video uploads from a specific author / channel (Person API).
     *
     * @param authorId The unique author ID string.
     * @param limit Maximum results limit (default 100).
     * @return List of [RutubeVideoItem] results from this author.
     */
    suspend fun getAuthorVideos(authorId: String, limit: Int = 100): List<RutubeVideoItem>

    /**
     * Fetches playback stream info and master HLS URLs for the specified video ID.
     *
     * @param videoId Unique Rutube video identifier.
     * @return [RutubeStreamInfo] or null if playback is unavailable.
     */
    suspend fun getStreamInfo(videoId: String): RutubeStreamInfo?
}
