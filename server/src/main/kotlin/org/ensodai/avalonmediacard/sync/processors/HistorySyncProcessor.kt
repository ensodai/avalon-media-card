package org.ensodai.avalonmediacard.sync.processors

import org.ensodai.avalonmediacard.auth.ExternalHistoryItem
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

@Single
class HistorySyncProcessor(
    private val userMovieRepository: UserMovieRepository
) {
    private val logger = LoggerFactory.getLogger(HistorySyncProcessor::class.java)

    suspend fun sync(
        context: SyncContext,
        localMovies: List<UserMovieItem>,
        localEpisodes: List<UserEpisodeItem>,
        externalHistory: List<ExternalHistoryItem>
    ): List<ExternalHistoryItem> {
        if (!context.settings.syncHistory) return emptyList()

        val historyToPush = mutableListOf<ExternalHistoryItem>()
        val userId = context.userId

        // 1. Синхронизация просмотров фильмов
        localMovies.filter { it.mediaType == MediaType.MOVIE }.forEach { localMovie ->
            val extHistory = externalHistory.find {
                it.tmdbId == localMovie.mediaId.toIntOrNull() &&
                        it.mediaType == MediaType.MOVIE
            }

            if (extHistory != null) {
                // Фильм просмотрен и там, и там
                if (localMovie.status != MediaStatus.COMPLETED) {
                    logger.info("Movie ${localMovie.mediaId} is watched on Trakt. Marking COMPLETED locally.")
                    userMovieRepository.updateUserMovie(
                        localMovie.copy(
                            status = MediaStatus.COMPLETED,
                            lastWatchedAt = extHistory.watchedAt
                        )
                    )
                } else if (context.isFirstSync) {
                    // Конфликт дат при первой синхронизации: сравниваем время
                    val localTime = localMovie.lastWatchedAt
                    val extTime = extHistory.watchedAt
                    if (localTime > extTime) {
                        logger.info("Local watch date is newer for movie ${localMovie.mediaId}. Pushing history.")
                        historyToPush.add(
                            ExternalHistoryItem(
                                tmdbId = localMovie.mediaId.toIntOrNull() ?: 0,
                                mediaType = MediaType.MOVIE,
                                watchedAt = localTime
                            )
                        )
                    }
                }
            } else {
                // Просмотрено локально, но нет на Trakt
                if (localMovie.status == MediaStatus.COMPLETED) {
                    logger.info("Movie ${localMovie.mediaId} is COMPLETED locally but not on Trakt. Pushing history.")
                    historyToPush.add(
                        ExternalHistoryItem(
                            tmdbId = localMovie.mediaId.toIntOrNull() ?: 0,
                            mediaType = MediaType.MOVIE,
                            watchedAt = localMovie.lastWatchedAt
                        )
                    )
                }
            }
        }

        // 2. Синхронизация просмотров серий (эпизодов)
        localEpisodes.forEach { localEpisode ->
            val extHistory = externalHistory.find {
                it.tmdbId == localEpisode.mediaId.toIntOrNull() &&
                        it.mediaType == MediaType.TV &&
                        it.season == localEpisode.season &&
                        it.episode == localEpisode.episode
            }

            if (extHistory != null) {
                if (!localEpisode.isWatched) {
                    logger.info("Episode ${localEpisode.mediaId} S${localEpisode.season}E${localEpisode.episode} is watched on Trakt. Marking watched locally.")
                    userMovieRepository.updateUserEpisode(
                        localEpisode.copy(
                            isWatched = true,
                            lastWatchedAt = extHistory.watchedAt
                        )
                    )
                } else if (context.isFirstSync) {
                    val localTime = localEpisode.lastWatchedAt
                    val extTime = extHistory.watchedAt
                    if (localTime > extTime) {
                        logger.info("Local watch date is newer for episode ${localEpisode.mediaId} S${localEpisode.season}E${localEpisode.episode}. Pushing history.")
                        historyToPush.add(
                            ExternalHistoryItem(
                                tmdbId = localEpisode.mediaId.toIntOrNull() ?: 0,
                                mediaType = MediaType.TV,
                                watchedAt = localTime,
                                season = localEpisode.season,
                                episode = localEpisode.episode
                            )
                        )
                    }
                }
            } else {
                if (localEpisode.isWatched) {
                    logger.info("Episode ${localEpisode.mediaId} S${localEpisode.season}E${localEpisode.episode} is watched locally. Pushing history.")
                    historyToPush.add(
                        ExternalHistoryItem(
                            tmdbId = localEpisode.mediaId.toIntOrNull() ?: 0,
                            mediaType = MediaType.TV,
                            watchedAt = localEpisode.lastWatchedAt,
                            season = localEpisode.season,
                            episode = localEpisode.episode
                        )
                    )
                }
            }
        }

        // 3. Импортируем внешние просмотры фильмов, которых нет локально
        externalHistory.filter { it.mediaType == MediaType.MOVIE }.forEach { extHistory ->
            val localMovie = localMovies.find {
                it.mediaId == extHistory.tmdbId.toString() &&
                        it.mediaType == MediaType.MOVIE
            }
            if (localMovie == null) {
                logger.info("Importing external watch history for new movie ${extHistory.tmdbId}")
                userMovieRepository.updateUserMovie(
                    UserMovieItem(
                        id = Uuid.random(),
                        userId = userId,
                        catalogId = "tmdb",
                        mediaId = extHistory.tmdbId.toString(),
                        mediaType = MediaType.MOVIE,
                        status = MediaStatus.COMPLETED,
                        userRating = null,
                        progressSeconds = 0L,
                        durationSeconds = 0L,
                        lastWatchedAt = extHistory.watchedAt
                    )
                )
            }
        }

        return historyToPush
    }
}
