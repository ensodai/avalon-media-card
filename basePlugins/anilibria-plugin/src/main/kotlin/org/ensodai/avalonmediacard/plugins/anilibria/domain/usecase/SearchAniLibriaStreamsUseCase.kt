package org.ensodai.avalonmediacard.plugins.anilibria.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.plugins.anilibria.domain.model.AniLibriaRelease
import org.ensodai.avalonmediacard.plugins.anilibria.domain.repository.AniLibriaRepository
import kotlin.uuid.Uuid

/**
 * **Search AniLibria Streams UseCase (Reference Implementation)**
 *
 * Discovers and builds high-level [MediaStream] source entries for the "Watch Online" UI drawer.
 *
 * ### Workflow:
 * 1. Resolves localized title and original title from the host catalog via [MediaKey].
 * 2. Queries AniLibria REST API across available titles (Russian, English/Romaji).
 * 3. Builds a preview [MediaStream] for each matching release with:
 *    - `providerId = "anilibria-plugin"` and `sourceId = release.id`
 *    - Direct HLS playback URL for the initial target episode
 *    - `clickAction = SelectMediaSourceCommand` for direct provider playback routing.
 *
 * @property context Host plugin context providing catalog, logger, and registries.
 * @property repository AniLibria domain repository.
 */
class SearchAniLibriaStreamsUseCase(
    private val context: PluginContext,
    private val repository: AniLibriaRepository
) {
    private val logger = context.logger

    /**
     * Executes the stream search and returns available release candidates.
     *
     * @param key Canonical [MediaKey] of the media resource (Movie or TV show).
     * @param season Optional target season number.
     * @param episode Optional target episode number.
     * @param userId Optional user UUID for personalized stream filtering.
     * @return List of discovered [MediaStream] candidates.
     */
    suspend fun execute(
        key: MediaKey,
        season: Int?,
        episode: Int?,
        userId: Uuid?
    ): List<MediaStream> {
        val targetSeason = season ?: 1
        val targetEpisode = episode ?: 1

        val metadata = try {
            context.catalog.getMediaDetails(key)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.warn("AniLibria: Failed to fetch TMDB details for key=$key: ${e.message}")
            return emptyList()
        }

        if (!metadata.isAnime) {
            return emptyList()
        }

        val searchTitles = listOfNotNull(metadata.title, metadata.originalTitle).distinct()
        logger.info("AniLibria: Searching anime for '${metadata.title}' (titles=$searchTitles) [S${targetSeason}E${targetEpisode}]")

        val foundReleases = mutableListOf<AniLibriaRelease>()

        for (query in searchTitles) {
            val results = repository.searchReleases(query)
            for (rel in results) {
                if (foundReleases.none { it.id == rel.id }) {
                    foundReleases.add(rel)
                }
            }
        }

        if (foundReleases.isEmpty()) {
            logger.info("AniLibria: No releases found for queries: $searchTitles")
            return emptyList()
        }

        val streams = mutableListOf<MediaStream>()
        val candidates = foundReleases.take(5)
        val detailsList = coroutineScope {
            candidates.map { release ->
                async { release to repository.getReleaseDetails(release.id) }
            }.awaitAll()
        }

        for ((release, details) in detailsList) {
            if (details == null) continue
            val ep = details.episodes.find { it.ordinal == targetEpisode } ?: details.episodes.firstOrNull()
            val hlsUrl = ep?.bestHlsUrl ?: continue

            val episodesCount = details.episodes.size
            val episodesTotal = release.episodesTotal ?: episodesCount
            val epSummary = when {
                episodesCount >= episodesTotal && episodesTotal > 0 -> context.i18n.t("episodes.all_fmt", episodesTotal)
                episodesCount > 0 -> context.i18n.t("episodes.count_of_total_fmt", episodesCount, episodesTotal)
                else -> context.i18n.t("episode.ordinal_fmt", ep.ordinal)
            }

            val stream = MediaStream(
                id = "anilibria_season_${release.id}",
                title = release.titleRu,
                url = hlsUrl,
                type = StreamType.Hls,
                quality = ep.bestQuality,
                format = "HLS",
                videoCodec = "H.264",
                sourceName = "AniLibria",
                durationSeconds = ep.durationSeconds,
                isMapped = true,
                seasonNumber = targetSeason,
                episodeNumber = null,
                episodesCount = episodesCount,
                episodesTotal = episodesTotal,
                episodeName = "AniLibria • $epSummary",
                qualityVariants = ep.qualityVariants
            )

            streams.add(stream)
        }

        logger.info("AniLibria: Found ${streams.size} streams for '${metadata.title}'")
        return streams
    }
}
