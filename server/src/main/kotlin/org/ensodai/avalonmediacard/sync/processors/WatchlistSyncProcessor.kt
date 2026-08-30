package org.ensodai.avalonmediacard.sync.processors

import org.ensodai.avalonmediacard.auth.ExternalWatchlistItem
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Single
class WatchlistSyncProcessor(
    private val userMovieRepository: UserMovieRepository
) {
    private val logger = LoggerFactory.getLogger(WatchlistSyncProcessor::class.java)

    suspend fun sync(
        context: SyncContext,
        localMovies: List<UserMovieItem>,
        externalWatchlist: List<ExternalWatchlistItem>
    ): List<ExternalWatchlistItem> {
        if (!context.settings.syncWatchlist) return emptyList()

        val watchlistToPush = mutableListOf<ExternalWatchlistItem>()
        val userId = context.userId

        localMovies.forEach { localMovie ->
            if (localMovie.status == MediaStatus.PLANNED) {
                val extWatchlist = externalWatchlist.find {
                    it.tmdbId == localMovie.mediaId.toIntOrNull() &&
                            it.mediaType == localMovie.mediaType
                }
                if (extWatchlist == null) {
                    logger.info("Media ${localMovie.mediaId} is PLANNED locally. Pushing to external watchlist.")
                    watchlistToPush.add(
                        ExternalWatchlistItem(
                            tmdbId = localMovie.mediaId.toIntOrNull() ?: 0,
                            mediaType = localMovie.mediaType,
                            addedAt = localMovie.lastWatchedAt
                        )
                    )
                }
            }
        }

        externalWatchlist.forEach { extWatchlist ->
            val localMovie = localMovies.find {
                it.mediaId == extWatchlist.tmdbId.toString() &&
                        it.mediaType == extWatchlist.mediaType
            }
            if (localMovie == null) {
                logger.info("Importing external watchlist item ${extWatchlist.tmdbId} as PLANNED")
                userMovieRepository.updateUserMovie(
                    UserMovieItem(
                        id = Uuid.random(),
                        userId = userId,
                        catalogId = "tmdb",
                        mediaId = extWatchlist.tmdbId.toString(),
                        mediaType = extWatchlist.mediaType,
                        status = MediaStatus.PLANNED,
                        userRating = null,
                        progressSeconds = 0L,
                        durationSeconds = 0L,
                        lastWatchedAt = extWatchlist.addedAt
                    )
                )
            }
        }

        return watchlistToPush
    }
}
