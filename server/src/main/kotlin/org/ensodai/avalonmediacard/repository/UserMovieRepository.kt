package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.ensodai.avalonmediacard.auth.WatchedProgress
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.plugins.UserMovieProvider
import org.ensodai.avalonmediacard.contract.sync.SyncServiceStatus
import org.ensodai.avalonmediacard.contract.sync.SyncStatus
import org.ensodai.avalonmediacard.contract.sync.UserMediaSyncQueueItem
import org.ensodai.avalonmediacard.database.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid


sealed class UserMovieEvent {
    object Changed : UserMovieEvent()
}

@Single
class UserMovieRepository : UserMovieProvider {
    private val _updates = MutableSharedFlow<UserMovieEvent>(extraBufferCapacity = 64)
    val updates = _updates.asSharedFlow()

    override suspend fun getUserMovies(userId: Uuid): List<UserMovieItem> = dbQuery {
        val query = (UserMovieTable innerJoin MediaTable)
            .selectAll()
            .where { UserMovieTable.userId eq userId }
            .orderBy(UserMovieTable.lastWatchedAt to SortOrder.DESC)

        query.map {
            UserMovieItem(
                id = it[UserMovieTable.id].value,
                userId = it[UserMovieTable.userId],
                catalogId = it[MediaTable.catalogId],
                mediaId = it[MediaTable.externalId], // return external ID to client
                mediaType = it[UserMovieTable.mediaType],
                status = it[UserMovieTable.status],
                userRating = it[UserMovieTable.userRating],
                progressSeconds = it[UserMovieTable.progressSeconds],
                durationSeconds = it[UserMovieTable.durationSeconds],
                inCollection = it[UserMovieTable.inCollection],
                lastWatchedAt = it[UserMovieTable.lastWatchedAt],
                lastSourceProviderId = it[UserMovieTable.lastSourceProviderId],
                lastSourceId = it[UserMovieTable.lastSourceId],
                lastSourcePayload = it[UserMovieTable.lastSourcePayload]
            )
        }
    }

    override fun observeUserMovies(userId: Uuid): kotlinx.coroutines.flow.Flow<List<UserMovieItem>> =
        kotlinx.coroutines.flow.flow {
            emit(getUserMovies(userId))
            updates.collect {
                emit(getUserMovies(userId))
            }
        }

