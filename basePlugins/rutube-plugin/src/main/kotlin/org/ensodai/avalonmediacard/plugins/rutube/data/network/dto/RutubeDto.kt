package org.ensodai.avalonmediacard.plugins.rutube.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root response from the Rutube search endpoint (`/api/search/video/`).
 */
@Serializable
data class RutubeSearchResponseDto(
    val results: List<RutubeVideoDto> = emptyList(),
    @SerialName("has_next") val hasNext: Boolean = false,
    @SerialName("next_page") val nextPage: String? = null
)

/**
 * Data Transfer Object representing an individual video item returned by Rutube search.
 */
@Serializable
data class RutubeVideoDto(
    val id: String,
    val title: String,
    val duration: Long = 0L, // Duration in seconds
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("picture_url") val pictureUrl: String? = null,
    @SerialName("is_adult") val isAdult: Boolean = false,
    @SerialName("is_locked") val isLocked: Boolean = false,
    @SerialName("is_paid") val isPaid: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_hidden") val isHidden: Boolean = false,
    @SerialName("is_livestream") val isLivestream: Boolean = false,
    @SerialName("tv_show_id") val tvShowId: Long? = null,
    val author: RutubeAuthorDto? = null,
    val category: RutubeCategoryDto? = null
)

/**
 * Author / Channel metadata for a Rutube video.
 */
@Serializable
data class RutubeAuthorDto(
    val id: Long? = null,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

/**
 * Category metadata for a Rutube video.
 */
@Serializable
data class RutubeCategoryDto(
    val id: Int? = null,
    val name: String? = null
)

/**
 * Response from Rutube video play options endpoint (`/api/play/options/{id}/`).
 */
@Serializable
data class RutubePlayOptionsDto(
    @SerialName("video_id") val videoId: String? = null,
    val title: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("video_balancer") val videoBalancer: RutubeVideoBalancerDto? = null
)

/**
 * Balancer containing master HLS stream URLs.
 */
@Serializable
data class RutubeVideoBalancerDto(
    val default: String? = null,
    val m3u8: String? = null
)
