package org.ensodai.avalonmediacard.auth

import app.moviebase.trakt.Trakt
import app.moviebase.trakt.model.*
import io.ktor.client.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.sync.SyncAction
import org.ensodai.avalonmediacard.utils.EnvHelper
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.time.Clock

@Serializable
data class TraktIds(
    val tmdb: Int
)

@Serializable
data class TraktCreateListResponse(
    val name: String,
    val ids: TraktIdsResponse
)

@Serializable
data class TraktIdsResponse(
    val trakt: Int,
    val slug: String
)

@Serializable
data class TraktMovieItem(
    val ids: TraktIds,
    @SerialName("watched_at") val watchedAt: String? = null,
    val rating: Int? = null,
    @SerialName("rated_at") val ratedAt: String? = null
)

@Serializable
data class TraktEpisodeItem(
    val number: Int,
    @SerialName("watched_at") val watchedAt: String? = null,
    val rating: Int? = null,
    @SerialName("rated_at") val ratedAt: String? = null
)

@Serializable
data class TraktSeasonItem(
    val number: Int,
    val episodes: List<TraktEpisodeItem>
)

@Serializable
data class TraktShowItem(
    val ids: TraktIds,
    val rating: Int? = null,
    @SerialName("rated_at") val ratedAt: String? = null,
    val seasons: List<TraktSeasonItem>? = null
)

@Serializable
data class TraktHistoryPayload(
    val movies: List<TraktMovieItem>? = null,
    val shows: List<TraktShowItem>? = null
)
/**
 * Провайдер синхронизации данных с Trakt.tv.
 * 
 * ВНИМАНИЕ: Временно отключен (убрана аннотация @Single), так как API Trakt
 * работает крайне медленно (ответы на добавление элементов в списки могут
 * занимать 3-5 секунд). Это приводит к таймаутам и блокировкам корутин.
 * Используется только для чтения комментариев в TraktMetadataPlugin для
 * авторизованных пользователей.
 */
