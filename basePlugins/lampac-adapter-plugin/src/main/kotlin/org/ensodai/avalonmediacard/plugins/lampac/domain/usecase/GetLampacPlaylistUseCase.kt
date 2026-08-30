package org.ensodai.avalonmediacard.plugins.lampac.domain.usecase

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher
import org.ensodai.avalonmediacard.contract.parsers.HlsPlaylistParser
import org.ensodai.avalonmediacard.contract.parsers.MappingResult
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.SourceMapping
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.plugins.lampac.domain.model.LampacSourceDescriptor
import org.ensodai.avalonmediacard.plugins.lampac.domain.repository.LampacRepository
import kotlin.uuid.Uuid

/**
 * **Get Lampac Playlist UseCase**
 *
 * Constructs the playback playlist of [MediaStream] items for movie or TV show playback from Lampac.
 *
 * Supports:
 * - Direct single movies with audio track options
 * - Complete season playlists mapped 1-to-1 with TMDB metadata, stills, and user progress.
 */
class GetLampacPlaylistUseCase(
    private val context: PluginContext,
    private val repository: LampacRepository
) {
    private val episodeMatcher = EpisodeMatcher()

    suspend fun execute(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val isMovie = key.type == EntityType.MOVIE

        if (isMovie || sourceId.startsWith("lampac_movie_")) {
            return buildMoviePlaylist(key, sourceId, userId)
        }

        return buildSeasonPlaylist(key, sourceId, userId)
    }

    private suspend fun buildMoviePlaylist(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger
        val balancer = sourceId.removePrefix("lampac_movie_").substringBefore("_")

        val metadata = try {
            context.catalog.getMediaDetails(key)
        } catch (e: Exception) {
            null
        }

        val title = metadata?.title ?: ""
        val originalTitle = metadata?.originalTitle
        val year = metadata?.releaseDate?.take(4)?.toIntOrNull()
        val tmdbId = key.id.substringAfterLast(":").toLongOrNull()
        val imdbId = metadata?.imdbId

        val streamInfos = repository.getMovieStreams(
            balancer = balancer,
            title = title,
            originalTitle = originalTitle,
            year = year,
            tmdbId = tmdbId,
            imdbId = imdbId
        )

        if (streamInfos.isEmpty()) return emptyList()
        val defaultStream = streamInfos.first()

        val combinedAudioTracks = if (streamInfos.size > 1) {
            streamInfos.mapIndexed { idx, info ->
                AudioTrack(
                    id = "$idx",
                    name = info.translation ?: "$balancer #${idx + 1}",
                    isDefault = idx == 0,
                    url = info.streamUrl
                )
            }
        } else {
            defaultStream.audioTracks
        }

        val movieItem = if (userId != null) {
            try {
                context.userMovies.getUserMovies(userId).find { it.mediaId == key.id }
            } catch (e: Exception) {
                null
            }
        } else null

        val finalTitle = metadata?.title ?: defaultStream.title
        val bestQuality = defaultStream.qualities.firstOrNull()?.label ?: "1080p"
        val isDirect = defaultStream.streamUrl.contains(".mp4", ignoreCase = true)
        val streamType = if (isDirect) StreamType.DirectUrl else StreamType.Hls
        val streamFormat = if (isDirect) "MP4" else "HLS"

        val stream = MediaStream(
            id = sourceId,
            title = finalTitle,
            url = defaultStream.streamUrl,
            type = streamType,
            quality = bestQuality,
            format = streamFormat,
            videoCodec = "H.264",
            sourceName = "Lampac ($balancer)",
            durationSeconds = metadata?.runtime?.toDouble()?.let { it * 60 },
            isMapped = true,
            seasonNumber = null,
            episodeNumber = null,
            episodeName = defaultStream.translation ?: finalTitle,
            episodePosterUrl = metadata?.posterUrl,
            watchedProgressSeconds = movieItem?.progressSeconds,
            isWatched = movieItem?.status == MediaStatus.COMPLETED,
            userRating = movieItem?.userRating,
            lastWatchedAtEpochMs = movieItem?.lastWatchedAt?.toEpochMilliseconds(),
            audioTracks = combinedAudioTracks,
            qualityVariants = defaultStream.qualities,
            subtitleTracks = defaultStream.subtitles
        )

        return listOf(stream)
    }

    private suspend fun resolveTmdbEpisode(
        key: MediaKey,
        metadata: MediaMetadata?,
        seasonCache: MutableMap<Int, List<EpisodeItem>>,
        requestedSeason: Int,
        rawEpNum: Int,
        rawName: String
    ): Pair<Int, Pair<Int, EpisodeItem?>> {
        val tmdbSeasons = metadata?.seasons?.filter { it.seasonNumber > 0 }?.sortedBy { it.seasonNumber } ?: emptyList()

        suspend fun getSeasonEpisodes(sNum: Int): List<EpisodeItem> {
            return seasonCache.getOrPut(sNum) {
                try {
                    context.catalog.getSeasonDetails(key, sNum)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        val match = episodeMatcher.parse("", rawName)
        val parsedEpNum = when (match) {
            is MappingResult.Success -> match.episodes.firstOrNull() ?: rawEpNum
            is MappingResult.Partial -> match.episodes.firstOrNull() ?: rawEpNum
            is MappingResult.Failed -> rawEpNum
        }

        if (tmdbSeasons.isEmpty()) {
            val sEpisodes = getSeasonEpisodes(requestedSeason)
            val ep = sEpisodes.find { it.episodeNumber == parsedEpNum } ?: sEpisodes.getOrNull(parsedEpNum - 1)
            return requestedSeason to (parsedEpNum to ep)
        }

        if (tmdbSeasons.size == 1) {
            val sEpisodes = getSeasonEpisodes(tmdbSeasons.first().seasonNumber)
            val directMatch = sEpisodes.find { it.episodeNumber == parsedEpNum } ?: sEpisodes.getOrNull(parsedEpNum - 1)
            return tmdbSeasons.first().seasonNumber to (parsedEpNum to directMatch)
        }

        // Multi-season with explicit season request (> 1) and local episode index
        val targetSeasonMeta = tmdbSeasons.find { it.seasonNumber == requestedSeason }
        val isExplicitMultiSeason = requestedSeason > 1 && targetSeasonMeta != null && parsedEpNum <= targetSeasonMeta.episodeCount
        if (isExplicitMultiSeason) {
            val sEpisodes = getSeasonEpisodes(requestedSeason)
            val ep = sEpisodes.find { it.episodeNumber == parsedEpNum } ?: sEpisodes.getOrNull(parsedEpNum - 1)
            return requestedSeason to (parsedEpNum to ep)
        }

        // Continuous / absolute episode numbering (e.g. Naruto 370..500):
        var accumulated = 0
        for (season in tmdbSeasons) {
            val seasonEpisodesCount = if (season.episodeCount > 0) season.episodeCount else getSeasonEpisodes(season.seasonNumber).size
            if (seasonEpisodesCount <= 0) continue

            if (parsedEpNum in (accumulated + 1)..(accumulated + seasonEpisodesCount)) {
                val actualEpisodes = getSeasonEpisodes(season.seasonNumber)
                val relativeEpisodeNumber = parsedEpNum - accumulated
                val found = actualEpisodes.find { it.episodeNumber == relativeEpisodeNumber }
                    ?: actualEpisodes.getOrNull(relativeEpisodeNumber - 1)
                return season.seasonNumber to (relativeEpisodeNumber to found)
            }
            accumulated += seasonEpisodesCount
        }

        val sEpisodes = getSeasonEpisodes(requestedSeason)
        val ep = sEpisodes.find { it.episodeNumber == parsedEpNum }
        return requestedSeason to (parsedEpNum to ep)
    }

    private suspend fun buildSeasonPlaylist(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger
        val seasonMatch = Regex("""lampac_season_(?:.+_)?(\d+)_([^_]+)$""").find(sourceId)
        val seasonNum = seasonMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val balancer = seasonMatch?.groupValues?.get(2) ?: "rezka"

        val metadata = try {
            context.catalog.getMediaDetails(key)
        } catch (e: Exception) {
            null
        }

        val mainTitle = metadata?.title ?: ""
        val originalTitle = metadata?.originalTitle
        val year = metadata?.releaseDate?.take(4)?.toIntOrNull()
        val tmdbId = key.id.substringAfterLast(":").toLongOrNull()
        val imdbId = metadata?.imdbId

        // 2. Fetch episodes from Lampac
        val episodes = repository.getEpisodes(
            balancer = balancer,
            title = mainTitle,
            season = seasonNum,
            originalTitle = originalTitle,
            year = year,
            tmdbId = tmdbId,
            imdbId = imdbId
        )

        if (episodes.isEmpty()) {
            logger.warn("Lampac: No episodes returned by balancer '$balancer' for season $seasonNum")
            return emptyList()
        }

        val seasonCache = mutableMapOf<Int, List<EpisodeItem>>()
        val progressList = if (userId != null) {
            try {
                context.userEpisodes.getEpisodesProgress(userId, key.id)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val playlist = episodes.map { ep ->
            val (resolvedSeason, epInfo) = resolveTmdbEpisode(
                key = key,
                metadata = metadata,
                seasonCache = seasonCache,
                requestedSeason = seasonNum,
                rawEpNum = ep.episodeNumber,
                rawName = ep.name.ifBlank { ep.title }
            )
            val (resolvedEpNum, tmdbEp) = epInfo

            val progress = progressList.find {
                (it.season == resolvedSeason && it.episode == resolvedEpNum) ||
                (it.season == seasonNum && it.episode == ep.episodeNumber)
            }

            val episodeName = tmdbEp?.name?.ifBlank { null } ?: ep.name.ifBlank { null } ?: "$resolvedEpNum серия"
            val finalTitle = if (mainTitle.isNotBlank()) {
                "$mainTitle • S${resolvedSeason}E$resolvedEpNum «$episodeName»"
            } else {
                "S${resolvedSeason}E$resolvedEpNum «$episodeName»"
            }

            val epDuration = (tmdbEp?.runtime ?: metadata?.runtime)?.toDouble()?.let { it * 60 }
            val bestQuality = ep.qualities.firstOrNull()?.label ?: "1080p"

            val audioTracks = ep.voices.map { v ->
                AudioTrack(
                    id = "${balancer}_${v.id}",
                    name = v.name,
                    isDefault = v.isActive
                )
            }

            val isDirect = ep.url.contains(".mp4", ignoreCase = true)
            val streamType = if (isDirect) StreamType.DirectUrl else StreamType.Hls
            val streamFormat = if (isDirect) "MP4" else "HLS"

            MediaStream(
                id = "lampac_ep_${balancer}_s${resolvedSeason}e${resolvedEpNum}",
                title = finalTitle,
                url = ep.url,
                type = streamType,
                quality = bestQuality,
                format = streamFormat,
                videoCodec = "H.264",
                sourceName = "Lampac ($balancer)",
                durationSeconds = epDuration,
                isMapped = true,
                seasonNumber = resolvedSeason,
                episodeNumber = resolvedEpNum,
                episodeName = episodeName,
                episodePosterUrl = tmdbEp?.stillUrl ?: metadata?.posterUrl,
                watchedProgressSeconds = progress?.progressSeconds,
                isWatched = progress?.isWatched ?: false,
                userRating = progress?.userRating,
                lastWatchedAtEpochMs = progress?.lastWatchedAtEpochMs,
                audioTracks = audioTracks,
                qualityVariants = ep.qualities,
                subtitleTracks = ep.subtitles
            )
        }

        logger.info("Lampac: Successfully assembled and mapped ${playlist.size} episodes for season $seasonNum ($balancer)")
        return playlist
    }
}
