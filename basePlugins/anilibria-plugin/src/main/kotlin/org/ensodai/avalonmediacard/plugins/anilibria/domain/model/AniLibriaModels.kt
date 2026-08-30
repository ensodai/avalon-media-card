package org.ensodai.avalonmediacard.plugins.anilibria.domain.model

import org.ensodai.avalonmediacard.contract.plugins.VideoQuality

/**
 * Domain entity representing an anime release summary found via search.
 *
 * @property id Unique release ID in AniLibria's database.
 * @property titleRu Russian localized title.
 * @property titleEn English / Romaji title.
 * @property year Release broadcast year.
 * @property alias URL slug / alias of the release.
 * @property posterUrl Full direct CDN URL to the release cover art.
 * @property episodesTotal Total planned episodes in this release (or null if ongoing).
 */
data class AniLibriaRelease(
    val id: Long,
    val titleRu: String,
    val titleEn: String?,
    val year: Int?,
    val alias: String?,
    val posterUrl: String?,
    val episodesTotal: Int?
)

/**
 * Domain entity representing detailed release metadata and full list of available episodes.
 *
 * @property id Unique release ID.
 * @property titleRu Russian localized title.
 * @property titleEn English / Romaji title.
 * @property year Broadcast year.
 * @property alias URL slug / alias.
 * @property episodes Ordered list of available episodes on AniLibria CDN.
 */
data class AniLibriaReleaseDetails(
    val id: Long,
    val titleRu: String,
    val titleEn: String?,
    val year: Int?,
    val alias: String?,
    val episodes: List<AniLibriaEpisode>
)

/**
 * Domain entity representing an individual episode with multi-bitrate HLS streams.
 *
 * @property id Unique episode GUID.
 * @property ordinal Absolute episode ordinal (e.g. 1, 2, ..., 370, 500).
 * @property name Episode title string if provided by dubbing team.
 * @property durationSeconds Approximate duration in seconds.
 * @property hls1080 Direct 1080p (Full HD) HLS stream URL (`.m3u8`), or null if not encoded.
 * @property hls720 Direct 720p (HD) HLS stream URL (`.m3u8`), or null if not encoded.
 * @property hls480 Direct 480p (SD) HLS stream URL (`.m3u8`), or null if not encoded.
 */
data class AniLibriaEpisode(
    val id: String?,
    val ordinal: Int,
    val name: String?,
    val durationSeconds: Double?,
    val hls1080: String?,
    val hls720: String?,
    val hls480: String?
) {
    /**
     * Resolves the highest resolution HLS stream URL available for this episode.
     */
    val bestHlsUrl: String?
        get() = hls1080 ?: hls720 ?: hls480

    /**
     * Human-readable label of the highest available quality tier.
     */
    val bestQuality: String
        get() = when {
            hls1080 != null -> "1080p"
            hls720 != null -> "720p"
            hls480 != null -> "480p"
            else -> "720p"
        }

    /**
     * Real list of available [VideoQuality] variants dynamically built from non-null stream URLs.
     */
    val qualityVariants: List<VideoQuality>
        get() = listOfNotNull(
            hls1080?.let { VideoQuality("1080p", it) },
            hls720?.let { VideoQuality("720p", it) },
            hls480?.let { VideoQuality("480p", it) }
        )
}
