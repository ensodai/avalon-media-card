package org.ensodai.avalonmediacard.plugins.collaps.domain.usecase

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.plugins.collaps.domain.model.CollapsSearchResult
import org.ensodai.avalonmediacard.plugins.collaps.domain.repository.CollapsRepository
import kotlin.uuid.Uuid

/**
 * **Search Collaps Streams UseCase**
 *
 * Discovers movies and TV shows from Collaps CDN balancer.
 */
class SearchCollapsStreamsUseCase(
    private val context: PluginContext,
    private val repository: CollapsRepository
) {
    suspend fun execute(
        key: MediaKey,
        season: Int?,
        episode: Int?,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger

        val metadata = try {
            context.catalog.getMediaDetails(key)
        } catch (e: Exception) {
            logger.warn("Collaps: Failed to fetch TMDB details for key=$key: ${e.message}")
            return emptyList()
        }

        val mainTitle = metadata.title.trim()
        val originalTitle = metadata.originalTitle?.trim()
        val isMovie = key.type == EntityType.MOVIE

        val imdbId = metadata.imdbId?.trim()?.takeIf { it.isNotBlank() }
        val searchTitles = listOfNotNull(mainTitle.takeIf { it.isNotBlank() }, originalTitle.takeIf { !it.isNullOrBlank() }).distinct()

        val foundResults = mutableListOf<CollapsSearchResult>()
        val seenIds = mutableSetOf<Int>()

        if (imdbId != null) {
            logger.info("Collaps: Ищем по IMDb ID: '$imdbId' для '$mainTitle'")
            val imdbResults = repository.searchMediaByImdb(imdbId)
            for (item in imdbResults) {
                if (seenIds.add(item.id)) {
                    foundResults.add(item)
                }
            }
            if (foundResults.isNotEmpty()) {
                logger.info("Collaps: Найдено ${foundResults.size} результат(ов) по IMDb ID '$imdbId'")
            } else {
                logger.info("Collaps: По IMDb ID '$imdbId' ничего не найдено, переключаемся на поиск по названиям: $searchTitles")
            }
        } else {
            logger.info("Collaps: IMDb ID отсутствует, ищем по названиям: $searchTitles")
        }

        if (foundResults.isEmpty()) {
            for (query in searchTitles) {
                logger.info("Collaps: Ищем по названию: '$query'")
                val results = repository.searchMedia(query)
                for (item in results) {
                    if (seenIds.add(item.id)) {
                        foundResults.add(item)
                    }
                }
                if (foundResults.isNotEmpty()) {
                    logger.info("Collaps: Найдено ${foundResults.size} результат(ов) по названию '$query'")
                    break
                }
            }
        }

        if (foundResults.isEmpty()) {
            logger.info("Collaps: Ничего не найдено для '$mainTitle'")
            return emptyList()
        }

        return if (isMovie) {
            buildMovieStreams(key, foundResults.take(3))
        } else {
            if (episode != null) {
                buildSpecificEpisodeStreams(key, season ?: 1, episode, foundResults.take(3))
            } else {
                buildSeasonGroupStreams(key, season, foundResults.take(3))
            }
        }
    }

    private suspend fun buildMovieStreams(
        key: MediaKey,
        results: List<CollapsSearchResult>
    ): List<MediaStream> {
        val streams = mutableListOf<MediaStream>()

        for (item in results) {
            val embed = repository.getEmbedParseResult(item.iframeUrl) ?: continue
            val rawHls = embed.hlsUrl ?: item.iframeUrl
            val resolved = repository.resolveHlsPlaylist(rawHls)

            val audioTracks = embed.audioNames.mapIndexed { idx, name ->
                AudioTrack(id = "$idx", name = name, isDefault = idx == 0)
            }
            val subtitleTracks = embed.subtitles.mapIndexedNotNull { idx, cc ->
                val url = cc.url ?: return@mapIndexedNotNull null
                SubtitleTrack(id = "$idx", name = cc.name ?: context.i18n.t("subtitles.index_fmt", idx), url = wrapProxyUrl(url))
            }

            val subtitleText = formatAudioNamesSummary(embed.audioNames)

            val streamUrl = embed.downloadUrl ?: resolved.primaryUrl
            val proxyUrl = wrapProxyUrl(streamUrl)
            val isDirect = streamUrl.contains(".mp4", ignoreCase = true) || streamUrl.contains("dl.showvid.ws")
            val streamType = if (isDirect) StreamType.DirectUrl else StreamType.Hls
            val streamFormat = if (isDirect) "MP4" else "HLS"
            val proxyVariants = resolved.qualityVariants.map { it.copy(url = wrapProxyUrl(it.url)) }
            val actualQuality = resolved.qualityVariants.firstOrNull()?.label ?: item.quality ?: "720p"

            streams.add(
                MediaStream(
                    id = "collaps_movie_${item.id}",
                    title = item.name,
                    url = proxyUrl,
                    type = streamType,
                    quality = actualQuality,
                    format = streamFormat,
                    videoCodec = "H.264",
                    sourceName = "Collaps",
                    durationSeconds = embed.durationSeconds,
                    isMapped = true,
                    seasonNumber = null,
                    episodeNumber = null,
                    episodeName = subtitleText,
                    audioTracks = audioTracks,
                    subtitleTracks = subtitleTracks,
                    qualityVariants = proxyVariants
                )
            )
        }

        return streams
    }

    private suspend fun buildSpecificEpisodeStreams(
        key: MediaKey,
        targetSeason: Int,
        targetEpisode: Int,
        results: List<CollapsSearchResult>
    ): List<MediaStream> {
        val streams = mutableListOf<MediaStream>()

        for (item in results) {
            val targetIframe = item.seasons?.find { it.season == targetSeason }
                ?.episodes?.find { it.episode == targetEpisode }?.iframeUrl
                ?: "${item.iframeUrl}?season=$targetSeason&episode=$targetEpisode"

            val embed = repository.getEmbedParseResult(targetIframe) ?: continue
            val rawHls = embed.hlsUrl ?: targetIframe
            val resolved = repository.resolveHlsPlaylist(rawHls)

            val audioTracks = embed.audioNames.mapIndexed { idx, name ->
                AudioTrack(id = "$idx", name = name, isDefault = idx == 0)
            }
            val subtitleTracks = embed.subtitles.mapIndexedNotNull { idx, cc ->
                val url = cc.url ?: return@mapIndexedNotNull null
                SubtitleTrack(id = "$idx", name = cc.name ?: context.i18n.t("subtitles.index_fmt", idx), url = wrapProxyUrl(url))
            }

            val subtitleText = "S${targetSeason}E$targetEpisode • ${formatAudioNamesSummary(embed.audioNames)}"
            val streamUrl = embed.downloadUrl ?: resolved.primaryUrl
            val proxyUrl = wrapProxyUrl(streamUrl)
            val isDirect = streamUrl.contains(".mp4", ignoreCase = true) || streamUrl.contains("dl.showvid.ws")
            val streamType = if (isDirect) StreamType.DirectUrl else StreamType.Hls
            val streamFormat = if (isDirect) "MP4" else "HLS"
            val proxyVariants = resolved.qualityVariants.map { it.copy(url = wrapProxyUrl(it.url)) }
            val actualQuality = resolved.qualityVariants.firstOrNull()?.label ?: item.quality ?: "720p"

            streams.add(
                MediaStream(
                    id = "collaps_ep_${item.id}_s${targetSeason}e${targetEpisode}",
                    title = item.name,
                    url = proxyUrl,
                    type = streamType,
                    quality = actualQuality,
                    format = streamFormat,
                    videoCodec = "H.264",
                    sourceName = "Collaps",
                    durationSeconds = embed.durationSeconds,
                    isMapped = true,
                    seasonNumber = targetSeason,
                    episodeNumber = targetEpisode,
                    episodeName = subtitleText,
                    audioTracks = audioTracks,
                    subtitleTracks = subtitleTracks,
                    qualityVariants = proxyVariants
                )
            )
        }

        return streams
    }

    private suspend fun formatAudioNamesSummary(audioNames: List<String>): String {
        if (audioNames.isEmpty()) return "Collaps"
        val count = audioNames.size
        val firstDubs = audioNames.take(2).joinToString(", ")
        return if (count > 2) {
            context.i18n.t("dubs.multiple_fmt", count, firstDubs)
        } else {
            context.i18n.t("dubs.single_fmt", firstDubs)
        }
    }

    private suspend fun buildSeasonGroupStreams(
        key: MediaKey,
        targetSeasonHint: Int?,
        results: List<CollapsSearchResult>
    ): List<MediaStream> {
        val streams = mutableListOf<MediaStream>()

        for (item in results) {
            val seasonsList = item.seasons ?: emptyList()
            if (seasonsList.isEmpty()) {
                // Single season fallback
                val seasonNum = targetSeasonHint ?: 1
                val groupId = "collaps_season_${item.id}_$seasonNum"

                streams.add(
                    MediaStream(
                        id = groupId,
                        title = "${item.name} • ${context.i18n.t("season.index_fmt", seasonNum)}",
                        url = item.iframeUrl,
                        type = StreamType.Hls,
                        quality = item.quality ?: "1080p",
                        format = "HLS",
                        videoCodec = "H.264",
                        sourceName = "Collaps",
                        isMapped = true,
                        seasonNumber = seasonNum,
                        episodeNumber = null,
                        episodesCount = 10,
                        episodeName = context.i18n.t("season.collaps_fmt", seasonNum)
                    )
                )
            } else {
                for (s in seasonsList) {
                    val epCount = s.episodes?.size ?: 10
                    val groupId = "collaps_season_${item.id}_${s.season}"

                    streams.add(
                        MediaStream(
                            id = groupId,
                            title = "${item.name} • ${context.i18n.t("season.index_fmt", s.season)}",
                            url = s.iframeUrl ?: item.iframeUrl,
                            type = StreamType.Hls,
                            quality = item.quality ?: "1080p",
                            format = "HLS",
                            videoCodec = "H.264",
                            sourceName = "Collaps",
                            isMapped = true,
                            seasonNumber = s.season,
                            episodeNumber = null,
                            episodesCount = epCount,
                            episodeName = context.i18n.t("episodes.collaps_count_fmt", epCount)
                        )
                    )
                }
            }
        }

        streams.sortBy { it.seasonNumber ?: 99 }
        return streams
    }

    private fun wrapProxyUrl(url: String): String {
        if (url.startsWith("/api/stream-proxy")) return url
        val encoded = java.util.Base64.getUrlEncoder().encodeToString(url.toByteArray(Charsets.UTF_8))
        val refEncoded = java.util.Base64.getUrlEncoder().encodeToString("https://kinokrad.my/".toByteArray(Charsets.UTF_8))
        val origEncoded = java.util.Base64.getUrlEncoder().encodeToString("https://kinokrad.my".toByteArray(Charsets.UTF_8))
        return "/api/stream-proxy?url=$encoded&referer=$refEncoded&origin=$origEncoded"
    }
}
