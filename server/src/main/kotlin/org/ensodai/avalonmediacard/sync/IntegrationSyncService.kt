package org.ensodai.avalonmediacard.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ensodai.avalonmediacard.auth.ExternalDataSyncProvider
import org.ensodai.avalonmediacard.auth.ExternalShowProgressProvider
import org.ensodai.avalonmediacard.auth.OAuthProvider
import org.ensodai.avalonmediacard.auth.TraktSettings
import org.ensodai.avalonmediacard.contract.model.IntegrationService
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.sync.SyncStatus
import org.ensodai.avalonmediacard.plugin.CoreIntegrations
import org.ensodai.avalonmediacard.repository.UserExternalAuthRepository
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.ensodai.avalonmediacard.sync.processors.*
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

const val GLOBAL_SYNC_MEDIA_ID = "all"

@Single
class IntegrationSyncService(
    private val userMovieRepository: UserMovieRepository,
    private val userExternalAuthRepository: UserExternalAuthRepository,
    private val oauthProviders: List<OAuthProvider>,
    private val syncProviders: List<ExternalDataSyncProvider>,
    private val historySyncProcessor: HistorySyncProcessor,
    private val ratingSyncProcessor: RatingSyncProcessor,
    private val watchlistSyncProcessor: WatchlistSyncProcessor,
    private val collectionSyncProcessor: CollectionSyncProcessor,
    private val customListsSyncProcessor: CustomListsSyncProcessor,
    private val coreIntegrations: CoreIntegrations
) {
    private val logger = LoggerFactory.getLogger(IntegrationSyncService::class.java)

    suspend fun sync(userId: Uuid, service: IntegrationService): Boolean = withContext(Dispatchers.IO) {
        val oauthProvider = oauthProviders.find { it.serviceName.equals(service.id, ignoreCase = true) }
        val syncProvider = syncProviders.find { it.serviceName.equals(service.id, ignoreCase = true) }
        if (oauthProvider == null || syncProvider == null) {
            logger.error("Unsupported sync service: ${service.id}")
            return@withContext false
        }

        val auth = userExternalAuthRepository.getToken(userId, service)
        if (auth == null) {
            logger.warn("Credentials not found for service ${service.id} and user $userId")
            return@withContext false
        }

        // 1. Проверяем и обновляем токен при необходимости
        val accessToken = try {
            getOrRefreshAccessToken(oauthProvider, auth, service)
        } catch (e: Exception) {
            logger.error("Failed to refresh token during sync", e)
            return@withContext false
        } ?: return@withContext false

        // 2. Получаем настройки интеграции из общего типизированного интерфейса настроек ядра
        val syncHistory = coreIntegrations.settings.getBoolean("trakt_sync_history", true)
        val syncRatings = coreIntegrations.settings.getBoolean("trakt_sync_ratings", true)
        val syncWatchlist = coreIntegrations.settings.getBoolean("trakt_sync_watchlist", true)
        val syncCollection = coreIntegrations.settings.getBoolean("trakt_sync_collection", true)

        val settings = TraktSettings(
            syncHistory = syncHistory,
            syncRatings = syncRatings,
            syncWatchlist = syncWatchlist,
            syncCollection = syncCollection,
            syncLists = true
        )

        logger.info("Starting sync for user $userId on service ${service.id}. Settings: $settings")
        userMovieRepository.updateSyncStatus(
            userId,
            GLOBAL_SYNC_MEDIA_ID,
            MediaType.MOVIE,
            service.id,
            SyncStatus.PENDING
        )

        try {
            // Определение первого синка
            val isFirstSync = userMovieRepository.getSyncStatuses(userId, GLOBAL_SYNC_MEDIA_ID)
                .find { it.service == service.id }?.lastSyncedAt == null

            // 3. Скачиваем все данные из внешнего сервиса
            val externalData = syncProvider.fetchUserData(accessToken)

            // 4. Скачиваем все локальные данные
            val localMovies = userMovieRepository.getUserMovies(userId)
            val localEpisodes = localMovies.filter { it.mediaType == MediaType.TV }.flatMap {
                userMovieRepository.getUserEpisodes(userId, it.mediaId)
            }

            val context = SyncContext(
                userId = userId,
                service = service,
                isFirstSync = isFirstSync,
                accessToken = accessToken,
                syncProvider = syncProvider,
                settings = settings
            )

            // Запускаем умные процессоры синхронизации
            val ratingsToPush = ratingSyncProcessor.sync(context, localMovies, externalData.ratings)
            val historyToPush = historySyncProcessor.sync(context, localMovies, localEpisodes, externalData.history)
            val watchlistToPush = watchlistSyncProcessor.sync(context, localMovies, externalData.watchlist)
            val collectionToPush =
                collectionSyncProcessor.sync(context, localMovies, localEpisodes, externalData.collection)

            // Синхронизация кастомных списков
            customListsSyncProcessor.sync(context)

            // 5. Отправляем локальные изменения во внешний сервис (если они есть)
            if (historyToPush.isNotEmpty() || ratingsToPush.isNotEmpty() || watchlistToPush.isNotEmpty() || collectionToPush.isNotEmpty()) {
                logger.info("Pushing local updates to ${service.id}. History: ${historyToPush.size}, Ratings: ${ratingsToPush.size}, Watchlist: ${watchlistToPush.size}, Collection: ${collectionToPush.size}")
                val pushSuccess = syncProvider.pushUserData(
                    accessToken,
                    historyToPush,
                    ratingsToPush,
                    watchlistToPush,
                    collectionToPush
                )
                if (!pushSuccess) {
                    logger.warn("Failed to push some updates to ${service.id}")
                }
            }

            // === Е. Синхронизация Прогресса Сериалов (Show Watched Progress Cache) ===
            try {
                val progressProvider = syncProvider as? ExternalShowProgressProvider
                if (progressProvider != null) {
                    val tvShows = localMovies.filter { it.mediaType == MediaType.TV }
                    logger.info("Caching show watched progress for ${tvShows.size} shows")
                    tvShows.forEach { show ->
                        val showTmdbId = show.mediaId.toIntOrNull()
                        if (showTmdbId != null) {
                            val progress = progressProvider.getShowWatchedProgress(accessToken, showTmdbId)
                            userMovieRepository.saveCachedShowWatchedProgress(userId, showTmdbId, progress)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error during show watched progress caching", e)
            }

            userMovieRepository.updateSyncStatus(
                userId = userId,
                mediaId = GLOBAL_SYNC_MEDIA_ID,
                mediaType = MediaType.MOVIE,
                service = service.id,
                status = SyncStatus.SUCCESS,
                lastSyncedAt = Clock.System.now()
            )
            logger.info("Sync completed successfully for user $userId on service ${service.id}")
            true
        } catch (e: Exception) {
            logger.error("Error during sync with service ${service.id}", e)
            userMovieRepository.updateSyncStatus(
                userId = userId,
                mediaId = GLOBAL_SYNC_MEDIA_ID,
                mediaType = MediaType.MOVIE,
                service = service.id,
                status = SyncStatus.FAILED,
                lastSyncedAt = Clock.System.now(),
                errorMessage = e.message ?: "Sync error"
            )
            false
        }
    }

    private suspend fun getOrRefreshAccessToken(
        provider: OAuthProvider,
        auth: org.ensodai.avalonmediacard.repository.UserExternalAuth,
        service: IntegrationService
    ): String? {
        val now = Clock.System.now()
        val expiresIn = auth.expiresIn ?: 0L
        val expiryTime = auth.updatedAt.plus(expiresIn.seconds)
        val threshold = now.plus(86400.seconds)

        if (expiryTime < threshold && auth.refreshToken != null) {
            logger.info("Token is expiring soon for user ${auth.userId} on service ${provider.serviceName}. Refreshing...")
            val tokenResponse = provider.refreshToken(auth.refreshToken)
            userExternalAuthRepository.saveToken(
                userId = auth.userId,
                service = service,
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresIn = tokenResponse.expiresIn
            )
            return tokenResponse.accessToken
        }
        return auth.accessToken
    }
}