class TraktDataSyncProvider(
    private val client: HttpClient
) : ExternalDataSyncProvider, ExternalShowProgressProvider {
    private val logger = LoggerFactory.getLogger(TraktDataSyncProvider::class.java)

    override val serviceName: String = "trakt"

    private val clientId: String
        get() = EnvHelper.getEnv("TRAKT_CLIENT_ID") ?: ""

    private suspend fun postTrakt(trakt: Trakt, path: String, body: JsonElement): Boolean {
        return try {
            val response = trakt.client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val success = response.status.value in 200..299
            if (!success) {
                logger.error("Trakt API request failed on path $path: Status ${response.status.value}, Body: ${response.bodyAsText()}")
            }
            success
        } catch (e: Exception) {
            logger.error("Trakt API request exception on path $path", e)
            false
        }
    }

    suspend fun syncHistoryAdd(
        trakt: Trakt,
        mediaType: MediaType,
        tmdbId: Int,
        season: Int? = null,
        episode: Int? = null
    ): Boolean {
        val payload = if (mediaType == MediaType.MOVIE) {
            TraktHistoryPayload(
                movies = listOf(TraktMovieItem(ids = TraktIds(tmdb = tmdbId)))
            )
        } else {
            val seasons = if (season != null && episode != null) {
                listOf(TraktSeasonItem(number = season, episodes = listOf(TraktEpisodeItem(number = episode))))
            } else null
            TraktHistoryPayload(
                shows = listOf(TraktShowItem(ids = TraktIds(tmdb = tmdbId), seasons = seasons))
            )
        }
        return postTrakt(trakt, "sync/history", Json.encodeToJsonElement(payload))
    }

    suspend fun syncHistoryRemove(
        trakt: Trakt,
        mediaType: MediaType,
        tmdbId: Int,
        season: Int? = null,
        episode: Int? = null
    ): Boolean {
        val payload = if (mediaType == MediaType.MOVIE) {
            TraktHistoryPayload(
                movies = listOf(TraktMovieItem(ids = TraktIds(tmdb = tmdbId)))
            )
        } else {
            val seasons = if (season != null && episode != null) {
                listOf(TraktSeasonItem(number = season, episodes = listOf(TraktEpisodeItem(number = episode))))
            } else null
            TraktHistoryPayload(
                shows = listOf(TraktShowItem(ids = TraktIds(tmdb = tmdbId), seasons = seasons))
            )
        }
        return postTrakt(trakt, "sync/history/remove", Json.encodeToJsonElement(payload))
    }

    suspend fun syncCollectionAdd(
        trakt: Trakt,
        mediaType: MediaType,
        tmdbId: Int,
        season: Int? = null,
        episode: Int? = null
    ): Boolean {
        val payload = if (mediaType == MediaType.MOVIE) {
            TraktHistoryPayload(
                movies = listOf(TraktMovieItem(ids = TraktIds(tmdb = tmdbId)))
            )
        } else {
            val seasons = if (season != null && episode != null) {
                listOf(TraktSeasonItem(number = season, episodes = listOf(TraktEpisodeItem(number = episode))))
            } else null
            TraktHistoryPayload(
                shows = listOf(TraktShowItem(ids = TraktIds(tmdb = tmdbId), seasons = seasons))
            )
        }
        return postTrakt(trakt, "sync/collection", Json.encodeToJsonElement(payload))
    }

    suspend fun syncCollectionRemove(
        trakt: Trakt,
        mediaType: MediaType,
        tmdbId: Int,
        season: Int? = null,
        episode: Int? = null
    ): Boolean {
        val payload = if (mediaType == MediaType.MOVIE) {
            TraktHistoryPayload(
                movies = listOf(TraktMovieItem(ids = TraktIds(tmdb = tmdbId)))
            )
        } else {
            val seasons = if (season != null && episode != null) {
                listOf(TraktSeasonItem(number = season, episodes = listOf(TraktEpisodeItem(number = episode))))
            } else null
            TraktHistoryPayload(
                shows = listOf(TraktShowItem(ids = TraktIds(tmdb = tmdbId), seasons = seasons))
            )
        }
        return postTrakt(trakt, "sync/collection/remove", Json.encodeToJsonElement(payload))
    }

    suspend fun syncRating(trakt: Trakt, mediaType: MediaType, tmdbId: Int, ratingValue: Int): Boolean {
        val payload = if (mediaType == MediaType.MOVIE) {
            TraktHistoryPayload(
                movies = listOf(TraktMovieItem(ids = TraktIds(tmdb = tmdbId), rating = ratingValue))
            )
        } else {
            TraktHistoryPayload(
                shows = listOf(TraktShowItem(ids = TraktIds(tmdb = tmdbId), rating = ratingValue))
            )
        }
        return postTrakt(trakt, "sync/ratings", Json.encodeToJsonElement(payload))
    }

    suspend fun syncProgress(
        trakt: Trakt,
        mediaType: MediaType,
        tmdbId: Int,
        progressPercent: Double,
        season: Int? = null,
        episode: Int? = null
    ): Boolean {
        val progressVal = progressPercent.toFloat()
        return try {
            val request = if (mediaType == MediaType.MOVIE) {
                TraktScrobbleRequest(
                    movie = TraktScrobbleMovie(ids = TraktItemIds(tmdb = tmdbId)),
                    progress = progressVal
                )
            } else {
                if (season != null && episode != null) {
                    TraktScrobbleRequest(
                        show = TraktScrobbleShow(ids = TraktItemIds(tmdb = tmdbId)),
                        episode = TraktScrobbleEpisode(season = season, number = episode),
                        progress = progressVal
                    )
                } else {
                    return false
                }
            }
            trakt.scrobble.pauseWatching(request)
            true
        } catch (e: Exception) {
            logger.error("Failed to sync progress via scrobble API", e)
            false
        }
    }

    override suspend fun syncMediaItem(
        accessToken: String,
        action: SyncAction,
        mediaType: MediaType,
        mediaId: String,
        progressSeconds: Long?,
        durationSeconds: Long?,
        rating: Int?,
        season: Int?,
        episode: Int?
    ): Boolean {
        val tmdbId = try {
            mediaId.toInt()
        } catch (e: Exception) {
            logger.error("Failed to parse mediaId as Int: $mediaId")
            return false
        }

        val trakt = Trakt {
            clientId = this@TraktDataSyncProvider.clientId
            userAuthentication {
                loadTokens {
                    BearerTokens(accessToken, "")
                }
            }
        }

        return when (action) {
            SyncAction.WATCH -> syncHistoryAdd(
                trakt = trakt,
                mediaType = mediaType,
                tmdbId = tmdbId,
                season = season,
                episode = episode
            )

            SyncAction.UNWATCH -> syncHistoryRemove(
                trakt = trakt,
                mediaType = mediaType,
                tmdbId = tmdbId,
                season = season,
                episode = episode
            )

            SyncAction.RATE -> {
                val r = rating ?: return false
                syncRating(trakt, mediaType, tmdbId, r)
            }

            SyncAction.PROGRESS -> {
                val progress = progressSeconds ?: 0L
                val duration = durationSeconds ?: 0L
                if (duration <= 0L) {
                    logger.warn("Progress sync ignored: duration is 0")
                    return false
                }
                val progressPercent = (progress.toDouble() / duration.toDouble()) * 100.0
                syncProgress(
                    trakt = trakt,
                    mediaType = mediaType,
                    tmdbId = tmdbId,
                    progressPercent = progressPercent.coerceIn(0.0, 100.0),
                    season = season,
                    episode = episode
                )
            }

            SyncAction.COLLECT -> syncCollectionAdd(
                trakt = trakt,
                mediaType = mediaType,
                tmdbId = tmdbId,
                season = season,
                episode = episode
            )

            SyncAction.UNCOLLECT -> syncCollectionRemove(
                trakt = trakt,
                mediaType = mediaType,
                tmdbId = tmdbId,
                season = season,
                episode = episode
            )
        }
    }

    override suspend fun fetchUserData(accessToken: String): ExternalUserData {
        val trakt = Trakt {
            clientId = this@TraktDataSyncProvider.clientId
            userAuthentication {
                loadTokens {
                    BearerTokens(accessToken, "")
                }
            }
        }

        return try {
            val historyMovies = trakt.runsAndCatchingError {
                trakt.users.getHistory(
                    TraktUserSlug.ME,
                    TraktListMediaType.MOVIES,
                    null,
                    null,
                    null,
                    null,
                    1,
                    1000
                ).mapNotNull { item ->
                    val tmdbId = item.movie?.ids?.tmdb ?: return@mapNotNull null
                    ExternalHistoryItem(
                        tmdbId = tmdbId,
                        mediaType = MediaType.MOVIE,
                        watchedAt = item.watchedAt ?: Clock.System.now()
                    )
                }
            }

            val historyEpisodes = trakt.runsAndCatchingError {
                trakt.users.getHistory(
                    TraktUserSlug.ME,
                    TraktListMediaType.EPISODES,
                    null,
                    null,
                    null,
                    null,
                    1,
                    1000
                ).mapNotNull { item ->
                    val showTmdbId = item.show?.ids?.tmdb ?: return@mapNotNull null
                    val ep = item.episode ?: return@mapNotNull null
                    ExternalHistoryItem(
                        tmdbId = showTmdbId,
                        mediaType = MediaType.TV,
                        watchedAt = item.watchedAt ?: Clock.System.now(),
                        season = ep.season,
                        episode = ep.number
                    )
                }
            }

            val ratingsMovies = trakt.runsAndCatchingError {
                trakt.users.getRatingsMovies(
                    TraktUserSlug.ME,
                    null,
                    1,
                    1000,
                    null
                ).mapNotNull { item ->
                    val tmdbId = item.movie?.ids?.tmdb ?: return@mapNotNull null
                    ExternalRatingItem(
                        tmdbId = tmdbId,
                        mediaType = MediaType.MOVIE,
                        rating = item.rating,
                        ratedAt = item.ratedAt ?: Clock.System.now()
                    )
                }
            }

            val ratingsShows = trakt.runsAndCatchingError {
                trakt.users.getRatingsShows(
                    TraktUserSlug.ME,
                    null,
                    1,
                    1000,
                    null
                ).mapNotNull { item ->
                    val tmdbId = item.show?.ids?.tmdb ?: return@mapNotNull null
                    ExternalRatingItem(
                        tmdbId = tmdbId,
                        mediaType = MediaType.TV,
                        rating = item.rating,
                        ratedAt = item.ratedAt ?: Clock.System.now()
                    )
                }
            }

            val watchlistMovies = trakt.runsAndCatchingError {
                trakt.users.getWatchlistMovies(
                    TraktUserSlug.ME,
                    1,
                    1000,
                    null
                ).mapNotNull { item ->
                    val tmdbId = item.movie?.ids?.tmdb ?: return@mapNotNull null
                    ExternalWatchlistItem(
                        tmdbId = tmdbId,
                        mediaType = MediaType.MOVIE,
                        addedAt = item.listedAt ?: Clock.System.now()
                    )
                }
            }

            val watchlistShows = trakt.runsAndCatchingError {
                trakt.users.getWatchlistShows(
                    TraktUserSlug.ME,
                    1,
                    1000,
                    null
                ).mapNotNull { item ->
                    val tmdbId = item.show?.ids?.tmdb ?: return@mapNotNull null
                    ExternalWatchlistItem(
                        tmdbId = tmdbId,
                        mediaType = MediaType.TV,
                        addedAt = item.listedAt ?: Clock.System.now()
                    )
                }
            }
            val collectionMovies = trakt.runsAndCatchingError {
                trakt.users.getCollectionMovies(
                    TraktUserSlug.ME
                ).mapNotNull { item ->
                    val tmdbId = item.movie?.ids?.tmdb ?: return@mapNotNull null
                    ExternalCollectionItem(
                        tmdbId = tmdbId,
                        mediaType = MediaType.MOVIE,
                        addedAt = item.collectedAt ?: Clock.System.now()
                    )
                }
            }

            val collectionShows = trakt.runsAndCatchingError {
                trakt.users.getCollectionShows(
                    TraktUserSlug.ME
                ).flatMap { item ->
                    val showTmdbId = item.show?.ids?.tmdb ?: return@flatMap emptyList()
                    val collectedAt = item.lastCollectedAt ?: Clock.System.now()
                    item.seasons.flatMap { season ->
                        season.episodes?.map { episode ->
                            ExternalCollectionItem(
                                tmdbId = showTmdbId,
                                mediaType = MediaType.TV,
                                addedAt = collectedAt,
                                season = season.number,
                                episode = episode.number
                            )
                        } ?: emptyList()
                    } ?: emptyList()
                }
            }

            ExternalUserData(
                history = historyMovies + historyEpisodes,
                ratings = ratingsMovies + ratingsShows,
                watchlist = watchlistMovies + watchlistShows,
                collection = collectionMovies + collectionShows
            )
        } catch (e: Exception) {
            logger.error("Failed to fetch Trakt user data", e)
            ExternalUserData(emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    private inline fun <T> Trakt.runsAndCatchingError(block: () -> List<T>): List<T> {
        return try {
            block()
        } catch (e: Exception) {
            logger.error("Trakt API request error in fetchUserData sub-call", e)
            emptyList()
        }
    }

    override suspend fun pushUserData(
        accessToken: String,
        historyItems: List<ExternalHistoryItem>,
        ratingItems: List<ExternalRatingItem>,
        watchlistItems: List<ExternalWatchlistItem>,
        collectionItems: List<ExternalCollectionItem>
    ): Boolean {
        val trakt = Trakt {
            clientId = this@TraktDataSyncProvider.clientId
            userAuthentication {
                loadTokens {
                    BearerTokens(accessToken, "")
                }
            }
        }

        var success = true

        // 1. Отправка истории
        if (historyItems.isNotEmpty()) {
            val movies = historyItems.filter { it.mediaType == MediaType.MOVIE }.map { movie ->
                TraktMovieItem(ids = TraktIds(tmdb = movie.tmdbId), watchedAt = movie.watchedAt.toString())
            }
            val tvItems = historyItems.filter { it.mediaType == MediaType.TV }
            val shows = tvItems.groupBy { it.tmdbId }.map { (showTmdbId, episodes) ->
                val seasons = episodes.filter { it.season != null && it.episode != null }
                    .groupBy { it.season!! }
                    .map { (seasonNum, eps) ->
                        TraktSeasonItem(
                            number = seasonNum,
                            episodes = eps.map { ep ->
                                TraktEpisodeItem(number = ep.episode!!, watchedAt = ep.watchedAt.toString())
                            }
                        )
                    }
                TraktShowItem(ids = TraktIds(tmdb = showTmdbId), seasons = seasons.takeIf { it.isNotEmpty() })
            }

            val payload = TraktHistoryPayload(
                movies = movies.takeIf { it.isNotEmpty() },
                shows = shows.takeIf { it.isNotEmpty() }
            )
            success = success && postTrakt(trakt, "sync/history", Json.encodeToJsonElement(payload))
        }

        // 2. Отправка оценок
        if (ratingItems.isNotEmpty()) {
            val movies = ratingItems.filter { it.mediaType == MediaType.MOVIE }.map { movie ->
                TraktMovieItem(
                    ids = TraktIds(tmdb = movie.tmdbId),
                    rating = movie.rating,
                    ratedAt = movie.ratedAt.toString()
                )
            }

            val tvShows = ratingItems.filter { it.mediaType == MediaType.TV && it.season == null }
            val showsList = tvShows.map { show ->
                TraktShowItem(
                    ids = TraktIds(tmdb = show.tmdbId),
                    rating = show.rating,
                    ratedAt = show.ratedAt.toString()
                )
            }

            val tvEpisodes =
                ratingItems.filter { it.mediaType == MediaType.TV && it.season != null && it.episode != null }
            val showsWithEpisodes = tvEpisodes.groupBy { it.tmdbId }.map { (showTmdbId, eps) ->
                val seasons = eps.groupBy { it.season!! }.map { (seasonNum, seasonEps) ->
                    TraktSeasonItem(
                        number = seasonNum,
                        episodes = seasonEps.map { ep ->
                            TraktEpisodeItem(number = ep.episode!!, rating = ep.rating, ratedAt = ep.ratedAt.toString())
                        }
                    )
                }
                TraktShowItem(ids = TraktIds(tmdb = showTmdbId), seasons = seasons)
            }

            val payload = TraktHistoryPayload(
                movies = movies.takeIf { it.isNotEmpty() },
                shows = (showsList + showsWithEpisodes).takeIf { it.isNotEmpty() }
            )
            success = success && postTrakt(trakt, "sync/ratings", Json.encodeToJsonElement(payload))
        }

        // 3. Отправка Watchlist
        if (watchlistItems.isNotEmpty()) {
            val movies = watchlistItems.filter { it.mediaType == MediaType.MOVIE }.map { movie ->
                TraktMovieItem(ids = TraktIds(tmdb = movie.tmdbId))
            }
            val shows = watchlistItems.filter { it.mediaType == MediaType.TV }.map { show ->
                TraktShowItem(ids = TraktIds(tmdb = show.tmdbId))
            }

            val payload = TraktHistoryPayload(
                movies = movies.takeIf { it.isNotEmpty() },
                shows = shows.takeIf { it.isNotEmpty() }
            )
            success = success && postTrakt(trakt, "sync/watchlist", Json.encodeToJsonElement(payload))
        }

        // 4. Отправка коллекции (Collection)
        if (collectionItems.isNotEmpty()) {
            val movies = collectionItems.filter { it.mediaType == MediaType.MOVIE }.map { movie ->
                TraktMovieItem(ids = TraktIds(tmdb = movie.tmdbId), watchedAt = movie.addedAt.toString())
            }
            val tvItems = collectionItems.filter { it.mediaType == MediaType.TV }
            val shows = tvItems.groupBy { it.tmdbId }.map { (showTmdbId, episodes) ->
                val seasons = episodes.filter { it.season != null && it.episode != null }
                    .groupBy { it.season!! }
                    .map { (seasonNum, eps) ->
                        TraktSeasonItem(
                            number = seasonNum,
                            episodes = eps.map { ep ->
                                TraktEpisodeItem(number = ep.episode!!, watchedAt = ep.addedAt.toString())
                            }
                        )
                    }
                TraktShowItem(ids = TraktIds(tmdb = showTmdbId), seasons = seasons.takeIf { it.isNotEmpty() })
            }

            val payload = TraktHistoryPayload(
                movies = movies.takeIf { it.isNotEmpty() },
                shows = shows.takeIf { it.isNotEmpty() }
            )
            success = success && postTrakt(trakt, "sync/collection", Json.encodeToJsonElement(payload))
        }

        return success
    }

    override suspend fun getShowWatchedProgress(
        accessToken: String,
        showTmdbId: Int
    ): WatchedProgress? {
        val trakt = Trakt {
            clientId = this@TraktDataSyncProvider.clientId
            userAuthentication {
                loadTokens {
                    BearerTokens(accessToken, "")
                }
            }
        }
        return try {
            val progress = trakt.shows.getProgress(showTmdbId.toString())
            val nextEp = progress.nextEpisode ?: return null
            WatchedProgress(
                season = nextEp.season ?: return null,
                number = nextEp.number ?: return null,
                title = nextEp.title,
                tmdbId = nextEp.ids?.tmdb
            )
        } catch (e: Exception) {
            logger.error("Failed to get watched progress for show TMDB ID: $showTmdbId from Trakt", e)
            null
        }
    }

    override suspend fun fetchUserLists(accessToken: String): List<ExternalCustomList> {
        val trakt = Trakt {
            clientId = this@TraktDataSyncProvider.clientId
            userAuthentication {
                loadTokens {
                    BearerTokens(accessToken, "")
                }
            }
        }

        return try {
            val lists = trakt.users.getLists(TraktUserSlug.ME)
            lists.mapNotNull { traktList ->
                val listId = traktList.ids?.trakt?.toString() ?: return@mapNotNull null
                val slug = traktList.ids?.slug ?: return@mapNotNull null
                val name = traktList.name ?: return@mapNotNull null

                val items = try {
                    trakt.users.getListItems(TraktUserSlug.ME, listId).mapNotNull { item ->
                        val tmdbId: Int?
                        val mediaType: MediaType?
                        val title: String?

                        when (item.type) {
                            TraktListItemType.MOVIE -> {
                                tmdbId = item.movie?.ids?.tmdb
                                mediaType = MediaType.MOVIE
                                title = item.movie?.title
                            }

                            TraktListItemType.SHOW -> {
                                tmdbId = item.show?.ids?.tmdb
                                mediaType = MediaType.TV
                                title = item.show?.title
                            }

                            else -> return@mapNotNull null
                        }

                        if (tmdbId == null) return@mapNotNull null

                        ExternalCustomListItem(
                            tmdbId = tmdbId,
                            mediaType = mediaType,
                            title = title,
                            rank = item.rank,
                            listedAt = item.listedAt
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Failed to fetch items for Trakt list '$name' (slug=$slug)", e)
                    emptyList()
                }

                ExternalCustomList(
                    externalListId = listId,
                    slug = slug,
                    name = name,
                    privacy = traktList.privacy?.name?.lowercase() ?: "private",
                    items = items
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch Trakt user lists", e)
            emptyList()
        }
    }

    override suspend fun createCustomList(
        accessToken: String,
        name: String,
        privacy: String
    ): ExternalCustomList? {
        val trakt = Trakt {
            clientId = this@TraktDataSyncProvider.clientId
            userAuthentication {
                loadTokens {
                    BearerTokens(accessToken, "")
                }
            }
        }
        val listPrivacy = when (privacy.lowercase()) {
            "private" -> TraktListPrivacy.PRIVATE
            "public" -> TraktListPrivacy.PUBLIC
            "friends" -> TraktListPrivacy.FRIENDS
            else -> TraktListPrivacy.PRIVATE
        }
        val listRequest = TraktList(
            name = name,
            privacy = listPrivacy
        )
        val createdList = trakt.users.createList(TraktUserSlug.ME, listRequest)
        val listId = createdList.ids?.trakt?.toString() ?: return null
        val slug = createdList.ids?.slug ?: return null
        return ExternalCustomList(
            externalListId = listId,
            slug = slug,
            name = createdList.name ?: name,
            privacy = createdList.privacy?.name?.lowercase() ?: "private",
            items = emptyList()
        )
    }

    override suspend fun addMediaToList(
        accessToken: String,
        externalListId: String, // Передается slug списка
        mediaType: MediaType,
        tmdbId: Int
    ): Boolean {
        logger.info("addMediaToList: slug='$externalListId', mediaType=$mediaType, tmdbId=$tmdbId")
        val trakt = Trakt {
            clientId = this@TraktDataSyncProvider.clientId
            userAuthentication {
                loadTokens {
                    BearerTokens(accessToken, "")
                }
            }
        }
        return try {
            val syncMovie =
                if (mediaType == MediaType.MOVIE) listOf(TraktSyncMovie(ids = TraktItemIds(tmdb = tmdbId))) else null
            val syncShow =
                if (mediaType == MediaType.TV) listOf(TraktSyncShow(ids = TraktItemIds(tmdb = tmdbId))) else null
            val syncItems = TraktSyncItems(movies = syncMovie, shows = syncShow)

            val response = trakt.users.addListItems(TraktUserSlug.ME, externalListId, syncItems)
            logger.info("addMediaToList SUCCESS: slug='$externalListId', response added=${response.added}, notFound=${response.notFound}")
            true
        } catch (e: Exception) {
            logger.error("addMediaToList FAILED: slug='$externalListId', tmdbId=$tmdbId, error=${e.message}", e)
            false
        }
    }

    override suspend fun removeMediaFromList(
        accessToken: String,
        externalListId: String, // Передается slug списка
        mediaType: MediaType,
        tmdbId: Int
    ): Boolean {
        val trakt = Trakt {
            clientId = this@TraktDataSyncProvider.clientId
            userAuthentication {
                loadTokens {
                    BearerTokens(accessToken, "")
                }
            }
        }
        return try {
            val syncMovie =
                if (mediaType == MediaType.MOVIE) listOf(TraktSyncMovie(ids = TraktItemIds(tmdb = tmdbId))) else null
            val syncShow =
                if (mediaType == MediaType.TV) listOf(TraktSyncShow(ids = TraktItemIds(tmdb = tmdbId))) else null
            val syncItems = TraktSyncItems(movies = syncMovie, shows = syncShow)

            trakt.users.removeListItems(TraktUserSlug.ME, externalListId, syncItems)
            true
        } catch (e: Exception) {
            logger.error("Failed to remove media from Trakt list: externalListId=$externalListId, tmdbId=$tmdbId", e)
            false
        }
    }
}
