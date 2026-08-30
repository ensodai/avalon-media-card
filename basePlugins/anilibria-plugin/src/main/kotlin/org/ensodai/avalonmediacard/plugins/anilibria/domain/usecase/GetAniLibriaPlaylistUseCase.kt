package org.ensodai.avalonmediacard.plugins.anilibria.domain.usecase

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.plugins.anilibria.domain.model.AniLibriaReleaseDetails
import org.ensodai.avalonmediacard.plugins.anilibria.domain.repository.AniLibriaRepository
import kotlin.uuid.Uuid

/**
 * **Get AniLibria Playlist UseCase (Reference Implementation)**
 *
 * Constructs the complete playback playlist of [MediaStream] items for the video player.
 *
 * ### Key Responsibilities:
 * 1. **Release Resolution**: Fetches release details from AniLibria API using `sourceId` or title matching.
 * 2. **TMDB Season & Episode Resolution**:
 *    - Maps continuous absolute episode numbering (e.g. Naruto 370..500) into relative TMDB seasons and episodes.
 *    - Uses cumulative season episode counts with lazy catalog validation if metadata cache is incomplete.
 *    - Employs dual-matching: by `episodeNumber` and fallback to 0-based list index.
 * 3. **Stream Enrichment**:
 *    - Attaches episode title and still image URLs from TMDB metadata.
 *    - Attaches all available [org.ensodai.avalonmediacard.contract.plugins.VideoQuality] tiers (`1080p`, `720p`, `480p`).
 * 4. **User Progress Integration**:
 *    - Matches and attaches user watch progress, ratings, and completion status.
 *
 * @property context Host plugin context providing catalog, logger, and user storage.
 * @property repository AniLibria domain repository.
 */
