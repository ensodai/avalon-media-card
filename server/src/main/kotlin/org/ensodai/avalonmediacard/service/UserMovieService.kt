package org.ensodai.avalonmediacard.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.auth.ExternalShowProgressProvider
import org.ensodai.avalonmediacard.auth.OAuthProvider
import org.ensodai.avalonmediacard.auth.WatchedProgress
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.IntegrationService
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.sync.SyncAction
import org.ensodai.avalonmediacard.contract.sync.SyncStatus
import org.ensodai.avalonmediacard.contract.sync.UserMediaSyncQueueItem
import org.ensodai.avalonmediacard.repository.UserExternalAuthRepository
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

@Single
class UserMovieService(
    private val userMovieRepository: UserMovieRepository,
    private val userExternalAuthRepository: UserExternalAuthRepository,
    private val oauthProviders: List<OAuthProvider>,
    private val progressProviders: List<ExternalShowProgressProvider>,
    private val mediaCatalog: MediaCatalog
) {
    private val traktSyncProvider: ExternalShowProgressProvider?
        get() = progressProviders.find { it.serviceName.equals("trakt", ignoreCase = true) }
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun getShowWatchedProgress(userId: Uuid, showTmdbId: Int): WatchedProgress? {
        val cached = userMovieRepository.getCachedShowWatchedProgress(userId, showTmdbId)
        val now = Clock.System.now()
        val needsUpdate = cached == null || (now - cached.updatedAt) >= 5.minutes
        if (needsUpdate) {
            serviceScope.launch {
                try {
                    val token = userExternalAuthRepository.getToken(userId, IntegrationService.TRAKT)?.accessToken
                    if (token != null) {
                        val progress = traktSyncProvider?.getShowWatchedProgress(token, showTmdbId)
                        userMovieRepository.saveCachedShowWatchedProgress(userId, showTmdbId, progress)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return cached?.progress
    }

    suspend fun getUserMovies(userId: Uuid): List<UserMovieItem> {
        return userMovieRepository.getUserMovies(userId)
    }

    suspend fun getUserEpisodes(userId: Uuid, mediaId: String): List<UserEpisodeItem> {
        return userMovieRepository.getUserEpisodes(userId, mediaId)
    }

    suspend fun deleteUserMovie(userId: Uuid, mediaId: String): Boolean {
        return userMovieRepository.deleteUserMovie(userId, mediaId)
    }

    suspend fun updateUserMovie(item: UserMovieItem): Boolean {
        // 1. Сохраняем локально в БД
        val saved = userMovieRepository.updateUserMovie(item)
        if (!saved) return false

        // 2. Add metadata to cache in background
        serviceScope.launch {
            try {
                val entityType = if (item.mediaType == MediaType.MOVIE) EntityType.MOVIE else EntityType.TV
                val provider =
                    if (item.catalogId == "tmdb") MediaProvider.Tmdb else MediaProvider.Custom(item.catalogId)
                val key = MediaKey(provider, entityType, item.mediaId)
                mediaCatalog.getMediaDetails(key) // This fetches and caches quietly
            } catch (e: Exception) {
                // Ignore background fetch errors
            }
        }

        // 3. Добавляем задачи в очередь синхронизации
        try {
            val now = Clock.System.now()

            for (provider in oauthProviders) {
                val serviceName = provider.serviceName
                // Проверяем наличие токена для данного сервиса
                val token = userExternalAuthRepository.getToken(
                    item.userId,
                    IntegrationService.fromId(serviceName) ?: IntegrationService.TRAKT
                )
                if (token != null) {
                    val actions = mutableListOf<SyncAction>()

                    if (item.status == MediaStatus.COMPLETED) {
                        actions.add(SyncAction.WATCH)
                    } else if (item.progressSeconds > 0 && item.durationSeconds > 0) {
                        actions.add(SyncAction.PROGRESS)
                    }

                    if (item.userRating != null && item.userRating!! > 0) {
                        actions.add(SyncAction.RATE)
                    }

                    if (item.inCollection) {
                        actions.add(SyncAction.COLLECT)
                    } else {
                        actions.add(SyncAction.UNCOLLECT)
                    }

                    for (act in actions) {
                        val queueItem = UserMediaSyncQueueItem(
                            id = Uuid.random(),
                            userId = item.userId,
                            mediaType = item.mediaType,
                            mediaId = item.mediaId,
                            service = serviceName,
                            action = act,
                            progressSeconds = item.progressSeconds,
                            durationSeconds = item.durationSeconds,
                            rating = item.userRating,
                            status = SyncStatus.PENDING,
                            createdAt = now,
                            updatedAt = now
                        )
                        userMovieRepository.addToSyncQueue(queueItem)
                        userMovieRepository.updateSyncStatus(
                            userId = item.userId,
                            mediaId = item.mediaId,
                            mediaType = item.mediaType,
                            service = serviceName,
                            status = SyncStatus.PENDING
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    suspend fun updateUserEpisode(item: UserEpisodeItem): Boolean {
        // 1. Сохраняем локально в БД
        val saved = userMovieRepository.updateUserEpisode(item)
        if (!saved) return false

        // 2. Добавляем задачи в очередь синхронизации
        try {
            val now = Clock.System.now()

            for (provider in oauthProviders) {
                val serviceName = provider.serviceName
                val token = userExternalAuthRepository.getToken(
                    item.userId,
                    IntegrationService.fromId(serviceName) ?: IntegrationService.TRAKT
                )
                if (token != null) {
                    val actions = mutableListOf<SyncAction>()

                    if (item.isWatched) {
                        actions.add(SyncAction.WATCH)
                    } else if (item.progressSeconds > 0 && item.durationSeconds > 0) {
                        actions.add(SyncAction.PROGRESS)
                    }

                    if (item.inCollection) {
                        actions.add(SyncAction.COLLECT)
                    } else {
                        actions.add(SyncAction.UNCOLLECT)
                    }

                    for (act in actions) {
                        val queueItem = UserMediaSyncQueueItem(
                            id = Uuid.random(),
                            userId = item.userId,
                            mediaType = MediaType.TV,
                            mediaId = item.mediaId,
                            service = serviceName,
                            action = act,
                            progressSeconds = item.progressSeconds,
                            durationSeconds = item.durationSeconds,
                            rating = null,
                            season = item.season,
                            episode = item.episode,
                            status = SyncStatus.PENDING,
                            createdAt = now,
                            updatedAt = now
                        )
                        userMovieRepository.addToSyncQueue(queueItem)
                        userMovieRepository.updateSyncStatus(
                            userId = item.userId,
                            mediaId = item.mediaId,
                            mediaType = MediaType.TV,
                            service = serviceName,
                            status = SyncStatus.PENDING
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    /**
     * Обновить только рейтинг фильма. Если записи нет — создаём со статусом WATCHING.
     */
    suspend fun setRating(userId: Uuid, mediaId: String, rating: Int) {
        val existing = userMovieRepository.getUserMovies(userId)
            .firstOrNull { it.mediaId == mediaId }

        val now = Clock.System.now()
        val item = existing?.copy(userRating = rating, lastWatchedAt = now)
            ?: UserMovieItem(
                id = Uuid.random(),
                userId = userId,
                catalogId = "",
                mediaId = mediaId,
                mediaType = MediaType.MOVIE,
                status = MediaStatus.NONE,
                userRating = rating,
                lastWatchedAt = now
            )
        updateUserMovie(item)
    }

    /**
     * Обновить только статус фильма. Если записи нет — создаём с пустым рейтингом.
     */
    suspend fun setStatus(userId: Uuid, mediaId: String, status: MediaStatus) {
        val existing = userMovieRepository.getUserMovies(userId)
            .firstOrNull { it.mediaId == mediaId }

        val now = Clock.System.now()
        val item = existing?.copy(status = status, lastWatchedAt = now)
            ?: UserMovieItem(
                id = Uuid.random(),
                userId = userId,
                catalogId = "",
                mediaId = mediaId,
                mediaType = MediaType.MOVIE,
                status = status,
                lastWatchedAt = now
            )
        updateUserMovie(item)
    }

    /**
     * Обновить только флаг коллекции фильма. Если записи нет — создаём со статусом WATCHING.
     */
    suspend fun setInCollection(userId: Uuid, mediaId: String, inCollection: Boolean) {
        val existing = userMovieRepository.getUserMovies(userId)
            .firstOrNull { it.mediaId == mediaId }

        val now = Clock.System.now()
        val item = existing?.copy(inCollection = inCollection, lastWatchedAt = now)
            ?: UserMovieItem(
                id = Uuid.random(),
                userId = userId,
                catalogId = "tmdb",
                mediaId = mediaId,
                mediaType = MediaType.MOVIE,
                status = MediaStatus.NONE,
                userRating = null,
                progressSeconds = 0,
                durationSeconds = 0,
                inCollection = inCollection,
                lastWatchedAt = now
            )
        updateUserMovie(item)
    }
}
