package org.ensodai.avalonmediacard.plugins.vk.domain.model

import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality

/**
 * Domain representation of a VK video item with extracted multi-quality stream URLs.
 */
data class VkVideoItem(
    val id: Long,
    val ownerId: Long,
    val title: String,
    val description: String?,
    val durationSeconds: Double,
    val qualities: List<VideoQuality>,
    val bestQuality: String,
    val directUrl: String,
    val hlsUrl: String?,
    val subtitles: List<SubtitleTrack>,
    val ownerName: String?
)

/**
 * Matched episode for TV series parsing.
 */
data class VkEpisodeMatch(
    val video: VkVideoItem,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val rawTitle: String
)

/**
 * Domain representation of a VK Video album / playlist.
 */
data class VkAlbumItem(
    val id: Long,
    val ownerId: Long,
    val title: String,
    val count: Int,
    val previewUrl: String? = null
)
