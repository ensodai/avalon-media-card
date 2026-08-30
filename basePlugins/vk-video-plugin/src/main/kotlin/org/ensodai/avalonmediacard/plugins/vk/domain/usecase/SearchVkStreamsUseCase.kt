package org.ensodai.avalonmediacard.plugins.vk.domain.usecase

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.plugins.vk.domain.model.VkVideoItem
import org.ensodai.avalonmediacard.plugins.vk.domain.repository.VkRepository
import kotlin.math.abs
import kotlin.uuid.Uuid

/**
 * Executes high-precision discovery of full movie streams from VK Video.
 * Strictly limited to Movies (EntityType.MOVIE) to guarantee high relevance and zero UI clutter for TV series.
 */
class SearchVkStreamsUseCase(
    private val context: PluginContext,
    private val repository: VkRepository
) {
    private val logger = context.logger

    private val stopWords = listOf(
        "трейлер", "trailer", "тизер", "teaser", "клип", "clip", "обзор", "разбор",
        "реакция", "reaction", "факты", "сцены", "саундтрек", "ost", "shorts", "съемки",
        "фильм о фильме", "нарезка", "отрывок", "фрагмент", "сериал", "сезон", "серия"
    )

    suspend fun execute(
        key: MediaKey,
        season: Int?,
        episode: Int?,
        userId: Uuid?
    ): List<MediaStream> {
        // Strictly movies only: VK Video is completely disabled for TV and Anime series
        if (key.type != EntityType.MOVIE) {
            return emptyList()
        }

        val metadata = try {
            context.catalog.getMediaDetails(key)
        } catch (e: Exception) {
            logger.warn("VK Video: Failed to fetch metadata for $key: ${e.message}")
            null
        }

        val mainTitle = metadata?.title?.trim() ?: ""
        val origTitle = metadata?.originalTitle?.trim()
        val releaseYear = metadata?.releaseDate?.take(4)?.toIntOrNull()
        val durationMinutes = metadata?.runtime

        if (mainTitle.isBlank()) {
            return emptyList()
        }

        return searchMovies(key, mainTitle, origTitle, releaseYear, durationMinutes)
    }

    private suspend fun searchMovies(
        key: MediaKey,
        mainTitle: String,
        origTitle: String?,
        releaseYear: Int?,
        durationMinutes: Int?
    ): List<MediaStream> {
        val queries = mutableListOf<String>()
        if (releaseYear != null) {
            queries.add("$mainTitle $releaseYear")
        }
        queries.add(mainTitle)
        if (!origTitle.isNullOrBlank() && !origTitle.equals(mainTitle, ignoreCase = true)) {
            if (releaseYear != null) {
                queries.add("$origTitle $releaseYear")
            }
            queries.add(origTitle)
        }

        logger.info("VK Video: Searching movies for '$mainTitle' (queries=$queries)")

        val collectedVideos = mutableMapOf<String, VkVideoItem>()
        for (q in queries.distinct()) {
            val results = repository.search(q)
            for (item in results) {
                val uniqueKey = "${item.ownerId}_${item.id}"
                if (!collectedVideos.containsKey(uniqueKey)) {
                    collectedVideos[uniqueKey] = item
                }
            }
            if (collectedVideos.size >= 25) break
        }

        val targetRuntimeSec = durationMinutes?.let { it * 60 }
        val streams = mutableListOf<MediaStream>()

        for ((_, video) in collectedVideos) {
            val titleLower = video.title.lowercase()

            // Filter stop words
            if (stopWords.any { titleLower.contains(it) }) continue

            // Filter out explicit series titles
            if (titleLower.contains("сезон") || titleLower.contains("серия") || titleLower.contains("все серии")) continue

            // Runtime filter (min 45 min / 2700s, or +/- 20% of TMDB runtime)
            val duration = video.durationSeconds
            if (targetRuntimeSec != null && targetRuntimeSec > 0) {
                val diffRatio = abs(duration - targetRuntimeSec) / targetRuntimeSec.toDouble()
                if (diffRatio > 0.20 || duration < 2700) continue
            } else {
                if (duration < 3600) continue
            }

            val format = if (video.directUrl.contains(".mp4")) "MP4" else "HLS"
            val streamType = if (video.directUrl.endsWith(".m3u8") || video.hlsUrl == video.directUrl) StreamType.Hls else StreamType.DirectUrl
            val streamId = "vk_movie_${video.ownerId}_${video.id}"

            val channelSubtitle = video.ownerName?.let { "Канал «$it» • ${video.bestQuality}" } ?: "VK Video • ${video.bestQuality}"

            streams.add(
                MediaStream(
                    id = streamId,
                    title = video.title,
                    url = video.directUrl,
                    type = streamType,
                    quality = video.bestQuality,
                    format = format,
                    videoCodec = "H.264",
                    sourceName = "VK Video",
                    durationSeconds = video.durationSeconds,
                    isMapped = true,
                    episodeName = channelSubtitle,
                    qualityVariants = video.qualities,
                    subtitleTracks = video.subtitles
                )
            )
        }

        logger.info("VK Video: Found ${streams.size} movie streams for '$mainTitle'")
        return streams
    }
}
