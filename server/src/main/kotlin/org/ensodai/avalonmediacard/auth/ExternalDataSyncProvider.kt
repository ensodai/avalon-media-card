package org.ensodai.avalonmediacard.auth

import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.sync.SyncAction

interface ExternalDataSyncProvider {
    val serviceName: String

    suspend fun syncMediaItem(
        accessToken: String,
        action: SyncAction,
        mediaType: MediaType,
        mediaId: String,
        progressSeconds: Long?,
        durationSeconds: Long?,
        rating: Int?,
        season: Int? = null,
        episode: Int? = null
    ): Boolean = true

    suspend fun fetchUserData(accessToken: String): ExternalUserData

    suspend fun fetchUserLists(accessToken: String): List<ExternalCustomList> = emptyList()

    suspend fun createCustomList(
        accessToken: String,
        name: String,
        privacy: String
    ): ExternalCustomList? = null

    suspend fun addMediaToList(
        accessToken: String,
        externalListId: String,
        mediaType: MediaType,
        tmdbId: Int
    ): Boolean = true

    suspend fun removeMediaFromList(
        accessToken: String,
        externalListId: String,
        mediaType: MediaType,
        tmdbId: Int
    ): Boolean = true

    suspend fun pushUserData(
        accessToken: String,
        historyItems: List<ExternalHistoryItem>,
        ratingItems: List<ExternalRatingItem>,
        watchlistItems: List<ExternalWatchlistItem>,
        collectionItems: List<ExternalCollectionItem>
    ): Boolean
}
