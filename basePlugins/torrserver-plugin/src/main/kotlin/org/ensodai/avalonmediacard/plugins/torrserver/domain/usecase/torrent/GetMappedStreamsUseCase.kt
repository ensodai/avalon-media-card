package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.TorrServerRepository
import kotlin.uuid.Uuid

class GetMappedStreamsUseCase(
    private val context: PluginContext,
    private val torrServerRepository: TorrServerRepository
) {
    suspend fun execute(key: MediaKey, boundHash: String? = null, userId: Uuid? = null): List<MediaStream> {
        var dbMappings = context.torrentMappings.getMappingsByMediaId(key.id)
        if (boundHash != null) {
            dbMappings = dbMappings.filter { it.torrentHash == boundHash }
        }
        context.logger.info("GetMappedStreamsUseCase: found ${dbMappings.size} mappings for mediaId=${key.id} (type: ${key.type})")

        val mappedStreams = mutableListOf<MediaStream>()

        // Fetch user progress history
        val progressHistory = if (userId != null) {
            context.userEpisodes.getEpisodesProgress(userId, key.id)
        } else {
            emptyList()
        }

        if (dbMappings.isNotEmpty()) {
            val seasonCache = mutableMapOf<Int, List<EpisodeItem>>()
            val seasonOffsets = mutableMapOf<Int, Int>()

            val sortedMappings = dbMappings.sortedWith(
                compareBy(
                    { it.seasons?.firstOrNull() ?: 0 },
                    { it.episodes?.firstOrNull() ?: 0 }
                )
            )

            for (mapping in sortedMappings) {
                val mappingSeasons = mapping.seasons
                val mappingEpisodes = mapping.episodes

                val isMovie =
                    key.type == EntityType.MOVIE || (mappingSeasons.isNullOrEmpty() && mappingEpisodes.isNullOrEmpty())
                if (!isMovie && (mappingSeasons.isNullOrEmpty() || mappingEpisodes.isNullOrEmpty())) continue

                val useGst = context.integrationManager.getTorrServerUseGst(userId)
                val finalUrlDirect = torrServerRepository.buildStreamUrl(
                    hash = mapping.torrentHash,
                    fileIndex = mapping.fileIndex,
                    filePath = mapping.filePath,
                    userId = userId,
                    useGst = false
                )
                
                val finalUrlGst = if (useGst) torrServerRepository.buildStreamUrl(
                    hash = mapping.torrentHash,
                    fileIndex = mapping.fileIndex,
                    filePath = mapping.filePath,
                    userId = userId,
                    useGst = true
                ) else null

                if (isMovie) {
                    val userMovie = if (userId != null) {
                        context.userMovies.getUserMovies(userId).find { it.mediaId == key.id }
                    } else null

                    val progressSec = userMovie?.progressSeconds?.takeIf { it > 0 }
                    val isWatched = userMovie?.status == MediaStatus.COMPLETED

                    if (finalUrlGst != null) {
                        mappedStreams.add(
                            MediaStream(
                                id = "movie",
                                title = "[GST] " + mapping.filePath.substringAfterLast('/'),
                                url = finalUrlGst,
                                type = StreamType.DirectUrl,
                                sourceName = "TorrServer",
                                isMapped = true,
                                episodeName = mapping.filePath.substringAfterLast('/'),
                                sizeBytes = null,
                                durationSeconds = userMovie?.durationSeconds?.toDouble(),
                                watchedProgressSeconds = progressSec,
                                isWatched = isWatched
                            )
                        )
                    } else {
                        mappedStreams.add(
                            MediaStream(
                                id = "movie",
                                title = mapping.filePath.substringAfterLast('/'),
                                url = finalUrlDirect,
                                type = StreamType.DirectUrl,
                                sourceName = "TorrServer",
                                isMapped = true,
                                episodeName = mapping.filePath.substringAfterLast('/'),
                                sizeBytes = null,
                                durationSeconds = userMovie?.durationSeconds?.toDouble(),
                                watchedProgressSeconds = progressSec,
                                isWatched = isWatched
                            )
                        )
                    }
                    continue
                }

                val season = mappingSeasons!!.first()
                val baseEpisode = mappingEpisodes!!.first()

                val currentOffset = seasonOffsets.getOrDefault(season, 0)
                val targetTmdbEpisode = baseEpisode - currentOffset

                // Обновляем сдвиг для последующих файлов в этом сезоне
                seasonOffsets[season] = currentOffset + (mappingEpisodes.size - 1)

                var tmdbEpisodeName: String? = null
                var tmdbPosterUrl: String? = null
                var tmdbDuration: Double? = null

                val episodes = seasonCache.getOrPut(season) {
                    try {
                        context.catalog.getSeasonDetails(key, season)
                    } catch (e: Exception) {
                        context.logger.warn("GetMappedStreamsUseCase: Failed to fetch metadata for season $season: ${e.message}")
                        emptyList()
                    }
                }

                val ep = episodes.find { it.episodeNumber == targetTmdbEpisode }
                if (ep != null) {
                    tmdbEpisodeName = ep.name
                    tmdbPosterUrl = ep.stillUrl
                    tmdbDuration = ep.runtime?.toDouble()
                }

                val progress = progressHistory.find { it.season == season && it.episode == baseEpisode }

                val streamId = "s${season}e${baseEpisode}"

                if (finalUrlGst != null) {
                    mappedStreams.add(
                        MediaStream(
                            id = streamId,
                            title = "[GST] " + mapping.filePath.substringAfterLast('/'),
                            url = finalUrlGst,
                            type = StreamType.DirectUrl,
                            sourceName = "TorrServer",
                            isMapped = true,
                            episodeName = tmdbEpisodeName ?: context.i18n.t("stream.episode_fmt", baseEpisode),
                            episodePosterUrl = tmdbPosterUrl,
                            durationSeconds = tmdbDuration?.let { it * 60 },
                            seasonNumber = season,
                            episodeNumber = baseEpisode,
                            sizeBytes = null,
                            watchedProgressSeconds = progress?.progressSeconds,
                            isWatched = progress?.isWatched ?: false,
                            userRating = progress?.userRating,
                            lastWatchedAtEpochMs = progress?.lastWatchedAtEpochMs
                        )
                    )
                } else {
                    mappedStreams.add(
                        MediaStream(
                            id = streamId,
                            title = mapping.filePath.substringAfterLast('/'),
                            url = finalUrlDirect,
                            type = StreamType.DirectUrl,
                            sourceName = "TorrServer",
                            isMapped = true,
                            episodeName = tmdbEpisodeName ?: context.i18n.t("stream.episode_fmt", baseEpisode),
                            episodePosterUrl = tmdbPosterUrl,
                            durationSeconds = tmdbDuration?.let { it * 60 },
                            seasonNumber = season,
                            episodeNumber = baseEpisode,
                            sizeBytes = null,
                            watchedProgressSeconds = progress?.progressSeconds,
                            isWatched = progress?.isWatched ?: false,
                            userRating = progress?.userRating,
                            lastWatchedAtEpochMs = progress?.lastWatchedAtEpochMs
                        )
                    )
                }
            }
        }

        return mappedStreams.sortedWith(compareBy({ it.seasonNumber ?: 0 }, { it.episodeNumber ?: 0 }))
    }
}
