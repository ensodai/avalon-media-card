package org.ensodai.avalonmediacard.auth

import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.model.MediaType
import kotlin.time.Instant

data class ExternalUserData(
    val history: List<ExternalHistoryItem>,
    val ratings: List<ExternalRatingItem>,
    val watchlist: List<ExternalWatchlistItem>,
    val collection: List<ExternalCollectionItem> = emptyList()
)

data class ExternalHistoryItem(
    val tmdbId: Int,
    val mediaType: MediaType,
    val watchedAt: Instant,
    val season: Int? = null,
    val episode: Int? = null
)

data class ExternalRatingItem(
    val tmdbId: Int,
    val mediaType: MediaType,
    val rating: Int, // 1-10
    val ratedAt: Instant,
    val season: Int? = null,
    val episode: Int? = null
)

data class ExternalWatchlistItem(
    val tmdbId: Int,
    val mediaType: MediaType,
    val addedAt: Instant
)

data class ExternalCollectionItem(
    val tmdbId: Int,
    val mediaType: MediaType,
    val addedAt: Instant,
    val season: Int? = null,
    val episode: Int? = null
)

@Serializable
data class TraktSettings(
    val syncHistory: Boolean = true,
    val syncRatings: Boolean = true,
    val syncWatchlist: Boolean = true,
    val syncCollection: Boolean = true,
    val syncLists: Boolean = true
)

data class WatchedProgress(
    val season: Int,
    val number: Int,
    val title: String?,
    val tmdbId: Int?
)

data class ExternalCustomList(
    val externalListId: String,
    val slug: String,
    val name: String,
    val privacy: String,
    val items: List<ExternalCustomListItem>
)

data class ExternalCustomListItem(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String?,
    val rank: Int,
    val listedAt: Instant?
)
