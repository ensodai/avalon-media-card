package org.ensodai.avalonmediacard.sync.processors

import org.ensodai.avalonmediacard.auth.ExternalRatingItem
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Single
class RatingSyncProcessor(
    private val userMovieRepository: UserMovieRepository
) {
    private val logger = LoggerFactory.getLogger(RatingSyncProcessor::class.java)

    suspend fun sync(
        context: SyncContext,
        localMovies: List<UserMovieItem>,
        externalRatings: List<ExternalRatingItem>
    ): List<ExternalRatingItem> {
        if (!context.settings.syncRatings) return emptyList()

        val ratingsToPush = mutableListOf<ExternalRatingItem>()
        val userId = context.userId

        localMovies.forEach { localMovie ->
            val extRating = externalRatings.find {
                it.tmdbId == localMovie.mediaId.toIntOrNull() &&
                        it.mediaType == localMovie.mediaType &&
                        it.season == null
            }

            if (extRating != null) {
                if (localMovie.userRating != extRating.rating) {
                    if (context.isFirstSync) {
                        // При первом синке: если локальная оценка новее (судя по lastWatchedAt) или внешняя оценка старше,
                        // мы можем разрешить конфликт. Но проще и надежнее довериться внешней оценке, если локальный lastWatchedAt старше.
                        // Для первого синка примем, что внешняя оценка приоритетна, кроме случаев когда локальное обновление явно новее.
                        val localTime = localMovie.lastWatchedAt
                        val extTime = extRating.ratedAt
                        if (localTime != null && extTime != null && localTime > extTime) {
                            logger.info("Local rating is newer for ${localMovie.mediaId}. Pushing local rating ${localMovie.userRating} to Trakt.")
                            ratingsToPush.add(
                                ExternalRatingItem(
                                    tmdbId = localMovie.mediaId.toIntOrNull() ?: 0,
                                    mediaType = localMovie.mediaType,
                                    rating = localMovie.userRating ?: 0,
                                    ratedAt = localTime
                                )
                            )
                        } else {
                            logger.info("Importing newer/conflict external rating for ${localMovie.mediaId} -> ${extRating.rating}")
                            userMovieRepository.updateUserMovie(localMovie.copy(userRating = extRating.rating))
                        }
                    } else {
                        // Обычный синк: внешняя оценка побеждает
                        logger.info("Rating conflict for ${localMovie.mediaId}. Overwriting local rating ${localMovie.userRating} with external ${extRating.rating}")
                        userMovieRepository.updateUserMovie(localMovie.copy(userRating = extRating.rating))
                    }
                }
            } else {
                // Оценки нет на внешнем сервисе
                if (localMovie.userRating != null && localMovie.userRating!! > 0) {
                    logger.info("Pushing local rating for ${localMovie.mediaId} -> ${localMovie.userRating}")
                    ratingsToPush.add(
                        ExternalRatingItem(
                            tmdbId = localMovie.mediaId.toIntOrNull() ?: 0,
                            mediaType = localMovie.mediaType,
                            rating = localMovie.userRating!!,
                            ratedAt = localMovie.lastWatchedAt
                        )
                    )
                }
            }
        }

        // Импортируем внешние оценки, которых нет локально
        externalRatings.forEach { extRating ->
            if (extRating.season == null) {
                val localMovie = localMovies.find {
                    it.mediaId == extRating.tmdbId.toString() &&
                            it.mediaType == extRating.mediaType
                }
                if (localMovie == null) {
                    logger.info("Importing external rating for new media ${extRating.tmdbId} (${extRating.mediaType}) -> ${extRating.rating}")
                    userMovieRepository.updateUserMovie(
                        UserMovieItem(
                            id = Uuid.random(),
                            userId = userId,
                            catalogId = "tmdb",
                            mediaId = extRating.tmdbId.toString(),
                            mediaType = extRating.mediaType,
                            status = MediaStatus.WATCHING,
                            userRating = extRating.rating,
                            progressSeconds = 0L,
                            durationSeconds = 0L,
                            lastWatchedAt = extRating.ratedAt
                        )
                    )
                } else if (localMovie.userRating == null) {
                    logger.info("Importing external rating for existing media ${extRating.tmdbId} -> ${extRating.rating}")
                    userMovieRepository.updateUserMovie(localMovie.copy(userRating = extRating.rating))
                }
            }
        }

        return ratingsToPush
    }
}
