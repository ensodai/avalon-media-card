package org.ensodai.avalonmediacard.plugins.rutube.domain.model

import org.ensodai.avalonmediacard.contract.plugins.VideoQuality

/**
 * Domain entity representing a video item discovered via Rutube search.
 *
 * @property id Unique video hash string.
 * @property title Video title.
 * @property durationSeconds Duration in seconds.
 * @property posterUrl Full image preview URL.
 * @property authorName Channel / Author name.
 */
data class RutubeVideoItem(
    val id: String,
    val title: String,
    val durationSeconds: Double,
    val posterUrl: String?,
    val authorName: String?,
    val authorId: String? = null
)

/**
 * Domain entity representing stream URLs and qualities for playback.
 *
 * @property videoId Unique video ID.
 * @property title Video title.
 * @property masterHlsUrl Direct master `.m3u8` HLS URL.
 * @property qualities Available dynamic video qualities.
 */
data class RutubeStreamInfo(
    val videoId: String,
    val title: String,
    val masterHlsUrl: String,
    val qualities: List<VideoQuality> = emptyList()
)

/**
 * Domain entity representing an episode mapped to a season and episode number.
 */
data class RutubeMappedEpisode(
    val video: RutubeVideoItem,
    val season: Int,
    val episode: Int
)
