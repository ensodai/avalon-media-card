package org.ensodai.avalonmediacard.sync.processors

import org.ensodai.avalonmediacard.auth.ExternalCollectionItem
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

@Single
class CollectionSyncProcessor(
    private val userMovieRepository: UserMovieRepository
) {
    private val logger = LoggerFactory.getLogger(CollectionSyncProcessor::class.java)

    suspend fun sync(
        context: SyncContext,
        localMovies: List<UserMovieItem>,
        localEpisodes: List<UserEpisodeItem>,
        externalCollection: List<ExternalCollectionItem>
    ): List<ExternalCollectionItem> {
        if (!context.settings.syncCollection) return emptyList()

        val collectionToPush = mutableListOf<ExternalCollectionItem>()
        val userId = context.userId

        // 1. Фильмы в коллекции
        localMovies.filter { it.mediaType == MediaType.MOVIE }.forEach { localMovie ->
            val extCollection = externalCollection.find {
                it.tmdbId == localMovie.mediaId.toIntOrNull() &&
                        it.mediaType == MediaType.MOVIE
            }

            if (extCollection != null) {
                if (!localMovie.inCollection) {
                    logger.info("Movie ${localMovie.mediaId} is in collection on Trakt. Marking in collection locally.")
                    userMovieRepository.updateUserMovie(localMovie.copy(inCollection = true))
                }
            } else {
                if (localMovie.inCollection) {
                    logger.info("Movie ${localMovie.mediaId} is in collection locally. Pushing to external collection.")
                    collectionToPush.add(
                        ExternalCollectionItem(
                            tmdbId = localMovie.mediaId.toIntOrNull() ?: 0,
                            mediaType = MediaType.MOVIE,
                            addedAt = localMovie.lastWatchedAt
                        )
                    )
                }
            }
        }

        // 2. Серии (эпизоды) в коллекции
        localEpisodes.forEach { localEpisode ->
            val extCollection = externalCollection.find {
                it.tmdbId == localEpisode.mediaId.toIntOrNull() &&
                        it.mediaType == MediaType.TV &&
                        it.season == localEpisode.season &&
                        it.episode == localEpisode.episode
            }

            if (extCollection != null) {
                if (!localEpisode.inCollection) {
                    logger.info("Episode ${localEpisode.mediaId} S${localEpisode.season}E${localEpisode.episode} is in collection on Trakt. Marking in collection locally.")
                    userMovieRepository.updateUserEpisode(localEpisode.copy(inCollection = true))
                }
            } else {
                if (localEpisode.inCollection) {
                    logger.info("Episode ${localEpisode.mediaId} S${localEpisode.season}E${localEpisode.episode} is in collection locally. Pushing to external collection.")
                    collectionToPush.add(
                        ExternalCollectionItem(
                            tmdbId = localEpisode.mediaId.toIntOrNull() ?: 0,
                            mediaType = MediaType.TV,
                            addedAt = localEpisode.lastWatchedAt,
                            season = localEpisode.season,
                            episode = localEpisode.episode
                        )
                    )
                }
            }
        }

        // 3. Импортируем внешние фильмы коллекции, которых нет локально
        externalCollection.filter { it.mediaType == MediaType.MOVIE }.forEach { extCollection ->
            val localMovie = localMovies.find {
                it.mediaId == extCollection.tmdbId.toString() &&
                        it.mediaType == MediaType.MOVIE
            }
            if (localMovie == null) {
                logger.info("Importing external collection item for new movie ${extCollection.tmdbId}")
                userMovieRepository.updateUserMovie(
                    UserMovieItem(
                        id = Uuid.random(),
                        userId = userId,
                        catalogId = "tmdb",
                        mediaId = extCollection.tmdbId.toString(),
                        mediaType = MediaType.MOVIE,
                        status = MediaStatus.WATCHING,
                        userRating = null,
                        progressSeconds = 0L,
                        durationSeconds = 0L,
                        inCollection = true,
                        lastWatchedAt = extCollection.addedAt
                    )
                )
            }
        }

        return collectionToPush
    }
}
