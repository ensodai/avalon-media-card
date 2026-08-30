package org.ensodai.avalonmediacard.plugins.collaps.domain.usecase

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.plugins.collaps.domain.repository.CollapsRepository
import kotlin.uuid.Uuid

/**
 * **Get Collaps Playlist UseCase**
 *
 * Constructs enriched [MediaStream] playlist items for movies or TV shows from Collaps CDN.
 */
class GetCollapsPlaylistUseCase(
    private val context: PluginContext,
    private val repository: CollapsRepository
) {
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

        return buildSeasonPlaylist(key, sourceId, userId)
    }

    private suspend fun buildMoviePlaylist(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger
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

        val imdbId = metadata?.imdbId?.trim()?.takeIf { it.isNotBlank() }
        val searchTitles = listOfNotNull(metadata?.title, metadata?.originalTitle).distinct()
        val searchResults = if (imdbId != null) {
            logger.info("Collaps Playlist: Ищем фильм по IMDb ID '$imdbId'")
            val res = repository.searchMediaByImdb(imdbId)
            val found = res.firstOrNull { it.type == "film" } ?: res.firstOrNull()
            if (found != null) {
                logger.info("Collaps Playlist: Фильм найден по IMDb ID '$imdbId' -> '${found.name}' (id=${found.id})")
            } else {
                logger.info("Collaps Playlist: По IMDb ID '$imdbId' ничего не найдено, ищем по названиям: $searchTitles")
            }
            found
        } else {
            logger.info("Collaps Playlist: IMDb ID отсутствует, ищем фильм по названиям: $searchTitles")
            null
        } ?: searchTitles.firstNotNullOfOrNull { q ->
            logger.info("Collaps Playlist: Ищем фильм по названию: '$q'")
            val res = repository.searchMedia(q)
            val found = res.firstOrNull { it.type == "film" } ?: res.firstOrNull()
            if (found != null) {
                logger.info("Collaps Playlist: Фильм найден по названию '$q' -> '${found.name}' (id=${found.id})")
            }
            found
        } ?: return emptyList()

        val embed = repository.getEmbedParseResult(searchResults.iframeUrl) ?: return emptyList()
        val rawHls = embed.hlsUrl ?: searchResults.iframeUrl
        val resolved = repository.resolveHlsPlaylist(rawHls)
        val finalTitle = metadata?.title ?: searchResults.name

        val audioTracks = embed.audioNames.mapIndexed { idx, name ->
            AudioTrack(id = "$idx", name = name, isDefault = idx == 0)
        }
        val subtitleTracks = embed.subtitles.mapIndexedNotNull { idx, cc ->
            val url = cc.url ?: return@mapIndexedNotNull null
            SubtitleTrack(id = "$idx", name = cc.name ?: context.i18n.t("subtitles.index_fmt", idx), url = wrapProxyUrl(url))
        }

        val streamUrl = embed.downloadUrl ?: resolved.primaryUrl
        val proxyUrl = wrapProxyUrl(streamUrl)
        val isDirect = streamUrl.contains(".mp4", ignoreCase = true) || streamUrl.contains("dl.showvid.ws")
        val streamType = if (isDirect) StreamType.DirectUrl else StreamType.Hls
        val streamFormat = if (isDirect) "MP4" else "HLS"
        val proxyVariants = resolved.qualityVariants.map { it.copy(url = wrapProxyUrl(it.url)) }
        val actualQuality = resolved.qualityVariants.firstOrNull()?.label ?: searchResults.quality ?: "720p"

        val stream = MediaStream(
            id = "collaps_movie_${searchResults.id}",
            title = finalTitle,
            url = proxyUrl,
            type = streamType,
            quality = actualQuality,
            format = streamFormat,
            videoCodec = "H.264",
            sourceName = "Collaps",
            durationSeconds = metadata?.runtime?.toDouble()?.let { it * 60 } ?: embed.durationSeconds,
            isMapped = true,
            seasonNumber = null,
            episodeNumber = null,
            episodeName = finalTitle,
            episodePosterUrl = metadata?.posterUrl,
            watchedProgressSeconds = movieItem?.progressSeconds,
            isWatched = movieItem?.status == MediaStatus.COMPLETED,
            userRating = movieItem?.userRating,
            lastWatchedAtEpochMs = movieItem?.lastWatchedAt?.toEpochMilliseconds(),
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            qualityVariants = proxyVariants
        )

        return listOf(stream)
    }

    private suspend fun buildSeasonPlaylist(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger
        val seasonNum = Regex("""collaps_season_\d+_(\d+)""").find(sourceId)?.groupValues?.get(1)?.toIntOrNull() ?: 1

        val metadata = try {
            context.catalog.getMediaDetails(key)
        } catch (e: Exception) {
            null
        }

        val imdbId = metadata?.imdbId?.trim()?.takeIf { it.isNotBlank() }
        val searchTitles = listOfNotNull(metadata?.title, metadata?.originalTitle).distinct()
        val searchResult = if (imdbId != null) {
            logger.info("Collaps Playlist: Ищем сериал по IMDb ID '$imdbId'")
            val res = repository.searchMediaByImdb(imdbId)
            val found = res.firstOrNull()
            if (found != null) {
                logger.info("Collaps Playlist: Сериал найден по IMDb ID '$imdbId' -> '${found.name}' (id=${found.id})")
            } else {
                logger.info("Collaps Playlist: По IMDb ID '$imdbId' ничего не найдено, ищем по названиям: $searchTitles")
            }
            found
        } else {
            logger.info("Collaps Playlist: IMDb ID отсутствует, ищем сериал по названиям: $searchTitles")
            null
        } ?: searchTitles.firstNotNullOfOrNull { q ->
            logger.info("Collaps Playlist: Ищем сериал по названию: '$q'")
            val res = repository.searchMedia(q)
            val found = res.firstOrNull()
            if (found != null) {
                logger.info("Collaps Playlist: Сериал найден по названию '$q' -> '${found.name}' (id=${found.id})")
            }
            found
        } ?: return emptyList()

        val seasonInfo = searchResult.seasons?.find { it.season == seasonNum }
        val targetIframe = searchResult.iframeUrl

        val embed = repository.getEmbedParseResult(targetIframe)
            ?: seasonInfo?.iframeUrl?.let { repository.getEmbedParseResult(it) }
        val seasonsData = embed?.seasons
        val seasonData = seasonsData?.find { it.seasonNumber == seasonNum }

        val tmdbEpisodes = try {
            context.catalog.getSeasonDetails(key, seasonNum)
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

        val playlist = mutableListOf<MediaStream>()

        if (seasonData != null && seasonData.episodes.isNotEmpty()) {
            for (epData in seasonData.episodes) {
                val epNum = epData.episodeNumber
                val rawEpHls = epData.hls ?: continue
                val resolvedEp = repository.resolveHlsPlaylist(rawEpHls)
                val tmdbEp = tmdbEpisodes.find { it.episodeNumber == epNum }
                val progress = progressList.find { it.season == seasonNum && it.episode == epNum }

                val episodeName = tmdbEp?.name?.ifBlank { null } ?: context.i18n.t("episode.index_fmt", epNum)
                val finalTitle = "${metadata?.title ?: searchResult.name} • S${seasonNum}E$epNum «$episodeName»"

                val audioTracks = (epData.audio?.names ?: embed?.audioNames ?: emptyList()).mapIndexed { idx, name ->
                    AudioTrack(id = "$idx", name = name, isDefault = idx == 0)
                }
                val subtitleTracks = (epData.cc ?: embed?.subtitles ?: emptyList()).mapIndexedNotNull { idx, cc ->
                    val url = cc.url ?: return@mapIndexedNotNull null
                    SubtitleTrack(id = "$idx", name = cc.name ?: context.i18n.t("subtitles.index_fmt", idx), url = wrapProxyUrl(url))
                }
                val streamUrl = epData.downloadUrl ?: resolvedEp.primaryUrl
                val epProxyUrl = wrapProxyUrl(streamUrl)
                val isDirect = streamUrl.contains(".mp4", ignoreCase = true) || streamUrl.contains("dl.showvid.ws")
                val streamType = if (isDirect) StreamType.DirectUrl else StreamType.Hls
                val streamFormat = if (isDirect) "MP4" else "HLS"
                val epProxyVariants = resolvedEp.qualityVariants.map { it.copy(url = wrapProxyUrl(it.url)) }
                val actualEpQuality = resolvedEp.qualityVariants.firstOrNull()?.label ?: searchResult.quality ?: "720p"

                playlist.add(
                    MediaStream(
                        id = "collaps_ep_${searchResult.id}_s${seasonNum}e${epNum}",
                        title = finalTitle,
                        url = epProxyUrl,
                        type = streamType,
                        quality = actualEpQuality,
                        format = streamFormat,
                        videoCodec = "H.264",
                        sourceName = "Collaps",
                        durationSeconds = epData.durationSeconds?.toDouble() ?: embed?.durationSeconds,
                        isMapped = true,
                        seasonNumber = seasonNum,
                        episodeNumber = epNum,
                        episodeName = episodeName,
                        episodePosterUrl = tmdbEp?.stillUrl ?: metadata?.posterUrl,
                        watchedProgressSeconds = progress?.progressSeconds,
                        isWatched = progress?.isWatched ?: false,
                        userRating = progress?.userRating,
                        lastWatchedAtEpochMs = progress?.lastWatchedAtEpochMs,
                        audioTracks = audioTracks,
                        subtitleTracks = subtitleTracks,
                        qualityVariants = epProxyVariants
                    )
                )
            }
        } else if (seasonInfo?.episodes != null) {
            for (ep in seasonInfo.episodes) {
                val epNum = ep.episode
                val epIframe = ep.iframeUrl ?: continue
                val epEmbed = repository.getEmbedParseResult(epIframe) ?: continue
                val rawEpHls = epEmbed.hlsUrl ?: epIframe
                val resolvedEp = repository.resolveHlsPlaylist(rawEpHls)

                val tmdbEp = tmdbEpisodes.find { it.episodeNumber == epNum }
                val progress = progressList.find { it.season == seasonNum && it.episode == epNum }

                val episodeName = tmdbEp?.name?.ifBlank { null } ?: context.i18n.t("episode.index_fmt", epNum)
                val finalTitle = "${metadata?.title ?: searchResult.name} • S${seasonNum}E$epNum «$episodeName»"

                val audioTracks = epEmbed.audioNames.mapIndexed { idx, name ->
                    AudioTrack(id = "$idx", name = name, isDefault = idx == 0)
                }
                val subtitleTracks = epEmbed.subtitles.mapIndexedNotNull { idx, cc ->
                    val url = cc.url ?: return@mapIndexedNotNull null
                    SubtitleTrack(id = "$idx", name = cc.name ?: context.i18n.t("subtitles.index_fmt", idx), url = wrapProxyUrl(url))
                }
                val streamUrl = epEmbed.downloadUrl ?: resolvedEp.primaryUrl
                val epProxyUrl = wrapProxyUrl(streamUrl)
                val isDirect = streamUrl.contains(".mp4", ignoreCase = true) || streamUrl.contains("dl.showvid.ws")
                val streamType = if (isDirect) StreamType.DirectUrl else StreamType.Hls
                val streamFormat = if (isDirect) "MP4" else "HLS"
                val epProxyVariants = resolvedEp.qualityVariants.map { it.copy(url = wrapProxyUrl(it.url)) }
                val actualEpQuality = resolvedEp.qualityVariants.firstOrNull()?.label ?: searchResult.quality ?: "720p"

                playlist.add(
                    MediaStream(
                        id = "collaps_ep_${searchResult.id}_s${seasonNum}e${epNum}",
                        title = finalTitle,
                        url = epProxyUrl,
                        type = streamType,
                        quality = actualEpQuality,
                        format = streamFormat,
                        videoCodec = "H.264",
                        sourceName = "Collaps",
                        durationSeconds = epEmbed.durationSeconds,
                        isMapped = true,
                        seasonNumber = seasonNum,
                        episodeNumber = epNum,
                        episodeName = episodeName,
                        episodePosterUrl = tmdbEp?.stillUrl ?: metadata?.posterUrl,
                        watchedProgressSeconds = progress?.progressSeconds,
                        isWatched = progress?.isWatched ?: false,
                        userRating = progress?.userRating,
                        lastWatchedAtEpochMs = progress?.lastWatchedAtEpochMs,
                        audioTracks = audioTracks,
                        subtitleTracks = subtitleTracks,
                        qualityVariants = epProxyVariants
                    )
                )
            }
        }

        logger.info("Collaps: Built playlist of ${playlist.size} episodes for season $seasonNum")
        return playlist
    }

    private fun wrapProxyUrl(url: String): String {
        if (url.startsWith("/api/stream-proxy")) return url
        val encoded = java.util.Base64.getUrlEncoder().encodeToString(url.toByteArray(Charsets.UTF_8))
        val refEncoded = java.util.Base64.getUrlEncoder().encodeToString("https://kinokrad.my/".toByteArray(Charsets.UTF_8))
        val origEncoded = java.util.Base64.getUrlEncoder().encodeToString("https://kinokrad.my".toByteArray(Charsets.UTF_8))
        return "/api/stream-proxy?url=$encoded&referer=$refEncoded&origin=$origEncoded"
    }
}