class GetAniLibriaPlaylistUseCase(
    private val context: PluginContext,
    private val repository: AniLibriaRepository
) {
    /**
     * Executes playlist generation for the given media and release identifier.
     *
     * @param key The canonical [MediaKey] of the movie or series.
     * @param sourceId The release identifier (e.g. `"413"` or `"anilibria_413"`).
     * @param userId Optional user UUID for fetching personalized watch progress.
     * @return List of enriched [MediaStream] items ready for playback.
     */
    suspend fun execute(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger
        val releaseId = sourceId.removePrefix("anilibria_").toLongOrNull() ?: sourceId.toLongOrNull()

        val details: AniLibriaReleaseDetails? = if (releaseId != null) {
            repository.getReleaseDetails(releaseId)
        } else {
            val metadata = try {
                context.catalog.getMediaDetails(key)
            } catch (e: Exception) {
                null
            }
            val searchTitles = listOfNotNull(metadata?.title, metadata?.originalTitle).distinct()
            var matched: AniLibriaReleaseDetails? = null
            for (query in searchTitles) {
                val releases = repository.searchReleases(query)
                if (releases.isNotEmpty()) {
                    matched = repository.getReleaseDetails(releases.first().id)
                    if (matched != null) break
                }
            }
            matched
        }

        if (details == null) {
            logger.warn("AniLibria: Failed to retrieve release details for key=$key, sourceId=$sourceId")
            return emptyList()
        }

        // 1. Fetch TMDB Metadata & Season Structure
        val tvKey = if (key.type == EntityType.MOVIE && details.episodes.size > 1) {
            key.copy(type = EntityType.TV)
        } else {
            key
        }

        val mediaMetadata = try {
            context.catalog.getMediaDetails(tvKey)
        } catch (e: Exception) {
            null
        }

        val tmdbSeasons = mediaMetadata?.seasons?.filter { it.seasonNumber > 0 }?.sortedBy { it.seasonNumber } ?: emptyList()
        val seasonCache = mutableMapOf<Int, List<EpisodeItem>>()

        /**
         * Resolves the corresponding TMDB season number and EpisodeItem for a given absolute episode ordinal.
         *
         * Algorithm:
         * 1. If 1-season show -> direct match in Season 1.
         * 2. If multi-season continuous show -> calculates cumulative season offset ranges.
         * 3. Dynamically validates season size via `catalog.getSeasonDetails` if cached count is missing or zero.
         */
        suspend fun resolveTmdbEpisode(ordinal: Int): Pair<Int, EpisodeItem?> {
            if (tmdbSeasons.isEmpty()) {
                val s1Episodes = seasonCache.getOrPut(1) {
                    try { context.catalog.getSeasonDetails(tvKey, 1) } catch (e: Exception) { emptyList() }
                }
                return 1 to (s1Episodes.find { it.episodeNumber == ordinal } ?: s1Episodes.getOrNull(ordinal - 1))
            }

            // Check if Season 1 contains this episode directly (for 1-season shows)
            if (tmdbSeasons.size == 1) {
                val s1Episodes = seasonCache.getOrPut(tmdbSeasons.first().seasonNumber) {
                    try { context.catalog.getSeasonDetails(tvKey, tmdbSeasons.first().seasonNumber) } catch (e: Exception) { emptyList() }
                }
                val directMatch = s1Episodes.find { it.episodeNumber == ordinal } ?: s1Episodes.getOrNull(ordinal - 1)
                return tmdbSeasons.first().seasonNumber to directMatch
            }

            // For multi-season anime with absolute episode numbering (e.g. Naruto, Bleach, One Piece):
            var accumulated = 0
            for (season in tmdbSeasons) {
                val seasonEpisodes = if (season.episodeCount > 0 && ordinal > (accumulated + season.episodeCount)) {
                    null
                } else {
                    seasonCache.getOrPut(season.seasonNumber) {
                        try { context.catalog.getSeasonDetails(tvKey, season.seasonNumber) } catch (e: Exception) { emptyList() }
                    }
                }

                val seasonEpisodesCount = if (seasonEpisodes != null && seasonEpisodes.isNotEmpty()) {
                    seasonEpisodes.size
                } else {
                    season.episodeCount
                }

                if (seasonEpisodesCount <= 0) continue

                if (ordinal in (accumulated + 1)..(accumulated + seasonEpisodesCount)) {
                    val actualEpisodes = seasonEpisodes ?: seasonCache.getOrPut(season.seasonNumber) {
                        try { context.catalog.getSeasonDetails(tvKey, season.seasonNumber) } catch (e: Exception) { emptyList() }
                    }
                    val relativeEpisodeNumber = ordinal - accumulated
                    val found = actualEpisodes.find { it.episodeNumber == relativeEpisodeNumber }
                        ?: actualEpisodes.getOrNull(relativeEpisodeNumber - 1)
                    return season.seasonNumber to found
                }
                accumulated += seasonEpisodesCount
            }

            return 1 to null
        }

        // 2. Fetch User Progress History
        val progressHistory = if (userId != null) {
            try {
                context.userEpisodes.getEpisodesProgress(userId, key.id)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return details.episodes.mapNotNull { ep ->
            val hlsUrl = ep.bestHlsUrl ?: return@mapNotNull null
            val (resolvedSeason, tmdbEp) = resolveTmdbEpisode(ep.ordinal)

            val progress = progressHistory.find {
                (it.season == resolvedSeason && it.episode == (tmdbEp?.episodeNumber ?: ep.ordinal)) ||
                (it.season == 1 && it.episode == ep.ordinal)
            }

            val finalEpisodeName = tmdbEp?.name?.takeIf { it.isNotBlank() } ?: ep.name?.takeIf { it.isNotBlank() } ?: context.i18n.t("episode.ordinal_fmt", ep.ordinal)
            val finalDuration = tmdbEp?.runtime?.toDouble()?.let { it * 60 } ?: ep.durationSeconds

            MediaStream(
                id = "s${resolvedSeason}e${tmdbEp?.episodeNumber ?: ep.ordinal}",
                title = "${details.titleRu} • $finalEpisodeName",
                url = hlsUrl,
                type = StreamType.Hls,
                quality = ep.bestQuality,
                format = "HLS",
                videoCodec = "H.264",
                sourceName = "AniLibria",
                durationSeconds = finalDuration,
                isMapped = true,
                seasonNumber = resolvedSeason,
                episodeNumber = tmdbEp?.episodeNumber ?: ep.ordinal,
                episodeName = finalEpisodeName,
                episodePosterUrl = tmdbEp?.stillUrl,
                watchedProgressSeconds = progress?.progressSeconds,
                isWatched = progress?.isWatched ?: false,
                userRating = progress?.userRating,
                lastWatchedAtEpochMs = progress?.lastWatchedAtEpochMs,
                qualityVariants = ep.qualityVariants
            )
        }
    }
}
