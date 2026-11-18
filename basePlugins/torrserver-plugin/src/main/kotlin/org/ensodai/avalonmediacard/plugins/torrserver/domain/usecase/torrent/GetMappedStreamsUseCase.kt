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
            val seasonCacheRu = mutableMapOf<Int, List<EpisodeItem>>()
            val seasonCacheEn = mutableMapOf<Int, List<EpisodeItem>>()

            val sortedMappings = dbMappings.sortedWith(
                compareBy(
                    { it.seasons?.firstOrNull() ?: 0 },
                    { it.episodes?.firstOrNull() ?: 0 }
                )
            )

            val isTvShow = key.type == EntityType.TV || sortedMappings.any { !it.seasons.isNullOrEmpty() }

            if (!isTvShow || key.type == EntityType.MOVIE) {
                for (mapping in sortedMappings) {
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

                    val userMovie = if (userId != null) {
                        context.userMovies.getUserMovies(userId).find { it.mediaId == key.id }
                    } else null

                    val progressSec = userMovie?.progressSeconds?.takeIf { it > 0 }
                    val isWatched = userMovie?.status == MediaStatus.COMPLETED

                    mappedStreams.add(
                        MediaStream(
                            id = "movie",
                            title = if (finalUrlGst != null) "[GST] " + mapping.filePath.substringAfterLast('/') else mapping.filePath.substringAfterLast('/'),
                            url = finalUrlGst ?: finalUrlDirect,
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
            } else {
                val tvMappings = sortedMappings.filter { !it.seasons.isNullOrEmpty() && !it.episodes.isNullOrEmpty() }
                val bySeason = tvMappings.groupBy { it.seasons!!.first() }

                for ((season, seasonMappings) in bySeason) {
                    val episodesRu = seasonCacheRu.getOrPut(season) {
                        try {
                            context.catalog.getSeasonDetails(key, season, "ru")
                        } catch (e: Exception) {
                            context.logger.warn("GetMappedStreamsUseCase: Failed to fetch metadata (ru) for season $season: ${e.message}")
                            emptyList()
                        }
                    }
                    val episodesEn = seasonCacheEn.getOrPut(season) {
                        try {
                            context.catalog.getSeasonDetails(key, season, "en")
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    val fileInputs = seasonMappings.map { mapping ->
                        EpisodeAligner.FileInput(
                            filePath = mapping.filePath,
                            parsedSeason = season,
                            parsedEpisodes = mapping.episodes ?: emptyList(),
                            isManual = mapping.isManual
                        )
                    }

                    val alignedMap = EpisodeAligner.alignSeason(season, fileInputs, episodesRu, episodesEn)

                    for (mapping in seasonMappings) {
                        val aligned = alignedMap[mapping.filePath]
                        val finalEpisodeNumber = aligned?.episodeNumber ?: mapping.episodes?.firstOrNull() ?: 1
                        val matchedTmdb = aligned?.matchedEpisode ?: episodesRu.find { it.episodeNumber == finalEpisodeNumber }

                        // Self-healing: if mapping was not manual and episode number changed, update DB
                        if (aligned != null && !mapping.isManual && mapping.episodes?.firstOrNull() != finalEpisodeNumber) {
                            try {
                                context.torrentMappings.saveMapping(
                                    torrentHash = mapping.torrentHash,
                                    filePath = mapping.filePath,
                                    fileSize = mapping.fileSize,
                                    fileIndex = mapping.fileIndex,
                                    seasons = listOf(season),
                                    episodes = listOf(finalEpisodeNumber),
                                    isAbsolute = mapping.isAbsolute,
                                    isManual = false,
                                    mediaId = key.id
                                )
                                context.logger.info("GetMappedStreamsUseCase: Auto-aligned S${season} mapping for ${mapping.filePath} -> Episode $finalEpisodeNumber")
                            } catch (e: Exception) {
                                context.logger.warn("GetMappedStreamsUseCase: Failed to auto-align mapping in DB: ${e.message}")
                            }
                        }

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

                        val progress = progressHistory.find { it.season == season && it.episode == finalEpisodeNumber }
                        val streamId = "s${season}e${finalEpisodeNumber}"

                        val streamUrl = finalUrlGst ?: finalUrlDirect
                        val streamTitle = if (finalUrlGst != null) "[GST] " + mapping.filePath.substringAfterLast('/') else mapping.filePath.substringAfterLast('/')

                        mappedStreams.add(
                            MediaStream(
                                id = streamId,
                                title = streamTitle,
                                url = streamUrl,
                                type = StreamType.DirectUrl,
                                sourceName = "TorrServer",
                                isMapped = true,
                                episodeName = matchedTmdb?.name?.ifBlank { null } ?: context.i18n.t("stream.episode_fmt", finalEpisodeNumber),
                                episodePosterUrl = matchedTmdb?.stillUrl,
                                durationSeconds = matchedTmdb?.runtime?.let { (it * 60).toDouble() },
                                seasonNumber = season,
                                episodeNumber = finalEpisodeNumber,
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
        }

        return mappedStreams.sortedWith(compareBy({ it.seasonNumber ?: 0 }, { it.episodeNumber ?: 0 }))
    }
}
