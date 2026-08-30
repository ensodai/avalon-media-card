package org.ensodai.avalonmediacard.plugins.vk.data.repository

import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.plugins.vk.data.network.VkApiClient
import org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkAlbumDto
import org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkVideoDto
import org.ensodai.avalonmediacard.plugins.vk.domain.model.VkAlbumItem
import org.ensodai.avalonmediacard.plugins.vk.domain.model.VkVideoItem
import org.ensodai.avalonmediacard.plugins.vk.domain.repository.VkRepository

class VkRepositoryImpl(
    private val apiClient: VkApiClient
) : VkRepository {

    override suspend fun search(query: String): List<VkVideoItem> {
        val dtos = apiClient.searchVideos(query)
        return dtos.mapNotNull { it.toDomain() }
    }

    override suspend fun searchAlbums(query: String): List<VkAlbumItem> {
        val dtos = apiClient.searchAlbums(query)
        return dtos.map { it.toDomain() }
    }

    override suspend fun getAlbumVideos(ownerId: Long, albumId: Long): List<VkVideoItem> {
        val dtos = apiClient.getAlbumVideos(ownerId, albumId)
        return dtos.mapNotNull { it.toDomain() }
    }

    override suspend fun getVideo(ownerId: Long, videoId: Long): VkVideoItem? {
        val dto = apiClient.getVideo(ownerId, videoId)
        return dto?.toDomain()
    }

    private fun VkAlbumDto.toDomain(): VkAlbumItem {
        val preview = this.image?.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url
        return VkAlbumItem(
            id = this.id,
            ownerId = this.ownerId,
            title = this.title.trim(),
            count = this.count,
            previewUrl = preview
        )
    }

    private fun VkVideoDto.toDomain(): VkVideoItem? {
        val files = this.files ?: return null

        val qualities = mutableListOf<VideoQuality>()
        fun addQuality(url: String?, label: String) {
            if (!url.isNullOrBlank()) {
                qualities.add(VideoQuality(label = label, url = url))
            }
        }

        addQuality(files.mp4_2160, "2160p (4K)")
        addQuality(files.mp4_1440, "1440p (2K)")
        addQuality(files.mp4_1080, "1080p")
        addQuality(files.mp4_720, "720p")
        addQuality(files.mp4_480, "480p")
        addQuality(files.mp4_360, "360p")
        addQuality(files.mp4_240, "240p")
        addQuality(files.mp4_144, "144p")

        val bestQuality = qualities.firstOrNull()?.label ?: "1080p"
        val directUrl = qualities.firstOrNull()?.url ?: files.hls ?: ""

        if (directUrl.isBlank() && qualities.isEmpty()) {
            return null
        }

        val subtitleTracks = this.subtitles?.mapNotNull { sub ->
            val url = sub.url
            if (url.isNullOrBlank()) return@mapNotNull null
            val name = sub.manifestName ?: sub.title ?: sub.lang ?: "Субтитры"
            SubtitleTrack(
                id = sub.lang ?: name,
                name = name,
                language = sub.lang,
                isExternal = true,
                url = url
            )
        } ?: emptyList()

        return VkVideoItem(
            id = this.id,
            ownerId = this.ownerId,
            title = this.title.trim(),
            description = this.description,
            durationSeconds = this.duration.toDouble(),
            qualities = qualities,
            bestQuality = bestQuality,
            directUrl = directUrl,
            hlsUrl = files.hls,
            subtitles = subtitleTracks,
            ownerName = this.ownerName
        )
    }
}
