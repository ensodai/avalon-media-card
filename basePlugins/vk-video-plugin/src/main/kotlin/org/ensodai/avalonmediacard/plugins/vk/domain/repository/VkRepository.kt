package org.ensodai.avalonmediacard.plugins.vk.domain.repository

import org.ensodai.avalonmediacard.plugins.vk.domain.model.VkAlbumItem
import org.ensodai.avalonmediacard.plugins.vk.domain.model.VkVideoItem

/**
 * Repository interface for VK Video operations.
 */
interface VkRepository {
    /**
     * Searches for videos by keyword query.
     */
    suspend fun search(query: String): List<VkVideoItem>

    /**
     * Searches for playlists/albums by keyword query.
     */
    suspend fun searchAlbums(query: String): List<VkAlbumItem>

    /**
     * Fetches all video items belonging to an album/playlist.
     */
    suspend fun getAlbumVideos(ownerId: Long, albumId: Long): List<VkVideoItem>

    /**
     * Fetches a single video by owner and video ID.
     */
    suspend fun getVideo(ownerId: Long, videoId: Long): VkVideoItem?
}
