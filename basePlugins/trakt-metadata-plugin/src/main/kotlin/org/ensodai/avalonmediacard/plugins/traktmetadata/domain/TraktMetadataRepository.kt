package org.ensodai.avalonmediacard.plugins.traktmetadata.domain

import app.moviebase.trakt.Trakt
import app.moviebase.trakt.model.TraktIdType
import app.moviebase.trakt.model.TraktSearchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.CommentItem

interface TraktMetadataRepository {
    suspend fun getComments(key: MediaKey, page: Int = 1, limit: Int = 10): List<CommentItem>
}

class TraktMetadataRepositoryImpl(
    private val context: PluginContext
) : TraktMetadataRepository {

    private val clientId = "5932b28446d655e166533aba62f6ef730aee104bf7f0021f6ad9d1d12858fe3c"

    private val trakt = Trakt {
        this.clientId = this@TraktMetadataRepositoryImpl.clientId
    }

    override suspend fun getComments(key: MediaKey, page: Int, limit: Int): List<CommentItem> =
        withContext(Dispatchers.IO) {
            val tmdbId = key.id
            val searchType = if (key.type == EntityType.MOVIE) TraktSearchType.MOVIE else TraktSearchType.SHOW

            try {
                val searchResults = trakt.search.searchIdLookup(
                    idType = TraktIdType.TMDB,
                    id = tmdbId,
                    searchType = searchType
                )
                val result = searchResults.firstOrNull() ?: return@withContext emptyList()
                val traktId = result.ids?.trakt?.toString() ?: return@withContext emptyList()

                val rawComments = if (key.type == EntityType.MOVIE) {
                    trakt.movies.getComments(
                        traktId,
                        sort = app.moviebase.trakt.model.TraktCommentSort.LIKES,
                        page = page,
                        limit = limit
                    )
                } else {
                    trakt.shows.getComments(
                        traktId,
                        sort = app.moviebase.trakt.model.TraktCommentSort.LIKES,
                        page = page,
                        limit = limit
                    )
                }

                rawComments.map { comment ->
                    CommentItem(
                        id = comment.id.toString(),
                        authorName = comment.displayUserName ?: "Аноним",
                        authorAvatarUrl = comment.imagePath,
                        commentText = comment.comment,
                        likesCount = comment.likes ?: 0,
                        isSpoiler = comment.spoiler,
                        dateText = comment.createdAt?.toString()?.take(10), // Просто ГГГГ-ММ-ДД
                        userRating = comment.userStats?.rating
                    )
                }
            } catch (e: Exception) {
                context.logger.error("Failed to fetch comments from Trakt for media $tmdbId", e)
                emptyList()
            }
        }
}
