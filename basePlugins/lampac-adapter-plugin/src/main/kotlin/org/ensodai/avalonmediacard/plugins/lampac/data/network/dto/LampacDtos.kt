package org.ensodai.avalonmediacard.plugins.lampac.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Balancer discovery item returned by `/lite/events`.
 */
@Serializable
data class LampacBalancerDto(
    val name: String,
    val url: String,
    val balanser: String
)

/**
 * Voiceover / translation item returned by Lampac balancers.
 */
@Serializable
data class LampacVoiceDto(
    val id: String? = null,
    val name: String? = null,
    val active: Boolean = false
)

/**
 * Subtitle track descriptor.
 */
@Serializable
data class LampacSubtitleDto(
    val label: String,
    val url: String
)

/**
 * Nested season, episode, or similar item within [LampacResponseDto.data].
 */
private val SEASON_REGEX = Regex("""(\d+)\s*сезон""", RegexOption.IGNORE_CASE)

@Serializable
data class LampacItemDto(
    val id: String? = null,
    val s: Int? = null,
    val season: Int? = null,
    val e: Int? = null,
    val episode: Int? = null,
    val name: String? = null,
    val title: String? = null,
    val translate: String? = null,
    val maxquality: String? = null,
    val url: String? = null,
    val stream: String? = null,
    val method: String? = null,
    val quality: Map<String, String>? = null,
    val subtitles: List<LampacSubtitleDto>? = null,
    val voice: List<LampacVoiceDto>? = null,
    val headers: Map<String, String>? = null,
    val year: Int? = null,
    val details: String? = null,
    val img: String? = null,
    val similar: Boolean = false
) {
    val seasonNumber: Int?
        get() = s?.takeIf { it > 0 }
            ?: season?.takeIf { it > 0 }
            ?: id?.toIntOrNull()?.takeIf { it > 0 }
            ?: SEASON_REGEX.find(name ?: "")?.groupValues?.get(1)?.toIntOrNull()
            ?: SEASON_REGEX.find(title ?: "")?.groupValues?.get(1)?.toIntOrNull()
            ?: if (id == "-1" || s == -1 || season == -1) 1 else null

    val episodeNumber: Int?
        get() = e ?: episode
}

/**
 * Universal Lampac response structure when requested with `?rjson=true`.
 */
@Serializable
data class LampacResponseDto(
    val type: String? = null,
    val method: String? = null,
    val title: String? = null,
    val translate: String? = null,
    val maxquality: String? = null,
    val quality: Map<String, String>? = null,
    val subtitles: List<LampacSubtitleDto>? = null,
    val headers: Map<String, String>? = null,
    val data: List<LampacItemDto>? = null,
    val voice: List<LampacVoiceDto>? = null,
    val url: String? = null,
    val stream: String? = null,
    @SerialName("hls_manifest_timeout")
    val hlsManifestTimeout: Long? = null
)

/**
 * Torrent item returned by JacRed aggregator (`/api/v2/torrents`).
 */
@Serializable
data class JacRedTorrentDto(
    val tracker: String,
    val title: String,
    val size: Long = 0L,
    val sizeName: String? = null,
    val seeders: Int = 0,
    val peers: Int = 0,
    val magnet: String? = null,
    val url: String? = null,
    val details: String? = null
)
