package org.ensodai.avalonmediacard.plugins.rutube.domain.usecase

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher
import org.ensodai.avalonmediacard.contract.parsers.MappingResult
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.SourceMapping
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeMappedEpisode
import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeVideoItem
import org.ensodai.avalonmediacard.plugins.rutube.domain.repository.RutubeRepository
import kotlin.uuid.Uuid

/**
 * **Get Rutube Playlist UseCase**
 *
 * Constructs the playback playlist of [MediaStream] items for movie or TV show playback from Rutube.
 *
 * Supports:
 * - Direct single movies
 * - Targeted single episodes (`rutube_ep_...`)
 * - Complete season playlists (`rutube_season_...`) enriched with TMDB titles, stills, and watch progress.
 *
 * @property context Host plugin context providing catalog, logger, and user storage.
 * @property repository Rutube domain repository.
 */
class GetRutubePlaylistUseCase(
    private val context: PluginContext,
    private val repository: RutubeRepository
) {
    private val episodeMatcher = EpisodeMatcher()

    /**
     * Executes playlist generation for the given media key and source identifier.
     *
     * @param key Canonical [MediaKey] of the media.
     * @param sourceId The Rutube video or season group identifier.
     * @param userId Optional user UUID for attaching watch progress history.
     * @return List of enriched [MediaStream] items ready for playback.
     */
    suspend fun execute(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger
        val isMovie = key.type == EntityType.MOVIE

        if (isMovie) {
            return buildMoviePlaylist(key, sourceId, userId)
        }

        return if (sourceId.startsWith("rutube_season_")) {
            buildSeasonPlaylist(key, sourceId, userId)
        } else {
            buildSingleEpisodePlaylist(key, sourceId, userId)
        }
    }

    private suspend fun buildMoviePlaylist(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val videoId = sourceId.removePrefix("rutube_")
        val streamInfo = repository.getStreamInfo(videoId) ?: return emptyList()

        val userLang = if (userId != null) {
            try {
                context.userGlobalSettings.getUserSettings(userId)?.uiLocale?.ifBlank { "ru" } ?: "ru"
            } catch (_: Exception) {
                "ru"
            }
        } else "ru"

        val metadata = try {
            context.catalog.getMediaDetails(key, language = userLang)
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

        val finalTitle = metadata?.title ?: streamInfo.title
        val bestQuality = streamInfo.qualities.firstOrNull()?.label ?: "1080p"

        val stream = MediaStream(
            id = "rutube_$videoId",
            title = finalTitle,
            url = streamInfo.masterHlsUrl,
            type = StreamType.Hls,
            quality = bestQuality,
            format = "HLS",
            videoCodec = "H.264",
            sourceName = "Rutube",
            durationSeconds = metadata?.runtime?.toDouble()?.let { it * 60 },
            isMapped = true,
            seasonNumber = null,
            episodeNumber = null,
            episodeName = finalTitle,
            episodePosterUrl = metadata?.posterUrl,
            watchedProgressSeconds = movieItem?.progressSeconds,
            isWatched = movieItem?.status == MediaStatus.COMPLETED,
            userRating = movieItem?.userRating,
            lastWatchedAtEpochMs = movieItem?.lastWatchedAt?.toEpochMilliseconds(),
            qualityVariants = streamInfo.qualities
        )

        return listOf(stream)
    }

    private suspend fun buildSingleEpisodePlaylist(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val videoId = sourceId.removePrefix("rutube_ep_").removePrefix("rutube_")
        val streamInfo = repository.getStreamInfo(videoId) ?: return emptyList()

        val userLang = if (userId != null) {
            try {
                context.userGlobalSettings.getUserSettings(userId)?.uiLocale?.ifBlank { "ru" } ?: "ru"
            } catch (_: Exception) {
                "ru"
            }
        } else "ru"

        val metadata = try {
            context.catalog.getMediaDetails(key, language = userLang)
        } catch (e: Exception) {
            null
        }

        val progressHistory = if (userId != null) {
            try {
                context.userEpisodes.getEpisodesProgress(userId, key.id)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val bestQuality = streamInfo.qualities.firstOrNull()?.label ?: "1080p"

        val stream = MediaStream(
            id = "rutube_ep_$videoId",
            title = if (metadata?.title?.isNotBlank() == true) "${metadata.title} • ${streamInfo.title}" else streamInfo.title,
            url = streamInfo.masterHlsUrl,
            type = StreamType.Hls,
            quality = bestQuality,
            format = "HLS",
            videoCodec = "H.264",
            sourceName = "Rutube",
            durationSeconds = metadata?.runtime?.toDouble()?.let { it * 60 },
            isMapped = true,
            seasonNumber = 1,
            episodeNumber = 1,
            episodeName = streamInfo.title,
            episodePosterUrl = metadata?.posterUrl,
            watchedProgressSeconds = progressHistory.firstOrNull()?.progressSeconds,
            isWatched = progressHistory.firstOrNull()?.isWatched ?: false,
            userRating = progressHistory.firstOrNull()?.userRating,
            lastWatchedAtEpochMs = progressHistory.firstOrNull()?.lastWatchedAtEpochMs,
            qualityVariants = streamInfo.qualities
        )

        return listOf(stream)
    }

    private suspend fun buildSeasonPlaylist(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger
        // 1. Check persistent database mappings for zero-delay instant response
        val dbMappings = context.sourceMappings.getMappings(key.id, sourceId)
        val seasonNum = dbMappings.firstOrNull()?.seasons?.firstOrNull()
            ?: Regex("""_(\d+)_(?:author|composite)""").find(sourceId)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""rutube_season_(\d+)""").find(sourceId)?.groupValues?.get(1)?.toIntOrNull()
            ?: 1

        val userLang = if (userId != null) {
            try {
                context.userGlobalSettings.getUserSettings(userId)?.uiLocale?.ifBlank { "ru" } ?: "ru"
            } catch (_: Exception) {
                "ru"
            }
        } else "ru"

        val metadata = try {
            context.catalog.getMediaDetails(key, language = userLang)
        } catch (e: Exception) {
            null
        }

        if (dbMappings.isNotEmpty()) {
            return buildPlaylistFromDbMappings(key, sourceId, dbMappings, seasonNum, userId, metadata)
        }

        val mainTitle = metadata?.title ?: ""
        val originalTitle = metadata?.originalTitle

        // 2. Fallback: Search Rutube for season videos if not yet in database
        val queries = mutableListOf<String>()
        if (mainTitle.isNotBlank()) {
            queries.add("$mainTitle $seasonNum сезон")
            queries.add("$mainTitle")
        }
        if (!originalTitle.isNullOrBlank() && originalTitle != mainTitle) {
            queries.add("$originalTitle Season $seasonNum")
        }

        val discoveredVideos = mutableListOf<RutubeVideoItem>()
        val seenIds = mutableSetOf<String>()

        for (q in queries.distinct()) {
            val results = repository.searchVideos(q, duration = null)
            for (item in results) {
                if (seenIds.add(item.id) && item.durationSeconds >= 300.0) {
                    discoveredVideos.add(item)
                }
            }
        }

        val mappedList = mutableListOf<RutubeMappedEpisode>()
        for (item in discoveredVideos) {
            val match = episodeMatcher.parse(mainTitle, item.title)
            when (match) {
                is MappingResult.Success -> {
                    val s = match.seasons.firstOrNull() ?: seasonNum
                    val e = match.episodes.firstOrNull() ?: continue
                    if (s == seasonNum) mappedList.add(RutubeMappedEpisode(item, s, e))
                }
                is MappingResult.Partial -> {
                    val e = match.episodes.firstOrNull() ?: continue
                    mappedList.add(RutubeMappedEpisode(item, seasonNum, e))
                }
                is MappingResult.Failed -> {
                    val seasonMatch = Regex("""(?<!\d)(\d{1,2})\s*(?:сезон|season|s)""", RegexOption.IGNORE_CASE).find(item.title)
                    val epMatch = Regex("""(?<!\d)(\d{1,3})\s*(?:серия|сер|эпизод|ep|e)""", RegexOption.IGNORE_CASE).find(item.title)
                    val s = seasonMatch?.groupValues?.get(1)?.toIntOrNull() ?: seasonNum
                    val e = epMatch?.groupValues?.get(1)?.toIntOrNull()
                    if (s == seasonNum && e != null) {
                        mappedList.add(RutubeMappedEpisode(item, s, e))
                    }
                }
            }
        }

        val authorHash = Regex("""_author_(-?\d+)""").find(sourceId)?.groupValues?.get(1)?.toIntOrNull()
        val filteredMappedList = if (authorHash != null) {
            val authorEpisodes = mappedList.filter { it.video.authorName?.hashCode() == authorHash }
            if (authorEpisodes.isNotEmpty()) authorEpisodes else mappedList
        } else {
            mappedList
        }

        val distinctSeasonEpisodes = filteredMappedList
            .distinctBy { it.episode }
            .sortedBy { it.episode }

        if (distinctSeasonEpisodes.isEmpty()) {
            logger.warn("Rutube: No episodes discovered for season $seasonNum")
            return emptyList()
        }

        val sourceMappingsToSave = distinctSeasonEpisodes.map { ep ->
            SourceMapping(
                sourceType = "rutube-plugin",
                sourceId = sourceId,
                itemKey = ep.video.id,
                seasons = listOf(seasonNum),
                episodes = listOf(ep.episode),
                mediaId = key.id,
                streamUrl = null,
                quality = "1080p"
            )
        }
        context.sourceMappings.saveMappingsBatch(sourceMappingsToSave)

        return buildPlaylistFromDbMappings(key, sourceId, sourceMappingsToSave, seasonNum, userId, metadata)
    }

    private suspend fun buildPlaylistFromDbMappings(
        key: MediaKey,
        sourceId: String,
        dbMappings: List<SourceMapping>,
        seasonNum: Int,
        userId: Uuid?,
        metadata: org.ensodai.avalonmediacard.contract.model.MediaMetadata?
    ): List<MediaStream> {
        val logger = context.logger
        val userLang = if (userId != null) {
            try {
                context.userGlobalSettings.getUserSettings(userId)?.uiLocale?.ifBlank { "ru" } ?: "ru"
            } catch (_: Exception) {
                "ru"
            }
        } else "ru"

        val tmdbEpisodes = try {
            context.catalog.getSeasonDetails(key, seasonNum, language = userLang)
        } catch (e: Exception) {
            emptyList()
        }

        val progressList = if (userId != null) {
            try {
                context.userEpisodes.getEpisodesProgress(userId, key.id)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val sortedMappings = dbMappings
            .distinctBy { it.episodes?.firstOrNull() ?: it.itemKey }
            .sortedBy { it.episodes?.firstOrNull() ?: 999 }

        val playlist = sortedMappings.map { mapping ->
            val epNum = mapping.episodes?.firstOrNull() ?: 1
            val tmdbEp = tmdbEpisodes.find { it.episodeNumber == epNum }
            val progress = progressList.find { it.season == seasonNum && it.episode == epNum }

            val episodeName = tmdbEp?.name?.ifBlank { null }
                ?: context.i18n.tForLocale(userLang, "rutube.episode_title_fmt", epNum)
            val finalTitle = if (metadata?.title?.isNotBlank() == true) {
                "${metadata.title} • S${seasonNum}E${epNum} «$episodeName»"
            } else {
                "S${seasonNum}E${epNum} «$episodeName»"
            }
            val videoId = mapping.itemKey
            val epDuration = (tmdbEp?.runtime ?: metadata?.runtime)?.toDouble()?.let { it * 60 }

            MediaStream(
                id = "rutube_ep_$videoId",
                title = finalTitle,
                url = mapping.streamUrl ?: "",
                type = StreamType.Hls,
                quality = mapping.quality ?: "1080p",
                format = "HLS",
                videoCodec = "H.264",
                sourceName = "Rutube",
                durationSeconds = epDuration,
                isMapped = true,
                seasonNumber = seasonNum,
                episodeNumber = epNum,
                episodeName = episodeName,
                episodePosterUrl = tmdbEp?.stillUrl ?: metadata?.posterUrl,
                watchedProgressSeconds = progress?.progressSeconds,
                isWatched = progress?.isWatched ?: false,
                userRating = progress?.userRating,
                lastWatchedAtEpochMs = progress?.lastWatchedAtEpochMs,
                qualityVariants = emptyList()
            )
        }

        logger.info("Rutube: Successfully assembled instant database playlist of ${playlist.size} episodes for season $seasonNum")
        return playlist
    }
}
