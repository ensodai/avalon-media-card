package org.ensodai.avalonmediacard.plugins.vk.domain.usecase

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.plugins.vk.domain.repository.VkRepository
import kotlin.uuid.Uuid

/**
 * Resolves the bound movie MediaStream for VK Video playback.
 */
class GetVkPlaylistUseCase(
    private val context: PluginContext,
    private val repository: VkRepository
) {
    suspend fun execute(
        key: MediaKey,
        sourceId: String?,
        userId: Uuid?
    ): List<MediaStream> {
        if (sourceId.isNullOrBlank()) return emptyList()

        val parts = sourceId.removePrefix("vk_movie_").split("_")
        val ownerId = parts.getOrNull(0)?.toLongOrNull() ?: return emptyList()
        val videoId = parts.getOrNull(1)?.toLongOrNull() ?: return emptyList()

        val video = repository.getVideo(ownerId, videoId) ?: return emptyList()
        val metadata = try {
            context.catalog.getMediaDetails(key)
        } catch (e: Exception) {
            null
        }

        val movieItem = if (userId != null) {
            try {
                context.userMovies.getUserMovies(userId).find { it.mediaId == key.id }
            } catch (e: Exception) {
                null
            }
        } else null

        val finalTitle = metadata?.title ?: video.title
        val format = if (video.directUrl.contains(".mp4")) "MP4" else "HLS"
        val streamType = if (video.directUrl.endsWith(".m3u8") || video.hlsUrl == video.directUrl) StreamType.Hls else StreamType.DirectUrl

        val stream = MediaStream(
            id = sourceId,
            title = finalTitle,
            url = video.directUrl,
            type = streamType,
            quality = video.bestQuality,
            format = format,
            videoCodec = "H.264",
            sourceName = "VK Video",
            durationSeconds = metadata?.runtime?.toDouble()?.let { it * 60 } ?: video.durationSeconds,
            isMapped = true,
            seasonNumber = null,
            episodeNumber = null,
            episodeName = finalTitle,
            episodePosterUrl = metadata?.posterUrl,
            watchedProgressSeconds = movieItem?.progressSeconds,
            isWatched = movieItem?.status == MediaStatus.COMPLETED,
            userRating = movieItem?.userRating,
            lastWatchedAtEpochMs = movieItem?.lastWatchedAt?.toEpochMilliseconds(),
            qualityVariants = video.qualities,
            subtitleTracks = video.subtitles
        )

        return listOf(stream)
    }
}
