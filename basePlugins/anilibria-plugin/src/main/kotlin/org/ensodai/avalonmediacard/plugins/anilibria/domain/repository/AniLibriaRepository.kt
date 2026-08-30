package org.ensodai.avalonmediacard.plugins.anilibria.domain.repository

import org.ensodai.avalonmediacard.plugins.anilibria.domain.model.AniLibriaRelease
import org.ensodai.avalonmediacard.plugins.anilibria.domain.model.AniLibriaReleaseDetails

/**
 * **AniLibria Domain Repository Interface**
 *
 * Defines domain operations for querying anime releases and episode streams.
 */
interface AniLibriaRepository {

    /**
     * Searches for anime releases matching the given query string.
     *
     * @param query Search query (Russian or English title).
     * @return List of matching [AniLibriaRelease] items.
     */
    suspend fun searchReleases(query: String): List<AniLibriaRelease>

    /**
     * Fetches detailed information, episode list, and HLS stream links for a release.
     *
     * @param releaseId Unique AniLibria release identifier.
     * @return [AniLibriaReleaseDetails] or null if not found.
     */
    suspend fun getReleaseDetails(releaseId: Long): AniLibriaReleaseDetails?
}
