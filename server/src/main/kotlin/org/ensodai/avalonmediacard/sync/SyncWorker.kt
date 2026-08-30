package org.ensodai.avalonmediacard.sync

import kotlinx.coroutines.*
import org.ensodai.avalonmediacard.auth.ExternalDataSyncProvider
import org.ensodai.avalonmediacard.auth.OAuthProvider
import org.ensodai.avalonmediacard.contract.model.IntegrationService
import org.ensodai.avalonmediacard.contract.sync.SyncStatus
import org.ensodai.avalonmediacard.contract.sync.UserMediaSyncQueueItem
import org.ensodai.avalonmediacard.repository.UserExternalAuthRepository
import org.ensodai.avalonmediacard.repository.UserMovieRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Single
class SyncWorker(
    private val userMovieRepository: UserMovieRepository,
    private val userExternalAuthRepository: UserExternalAuthRepository,
    private val oauthProviders: List<OAuthProvider>,
    private val syncProviders: List<ExternalDataSyncProvider>
) {
    private val logger = LoggerFactory.getLogger(SyncWorker::class.java)
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("SyncWorkerScope"))

    fun start() {
        if (job != null) return
        logger.info("Starting SyncWorker background loop...")
        job = scope.launch {
            while (isActive) {
                try {
                    processQueue()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Error in sync queue processing loop", e)
                }
                delay(30_000.milliseconds) // Запуск каждые 30 секунд
            }
        }
    }

    fun stop() {
        logger.info("Stopping SyncWorker...")
        job?.cancel()
        job = null
    }

    private suspend fun processQueue() {
        val pendingItems = userMovieRepository.getPendingSyncItems()
        if (pendingItems.isEmpty()) return

        logger.info("Found ${pendingItems.size} pending items in sync queue.")

        for (item in pendingItems) {
            try {
                val success = processSyncItem(item)

                val now = Clock.System.now()
                if (success) {
                    logger.info("Successfully synced queue item ${item.id}")
                    userMovieRepository.updateSyncItemStatus(
                        id = item.id,
                        status = SyncStatus.SUCCESS,
                        attempts = item.attempts + 1,
                        lastAttemptAt = now
                    )
                    userMovieRepository.updateSyncStatus(
                        userId = item.userId,
                        mediaId = item.mediaId,
                        mediaType = item.mediaType,
                        service = item.service,
                        status = SyncStatus.SUCCESS,
                        lastSyncedAt = now
                    )
                } else {
                    val nextAttempts = item.attempts + 1
                    val nextStatus = if (nextAttempts >= 5) {
                        logger.warn("Sync queue item ${item.id} exceeded maximum attempts (5). Setting to FAILED permanently.")
                        SyncStatus.FAILED
                    } else {
                        SyncStatus.FAILED
                    }
                    userMovieRepository.updateSyncItemStatus(
                        id = item.id,
                        status = nextStatus,
                        attempts = nextAttempts,
                        lastAttemptAt = now
                    )
                    userMovieRepository.updateSyncStatus(
                        userId = item.userId,
                        mediaId = item.mediaId,
                        mediaType = item.mediaType,
                        service = item.service,
                        status = nextStatus,
                        lastSyncedAt = now,
                        errorMessage = "Failed after $nextAttempts attempts."
                    )
                }
            } catch (e: Exception) {
                logger.error("Exception processing sync queue item ${item.id}", e)
                val now = Clock.System.now()
                userMovieRepository.updateSyncItemStatus(
                    id = item.id,
                    status = SyncStatus.FAILED,
                    attempts = item.attempts + 1,
                    lastAttemptAt = now
                )
                userMovieRepository.updateSyncStatus(
                    userId = item.userId,
                    mediaId = item.mediaId,
                    mediaType = item.mediaType,
                    service = item.service,
                    status = SyncStatus.FAILED,
                    lastSyncedAt = now,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }

    private suspend fun processSyncItem(item: UserMediaSyncQueueItem): Boolean {
        val oauthProvider = oauthProviders.find { it.serviceName.equals(item.service, ignoreCase = true) }
        val syncProvider = syncProviders.find { it.serviceName.equals(item.service, ignoreCase = true) }
        if (oauthProvider == null || syncProvider == null) {
            logger.error("Unsupported sync service: ${item.service} for item ${item.id}")
            return false
        }

        // 1. Получаем токен авторизации
        val auth = userExternalAuthRepository.getToken(
            item.userId,
            IntegrationService.fromId(oauthProvider.serviceName) ?: IntegrationService.TRAKT
        )
        if (auth == null) {
            logger.warn("Credentials not found for service ${oauthProvider.serviceName} and user ${item.userId}")
            return false
        }

        // 2. Проверяем срок действия токена и обновляем при необходимости
        val accessToken = getOrRefreshAccessToken(oauthProvider, auth) ?: return false

        // 3. Вызываем метод синхронизации у провайдера
        return syncProvider.syncMediaItem(
            accessToken = accessToken,
            action = item.action,
            mediaType = item.mediaType,
            mediaId = item.mediaId,
            progressSeconds = item.progressSeconds,
            durationSeconds = item.durationSeconds,
            rating = item.rating,
            season = item.season,
            episode = item.episode
        )
    }

    private suspend fun getOrRefreshAccessToken(
        provider: OAuthProvider,
        auth: org.ensodai.avalonmediacard.repository.UserExternalAuth
    ): String? {
        val now = Clock.System.now()
        val expiresIn = auth.expiresIn ?: 0L
        val expiryTime = auth.updatedAt.plus(expiresIn.seconds)

        // Если токен истекает менее чем через 1 день (86400 секунд), то обновляем его
        val threshold = now.plus(86400.seconds)
        if (expiryTime < threshold && auth.refreshToken != null) {
            logger.info("Token is expiring soon for user ${auth.userId} on service ${provider.serviceName}. Refreshing...")
            return try {
                val tokenResponse = provider.refreshToken(auth.refreshToken)
                userExternalAuthRepository.saveToken(
                    userId = auth.userId,
                    service = IntegrationService.fromId(provider.serviceName) ?: IntegrationService.TRAKT,
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresIn = tokenResponse.expiresIn
                )
                logger.info("Successfully refreshed token for user ${auth.userId} on service ${provider.serviceName}")
                tokenResponse.accessToken
            } catch (e: Exception) {
                logger.error("Failed to refresh token for user ${auth.userId} on service ${provider.serviceName}", e)
                // Если не получилось обновить, пробуем использовать старый (вдруг еще работает)
                auth.accessToken
            }
        }
        return auth.accessToken
    }
}
