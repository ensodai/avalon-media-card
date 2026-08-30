package org.ensodai.avalonmediacard.plugins.anilibria.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object representing a release item in the AniLibria search results list.
 */
@Serializable
data class AniLibriaSearchReleaseDto(
    val id: Long,
    val year: Int? = null,
    val name: AniLibriaNameDto? = null,
    val alias: String? = null,
    @SerialName("episodes_total") val episodesTotal: Int? = null,
    val poster: AniLibriaPosterDto? = null
)

/**
 * Multi-language title information from AniLibria.
 */
@Serializable
data class AniLibriaNameDto(
    val main: String? = null,
    val english: String? = null,
    val alternative: String? = null
)

/**
 * Poster image variants provided by AniLibria CDN.
 */
@Serializable
data class AniLibriaPosterDto(
    val src: String? = null,
    val preview: String? = null,
    val thumbnail: String? = null
)

/**
 * Data Transfer Object representing full details and episode list of a specific release.
 */
@Serializable
data class AniLibriaReleaseDetailsDto(
    val id: Long,
    val year: Int? = null,
    val name: AniLibriaNameDto? = null,
    val alias: String? = null,
    val episodes: List<AniLibriaEpisodeDto> = emptyList()
)

/**
 * Data Transfer Object representing an individual episode with its multi-quality HLS stream URLs.
 */
@Serializable
data class AniLibriaEpisodeDto(
    val id: String? = null,
    val name: String? = null,
    val ordinal: Int,
    val duration: Double? = null,
    @SerialName("hls_1080") val hls1080: String? = null,
    @SerialName("hls_720") val hls720: String? = null,
    @SerialName("hls_480") val hls480: String? = null
)