    override suspend fun updateUserMovie(item: UserMovieItem): Boolean = dbQuery {
            try {
                // Ensure media exists in MediaTable to satisfy foreign key
                MediaTable.insertIgnore {
                    it[id] = Uuid.random()
                    it[catalogId] = item.catalogId.ifEmpty { "tmdb" }
                    it[externalId] = item.mediaId
                    it[mediaType] = if (item.mediaType.name.equals("MOVIE", ignoreCase = true)) "movie" else "tv"
                }

                val internalMediaId = MediaTable.selectAll()
                    .where { MediaTable.externalId eq item.mediaId }
                    .limit(1)
                    .map { it[MediaTable.id].value }
                    .singleOrNull() ?: return@dbQuery false
                val exists = UserMovieTable.selectAll()
                    .where { (UserMovieTable.userId eq item.userId) and (UserMovieTable.mediaId eq internalMediaId) and (UserMovieTable.mediaType eq item.mediaType) }
                    .any()

                if (exists) {
                    UserMovieTable.update({ (UserMovieTable.userId eq item.userId) and (UserMovieTable.mediaId eq internalMediaId) and (UserMovieTable.mediaType eq item.mediaType) }) {
                        it[status] = item.status
                        it[userRating] = item.userRating
                        it[progressSeconds] = item.progressSeconds
                        it[durationSeconds] = item.durationSeconds
                        it[inCollection] = item.inCollection
                        it[lastWatchedAt] = item.lastWatchedAt
                        it[lastSourceProviderId] = item.lastSourceProviderId
                        it[lastSourceId] = item.lastSourceId
                        it[lastSourcePayload] = item.lastSourcePayload
                    }
                } else {
                    UserMovieTable.insert {
                        it[id] = Uuid.random()
                        it[userId] = item.userId
                        it[mediaId] = internalMediaId
                        it[mediaType] = item.mediaType
                        it[status] = item.status
                        it[userRating] = item.userRating
                        it[progressSeconds] = item.progressSeconds
                        it[durationSeconds] = item.durationSeconds
                        it[inCollection] = item.inCollection
                        it[lastWatchedAt] = item.lastWatchedAt
                        it[lastSourceProviderId] = item.lastSourceProviderId
                        it[lastSourceId] = item.lastSourceId
                        it[lastSourcePayload] = item.lastSourcePayload
                    }
                }
                _updates.emit(UserMovieEvent.Changed)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    override suspend fun deleteUserMovie(userId: Uuid, mediaId: String): Boolean = dbQuery {
        try {
            val internalMediaId = MediaTable.selectAll()
                .where { MediaTable.externalId eq mediaId }
                .limit(1)
                .map { it[MediaTable.id].value }
                .singleOrNull() ?: return@dbQuery false

            val deletedRows = UserMovieTable.deleteWhere {
                (UserMovieTable.userId eq userId) and (UserMovieTable.mediaId eq internalMediaId)
            }
            if (deletedRows > 0) {
                _updates.emit(UserMovieEvent.Changed)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getUserEpisodes(userId: Uuid, mediaId: String): List<UserEpisodeItem> = dbQuery {
        val internalMediaId = MediaTable.selectAll()
            .where { MediaTable.externalId eq mediaId }
            .limit(1)
            .map { it[MediaTable.id].value }
            .singleOrNull() ?: return@dbQuery emptyList()

        (UserEpisodeTable innerJoin MediaEpisodeTable innerJoin MediaSeasonTable)
            .selectAll()
            .where { (UserEpisodeTable.userId eq userId) and (MediaSeasonTable.mediaId eq internalMediaId) }
            .orderBy(MediaSeasonTable.seasonNumber to SortOrder.ASC, MediaEpisodeTable.episodeNumber to SortOrder.ASC)
            .map {
                UserEpisodeItem(
                    id = it[UserEpisodeTable.id].value,
                    userId = it[UserEpisodeTable.userId],
                    catalogId = "",
                    mediaId = mediaId,
                    season = it[MediaSeasonTable.seasonNumber],
                    episode = it[MediaEpisodeTable.episodeNumber],
                    progressSeconds = it[UserEpisodeTable.progressSeconds],
                    durationSeconds = it[UserEpisodeTable.durationSeconds],
                    isWatched = it[UserEpisodeTable.isWatched],
                    inCollection = it[UserEpisodeTable.inCollection],
                    userRating = it[UserEpisodeTable.userRating],
                    lastWatchedAt = it[UserEpisodeTable.lastWatchedAt],
                    lastSourceProviderId = it[UserEpisodeTable.lastSourceProviderId],
                    lastSourceId = it[UserEpisodeTable.lastSourceId],
                    lastSourcePayload = it[UserEpisodeTable.lastSourcePayload]
                )
            }
    }

    override suspend fun updateUserEpisode(item: UserEpisodeItem): Boolean = dbQuery {
        try {
            val internalMediaId = MediaTable.selectAll()
                .where { MediaTable.externalId eq item.mediaId }
                .limit(1)
                .map { it[MediaTable.id].value }
                .singleOrNull() ?: return@dbQuery false

            val episodeId = (MediaEpisodeTable innerJoin MediaSeasonTable)
                .selectAll()
                .where {
                    (MediaSeasonTable.mediaId eq internalMediaId) and
                            (MediaSeasonTable.seasonNumber eq item.season) and
                            (MediaEpisodeTable.episodeNumber eq item.episode)
                }
                .singleOrNull()
                ?.get(MediaEpisodeTable.id)
                ?.value ?: return@dbQuery false

            val exists = UserEpisodeTable.selectAll()
                .where {
                    (UserEpisodeTable.userId eq item.userId) and
                            (UserEpisodeTable.episodeId eq episodeId)
                }
                .any()

            if (exists) {
                UserEpisodeTable.update({
                    (UserEpisodeTable.userId eq item.userId) and
                            (UserEpisodeTable.episodeId eq episodeId)
                }) {
                    it[progressSeconds] = item.progressSeconds
                    it[durationSeconds] = item.durationSeconds
                    it[isWatched] = item.isWatched
                    it[inCollection] = item.inCollection
                    it[userRating] = item.userRating
                    it[lastWatchedAt] = item.lastWatchedAt
                    it[lastSourceProviderId] = item.lastSourceProviderId
                    it[lastSourceId] = item.lastSourceId
                    it[lastSourcePayload] = item.lastSourcePayload
                }
            } else {
                UserEpisodeTable.insert {
                    it[id] = Uuid.random()
                    it[userId] = item.userId
                    it[UserEpisodeTable.episodeId] = episodeId
                    it[progressSeconds] = item.progressSeconds
                    it[durationSeconds] = item.durationSeconds
                    it[isWatched] = item.isWatched
                    it[inCollection] = item.inCollection
                    it[userRating] = item.userRating
                    it[lastWatchedAt] = item.lastWatchedAt
                    it[lastSourceProviderId] = item.lastSourceProviderId
                    it[lastSourceId] = item.lastSourceId
                    it[lastSourcePayload] = item.lastSourcePayload
                }
            }
            _updates.emit(UserMovieEvent.Changed)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addToSyncQueue(item: UserMediaSyncQueueItem): Boolean = dbQuery {
        try {
            val internalMediaId = MediaTable.selectAll()
                .where { MediaTable.externalId eq item.mediaId }
                .limit(1)
                .map { it[MediaTable.id].value }
                .singleOrNull() ?: return@dbQuery false

            val seasonNum = item.season
            val episodeNum = item.episode
            val episodeId = if (seasonNum != null && episodeNum != null) {
                (MediaEpisodeTable innerJoin MediaSeasonTable)
                    .selectAll()
                    .where {
                        (MediaSeasonTable.mediaId eq internalMediaId) and
                                (MediaSeasonTable.seasonNumber eq seasonNum) and
                                (MediaEpisodeTable.episodeNumber eq episodeNum)
                    }
                    .singleOrNull()?.get(MediaEpisodeTable.id)?.value
            } else null

            UserMediaSyncQueueTable.insert {
                it[id] = item.id
                it[userId] = item.userId
                it[mediaType] = item.mediaType
                it[mediaId] = internalMediaId
                it[service] = item.service
                it[action] = item.action
                it[progressSeconds] = item.progressSeconds
                it[durationSeconds] = item.durationSeconds
                it[rating] = item.rating
                it[this.episodeId] = episodeId
                it[status] = item.status
                it[attempts] = item.attempts
                it[lastAttemptAt] = item.lastAttemptAt
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getPendingSyncItems(): List<UserMediaSyncQueueItem> = dbQuery {
        (UserMediaSyncQueueTable innerJoin MediaTable leftJoin MediaEpisodeTable leftJoin MediaSeasonTable).selectAll()
            .where {
                (UserMediaSyncQueueTable.status eq SyncStatus.PENDING) or
                        ((UserMediaSyncQueueTable.status eq SyncStatus.FAILED) and (UserMediaSyncQueueTable.attempts less 5))
            }
            .map {
                UserMediaSyncQueueItem(
                    id = it[UserMediaSyncQueueTable.id].value,
                    userId = it[UserMediaSyncQueueTable.userId],
                    mediaType = it[UserMediaSyncQueueTable.mediaType],
                    mediaId = it[MediaTable.externalId],
                    service = it[UserMediaSyncQueueTable.service],
                    action = it[UserMediaSyncQueueTable.action],
                    progressSeconds = it[UserMediaSyncQueueTable.progressSeconds],
                    durationSeconds = it[UserMediaSyncQueueTable.durationSeconds],
                    rating = it[UserMediaSyncQueueTable.rating],
                    season = it.getOrNull(MediaSeasonTable.seasonNumber),
                    episode = it.getOrNull(MediaEpisodeTable.episodeNumber),
                    status = it[UserMediaSyncQueueTable.status],
                    attempts = it[UserMediaSyncQueueTable.attempts],
                    lastAttemptAt = it[UserMediaSyncQueueTable.lastAttemptAt],
                    createdAt = it[UserMediaSyncQueueTable.createdAt],
                    updatedAt = it[UserMediaSyncQueueTable.updatedAt]
                )
            }
    }

    suspend fun updateSyncItemStatus(
        id: Uuid,
        status: SyncStatus,
        attempts: Int,
        lastAttemptAt: Instant?
    ): Boolean = dbQuery {
        try {
            val rows = UserMediaSyncQueueTable.update({ UserMediaSyncQueueTable.id eq id }) {
                it[UserMediaSyncQueueTable.status] = status
                it[UserMediaSyncQueueTable.attempts] = attempts
                it[UserMediaSyncQueueTable.lastAttemptAt] = lastAttemptAt
                it[updatedAt] = Clock.System.now()
            }
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getSyncStatuses(userId: Uuid, mediaId: String): List<SyncServiceStatus> = dbQuery {
        val internalMediaId = MediaTable.selectAll()
            .where { MediaTable.externalId eq mediaId }
            .limit(1)
            .map { it[MediaTable.id].value }
            .singleOrNull() ?: return@dbQuery emptyList()

        UserMediaSyncStatusTable.selectAll()
            .where { (UserMediaSyncStatusTable.userId eq userId) and (UserMediaSyncStatusTable.mediaId eq internalMediaId) }
            .map {
                SyncServiceStatus(
                    service = it[UserMediaSyncStatusTable.service],
                    status = it[UserMediaSyncStatusTable.status],
                    lastSyncedAt = it[UserMediaSyncStatusTable.lastSyncedAt],
                    errorMessage = it[UserMediaSyncStatusTable.errorMessage]
                )
            }
    }

    suspend fun updateSyncStatus(
        userId: Uuid,
        mediaId: String,
        mediaType: MediaType,
        service: String,
        status: SyncStatus,
        lastSyncedAt: Instant? = null,
        errorMessage: String? = null
    ): Boolean = dbQuery {
        try {
            val internalMediaId = MediaTable.selectAll()
                .where { MediaTable.externalId eq mediaId }
                .limit(1)
                .map { it[MediaTable.id].value }
                .singleOrNull() ?: return@dbQuery false

            val exists = UserMediaSyncStatusTable.selectAll()
                .where {
                    (UserMediaSyncStatusTable.userId eq userId) and
                            (UserMediaSyncStatusTable.mediaId eq internalMediaId) and
                            (UserMediaSyncStatusTable.service eq service)
                }
                .any()

            if (exists) {
                UserMediaSyncStatusTable.update({
                    (UserMediaSyncStatusTable.userId eq userId) and
                            (UserMediaSyncStatusTable.mediaId eq internalMediaId) and
                            (UserMediaSyncStatusTable.service eq service)
                }) {
                    it[UserMediaSyncStatusTable.status] = status
                    it[UserMediaSyncStatusTable.lastSyncedAt] = lastSyncedAt
                    it[UserMediaSyncStatusTable.errorMessage] = errorMessage
                    it[updatedAt] = Clock.System.now()
                }
            } else {
                UserMediaSyncStatusTable.insert {
                    it[id] = Uuid.random()
                    it[UserMediaSyncStatusTable.userId] = userId
                    it[UserMediaSyncStatusTable.mediaType] = mediaType
                    it[UserMediaSyncStatusTable.mediaId] = internalMediaId
                    it[UserMediaSyncStatusTable.service] = service
                    it[UserMediaSyncStatusTable.status] = status
                    it[UserMediaSyncStatusTable.lastSyncedAt] = lastSyncedAt
                    it[UserMediaSyncStatusTable.errorMessage] = errorMessage
                }
            }
            _updates.emit(UserMovieEvent.Changed)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getCachedShowWatchedProgress(userId: Uuid, showTmdbId: Int): CachedShowProgress? = dbQuery {
        UserShowProgressTable.selectAll()
            .where { (UserShowProgressTable.userId eq userId) and (UserShowProgressTable.showTmdbId eq showTmdbId) }
            .map {
                CachedShowProgress(
                    progress = org.ensodai.avalonmediacard.auth.WatchedProgress(
                        season = it[UserShowProgressTable.nextSeason],
                        number = it[UserShowProgressTable.nextEpisode],
                        title = it[UserShowProgressTable.title],
                        tmdbId = it[UserShowProgressTable.nextEpisodeTmdbId]
                    ),
                    updatedAt = it[UserShowProgressTable.updatedAt]
                )
            }
            .singleOrNull()
    }

    suspend fun saveCachedShowWatchedProgress(
        userId: Uuid,
        showTmdbId: Int,
        progress: org.ensodai.avalonmediacard.auth.WatchedProgress?
    ) = dbQuery {
        if (progress == null) {
            UserShowProgressTable.deleteWhere {
                (UserShowProgressTable.userId eq userId) and (UserShowProgressTable.showTmdbId eq showTmdbId)
            }
        } else {
            val exists = UserShowProgressTable.selectAll()
                .where { (UserShowProgressTable.userId eq userId) and (UserShowProgressTable.showTmdbId eq showTmdbId) }
                .any()

            val now = Clock.System.now()
            if (exists) {
                UserShowProgressTable.update({ (UserShowProgressTable.userId eq userId) and (UserShowProgressTable.showTmdbId eq showTmdbId) }) {
                    it[nextSeason] = progress.season
                    it[nextEpisode] = progress.number
                    it[title] = progress.title
                    it[nextEpisodeTmdbId] = progress.tmdbId
                    it[updatedAt] = now
                }
            } else {
                UserShowProgressTable.insert {
                    it[id] = Uuid.random()
                    it[UserShowProgressTable.userId] = userId
                    it[UserShowProgressTable.showTmdbId] = showTmdbId
                    it[nextSeason] = progress.season
                    it[nextEpisode] = progress.number
                    it[title] = progress.title
                    it[nextEpisodeTmdbId] = progress.tmdbId
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
        _updates.emit(UserMovieEvent.Changed)
    }

    override suspend fun notifyUpdate() {
        _updates.emit(UserMovieEvent.Changed)
    }
}

data class CachedShowProgress(
    val progress: WatchedProgress,
    val updatedAt: Instant
)
