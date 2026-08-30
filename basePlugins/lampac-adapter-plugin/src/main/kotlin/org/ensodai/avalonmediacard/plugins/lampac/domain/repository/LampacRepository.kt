package org.ensodai.avalonmediacard.plugins.lampac.domain.repository

import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.JacRedTorrentDto
import org.ensodai.avalonmediacard.plugins.lampac.domain.model.*

/**
 * Domain repository contract for interacting with the Lampac gateway.
 */
interface LampacRepository {

    /**
     * Checks if the Lampac Gateway is online.
     */
    suspend fun isGatewayAvailable(): Boolean

    /**
     * Retrieves all available balancers matching the media title.
     */
    suspend fun getAvailableBalancers(
        title: String,
        originalTitle: String? = null,
        year: Int? = null,
        tmdbId: Long? = null,
        imdbId: String? = null,
        kinopoiskId: Long? = null,
        isSerial: Boolean = false,
        isAnime: Boolean = false,
        originalLanguage: String? = null
    ): List<LampacBalancer>

    /**
     * Fetches all available movie stream variants/translations from a specific balancer.
     */
    suspend fun getMovieStreams(
        balancer: String,
        title: String,
        originalTitle: String? = null,
        year: Int? = null,
        tmdbId: Long? = null,
        imdbId: String? = null,
        kinopoiskId: Long? = null
    ): List<LampacStreamInfo>

    /**
     * Fetches seasons for a TV series from a specific balancer.
     */
    suspend fun getSeasons(
        balancer: String,
        title: String,
        originalTitle: String? = null,
        year: Int? = null,
        tmdbId: Long? = null,
        imdbId: String? = null,
        kinopoiskId: Long? = null
    ): List<LampacSeason>

    /**
     * Fetches episodes for a specific season from a balancer.
     */
    suspend fun getEpisodes(
        balancer: String,
        title: String,
        season: Int,
        originalTitle: String? = null,
        year: Int? = null,
        tmdbId: Long? = null,
        imdbId: String? = null,
        kinopoiskId: Long? = null,
        translationId: String? = null
    ): List<LampacEpisode>

    /**
     * Resolves a direct playable stream from a source descriptor.
     */
    suspend fun resolveStream(descriptor: LampacSourceDescriptor): LampacStreamInfo?

    /**
     * Searches for torrent releases via JacRed.
     */
    suspend fun searchTorrents(title: String, year: Int? = null): List<JacRedTorrentDto>
}
