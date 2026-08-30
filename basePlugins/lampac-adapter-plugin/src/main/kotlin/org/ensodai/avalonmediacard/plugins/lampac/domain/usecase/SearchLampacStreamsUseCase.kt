package org.ensodai.avalonmediacard.plugins.lampac.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.ensodai.avalonmediacard.contract.classification.AnimeSubType
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.plugins.lampac.domain.model.LampacBalancer
import org.ensodai.avalonmediacard.plugins.lampac.domain.repository.LampacRepository
import kotlin.uuid.Uuid

/**
 * **Search Lampac Streams UseCase**
 *
 * Queries the Lampac Gateway to discover available balancers and resolve consolidated
 * stream cards for Movies, TV Series, and Anime with multi-audio, quality variants, and IMDb ID routing.
 */
class SearchLampacStreamsUseCase(
    private val context: PluginContext,
    private val repository: LampacRepository
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
            logger.warn("Lampac: Failed to fetch TMDB metadata for $key: ${e.message}")
            null
        }

        val title = metadata?.title?.trim() ?: return emptyList()
        val originalTitle = metadata.originalTitle?.trim()
        val year = metadata.releaseDate?.take(4)?.toIntOrNull()
        val tmdbId = key.id.substringAfterLast(":").toLongOrNull()
        val imdbId = metadata.imdbId?.takeIf { it.isNotBlank() }
        val isSerial = key.type == EntityType.TV
        val isAnimeMedia = metadata?.isAnime == true
        val originalLang = when (metadata?.animeSubType) {
            AnimeSubType.JAPANESE_ANIME -> "ja"
            AnimeSubType.CHINESE_DONGHUA -> "zh"
            AnimeSubType.KOREAN_AENI -> "ko"
            else -> null
        }

        val rawBalancers = repository.getAvailableBalancers(
            title = title,
            originalTitle = originalTitle,
            year = year,
            tmdbId = tmdbId,
            imdbId = imdbId,
            isSerial = isSerial,
            isAnime = isAnimeMedia,
            originalLanguage = originalLang
        )

        // Исключаем некаталогизированные поисковики видео (VK, Rutube), чтобы избежать случайных пользовательских роликов и ложных озвучек
        val balancers = rawBalancers.filterNot { it.id == "vkmovie" || it.id == "rutubemovie" }

        if (balancers.isEmpty()) {
            logger.info("Lampac: No valid curated balancers found for '$title' ($year, imdb=$imdbId, anime=$isAnimeMedia)")
            return emptyList()
        }

        return if (isSerial) {
            searchSeriesStreams(key, balancers, title, originalTitle, year, tmdbId, imdbId, metadata)
        } else {
            searchMovieStreams(key, balancers, title, originalTitle, year, tmdbId, imdbId, metadata)
        }
    }

    private suspend fun searchMovieStreams(
        key: MediaKey,
        balancers: List<LampacBalancer>,
        title: String,
        originalTitle: String?,
        year: Int?,
        tmdbId: Long?,
        imdbId: String?,
        metadata: MediaMetadata?
    ): List<MediaStream> = coroutineScope {
        val streams = balancers.map { balancer ->
            async {
                val streamInfos = repository.getMovieStreams(
                    balancer = balancer.id,
                    title = title,
                    originalTitle = originalTitle,
                    year = year,
                    tmdbId = tmdbId,
                    imdbId = imdbId
                )

                if (streamInfos.isNotEmpty()) {
                    val defaultStream = streamInfos.first()
                    val bestQuality = defaultStream.qualities.firstOrNull()?.label ?: "1080p"
                    val durationSec = metadata?.runtime?.toDouble()?.let { it * 60 }

                    // If multiple streamInfos exist, they represent different audio tracks/translations
                    val combinedAudioTracks = if (streamInfos.size > 1) {
                        streamInfos.mapIndexed { idx, info ->
                            AudioTrack(
                                id = "$idx",
                                name = info.translation ?: "${balancer.name} #${idx + 1}",
                                isDefault = idx == 0,
                                url = info.streamUrl
                            )
                        }
                    } else {
                        defaultStream.audioTracks
                    }

                    val voicesCount = combinedAudioTracks.size
                    val voicesLabel = when {
                        voicesCount > 1 -> " ($voicesCount ${pluralizeVoices(voicesCount)})"
                        voicesCount == 1 -> " (${combinedAudioTracks.first().name})"
                        else -> ""
                    }

                    val isDirect = defaultStream.streamUrl.contains(".mp4", ignoreCase = true)
                    val streamType = if (isDirect) StreamType.DirectUrl else StreamType.Hls
                    val streamFormat = if (isDirect) "MP4" else "HLS"

                    MediaStream(
                        id = "lampac_movie_${balancer.id}_${key.id}",
                        title = "$title$voicesLabel",
                        url = defaultStream.streamUrl,
                        type = streamType,
                        quality = bestQuality,
                        format = streamFormat,
                        videoCodec = "H.264",
                        sourceName = balancer.name,
                        durationSeconds = durationSec,
                        isMapped = true,
                        episodeName = "${balancer.name}$voicesLabel",
                        episodePosterUrl = metadata?.posterUrl,
                        audioTracks = combinedAudioTracks,
                        qualityVariants = defaultStream.qualities,
                        subtitleTracks = defaultStream.subtitles,
                        subFilterId = "balancer_${balancer.id}",
                        subFilterLabel = balancer.name
                    )
                } else null
            }
        }.awaitAll().filterNotNull()

        streams
    }

    private suspend fun searchSeriesStreams(
        key: MediaKey,
        balancers: List<LampacBalancer>,
        title: String,
        originalTitle: String?,
        year: Int?,
        tmdbId: Long?,
        imdbId: String?,
        metadata: MediaMetadata?
    ): List<MediaStream> = coroutineScope {
        val streamLists = balancers.map { balancer ->
            async {
                val seasons = repository.getSeasons(
                    balancer = balancer.id,
                    title = title,
                    originalTitle = originalTitle,
                    year = year,
                    tmdbId = tmdbId,
                    imdbId = imdbId
                )

                seasons.map { season ->
                    val seasonNum = season.seasonNumber
                    val audioTracks = season.voices.map { v ->
                        AudioTrack(
                            id = "${balancer.id}_${v.id}",
                            name = v.name,
                            isDefault = v.isActive
                        )
                    }

                    val voicesCount = season.voices.size
                    val voicesLabel = when {
                        voicesCount > 1 -> " ($voicesCount ${pluralizeVoices(voicesCount)})"
                        voicesCount == 1 -> " (${season.voices.first().name})"
                        else -> ""
                    }

                    val tmdbSeason = metadata?.seasons?.find { it.seasonNumber == seasonNum }
                    val epTotal = tmdbSeason?.episodeCount

                    val groupId = "lampac_season_${key.id}_${seasonNum}_${balancer.id}"

                    MediaStream(
                        id = groupId,
                        title = "$title • $seasonNum сезон$voicesLabel",
                        url = season.url,
                        type = StreamType.Hls,
                        quality = season.maxQuality ?: "1080p",
                        format = "HLS",
                        videoCodec = "H.264",
                        sourceName = balancer.name,
                        isMapped = true,
                        seasonNumber = seasonNum,
                        episodeNumber = null,
                        episodesCount = epTotal ?: 1,
                        episodesTotal = epTotal,
                        episodeName = "${balancer.name}$voicesLabel",
                        episodePosterUrl = tmdbSeason?.posterUrl ?: metadata?.posterUrl,
                        audioTracks = audioTracks,
                        subFilterId = "season_$seasonNum",
                        subFilterLabel = "$seasonNum сезон"
                    )
                }
            }
        }.awaitAll().flatten()

        streamLists.sortedWith(compareBy({ it.seasonNumber ?: 99 }, { it.sourceName }))
    }

    private fun pluralizeVoices(count: Int): String {
        val rem10 = count % 10
        val rem100 = count % 100
        return when {
            rem100 in 11..19 -> "озвучек"
            rem10 == 1 -> "озвучка"
            rem10 in 2..4 -> "озвучки"
            else -> "озвучек"
        }
    }
}
