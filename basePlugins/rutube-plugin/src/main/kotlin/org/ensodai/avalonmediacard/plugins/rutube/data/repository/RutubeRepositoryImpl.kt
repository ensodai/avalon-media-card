package org.ensodai.avalonmediacard.plugins.rutube.data.repository

import java.util.concurrent.ConcurrentHashMap
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.plugins.rutube.data.network.RutubeApiClient
import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeMappedEpisode
import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeStreamInfo
import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeVideoItem
import org.ensodai.avalonmediacard.plugins.rutube.domain.repository.RutubeRepository

/**
 * **Rutube Repository Implementation**
 *
 * Implements [RutubeRepository] by delegating to [RutubeApiClient] and mapping DTOs into domain models.
 *
 * @property apiClient The low-level Ktor HTTP client.
 */
class RutubeRepositoryImpl(
    private val apiClient: RutubeApiClient
) : RutubeRepository {

    private val streamInfoCache = ConcurrentHashMap<String, RutubeStreamInfo>()

    override suspend fun searchVideos(query: String, duration: String?, limit: Int, page: Int): List<RutubeVideoItem> {
        val dtos = apiClient.searchVideos(query = query, duration = duration, limit = limit, page = page)
        return mapVideos(dtos)
    }

    override suspend fun getAuthorVideos(authorId: String, limit: Int): List<RutubeVideoItem> {
        val dtos = apiClient.getAuthorVideos(authorId = authorId, limit = limit)
        return mapVideos(dtos)
    }

    private fun mapVideos(dtos: List<org.ensodai.avalonmediacard.plugins.rutube.data.network.dto.RutubeVideoDto>): List<RutubeVideoItem> {
        return dtos
            .filter { dto ->
                // Filter out non-playable, deleted, private, or adult-locked content
                !dto.isDeleted && !dto.isHidden && !dto.isLocked && !dto.isPaid && !dto.isAdult && !dto.isLivestream
            }
            .map { dto ->
                val poster = dto.pictureUrl ?: dto.thumbnailUrl
                RutubeVideoItem(
                    id = dto.id,
                    title = dto.title.trim(),
                    durationSeconds = dto.duration.toDouble(),
                    posterUrl = poster,
                    authorName = dto.author?.name,
                    authorId = dto.author?.id?.toString()
                )
            }
    }

    override suspend fun getStreamInfo(videoId: String): RutubeStreamInfo? {
        streamInfoCache[videoId]?.let { return it }

        val options = apiClient.getPlayOptions(videoId) ?: return null
        val balancerUrl = options.videoBalancer?.m3u8 ?: options.videoBalancer?.default ?: return null

        val masterText = apiClient.fetchMasterPlaylistText(balancerUrl)
        val qualities = if (!masterText.isNullOrBlank()) {
            parseMasterPlaylist(masterText)
        } else {
            emptyList()
        }

        val bestUrl = qualities.firstOrNull()?.url ?: balancerUrl

        val info = RutubeStreamInfo(
            videoId = videoId,
            title = options.title ?: "Rutube Stream",
            masterHlsUrl = bestUrl,
            qualities = qualities.ifEmpty { listOf(VideoQuality("1080p", balancerUrl)) }
        )
        streamInfoCache[videoId] = info
        return info
    }

    companion object {
        private val RESOLUTION_REGEX = Regex("""RESOLUTION=(\d+x\d+)""")
        private val BANDWIDTH_REGEX = Regex("""BANDWIDTH=(\d+)""")
    }

    private fun parseMasterPlaylist(playlistText: String): List<VideoQuality> {
        val lines = playlistText.lines()
        val variants = mutableListOf<Pair<Int, VideoQuality>>()
        val seenLabels = mutableSetOf<String>()

        var currentResolution: String? = null
        var currentBandwidth: Int? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
                currentResolution = RESOLUTION_REGEX.find(trimmed)?.groupValues?.get(1)
                currentBandwidth = BANDWIDTH_REGEX.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()
            } else if (trimmed.startsWith("http") && !trimmed.startsWith("#")) {
                val height = currentResolution?.substringAfter("x")?.toIntOrNull() ?: (currentBandwidth ?: 0)
                val width = currentResolution?.substringBefore("x")?.toIntOrNull() ?: 0

                val label = when {
                    height >= 1000 || width >= 1900 -> "1080p"
                    height >= 700 || width >= 1200 -> "720p"
                    height >= 400 || width >= 800 -> "480p"
                    height >= 300 || width >= 600 -> "360p"
                    else -> "${height}p"
                }

                // Avoid duplicate alternate CDN mirrors for the same resolution
                if (seenLabels.add(label)) {
                    val sortKey = when {
                        height >= 1000 || width >= 1900 -> 1080
                        height >= 700 || width >= 1200 -> 720
                        height >= 400 || width >= 800 -> 480
                        height >= 300 || width >= 600 -> 360
                        else -> height
                    }

                    variants.add(sortKey to VideoQuality(label = label, url = trimmed))
                }

                currentResolution = null
                currentBandwidth = null
            }
        }

        return variants.sortedByDescending { it.first }.map { it.second }
    }
}
