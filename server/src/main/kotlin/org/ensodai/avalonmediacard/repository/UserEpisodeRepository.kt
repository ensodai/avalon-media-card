package org.ensodai.avalonmediacard.repository

import org.ensodai.avalonmediacard.contract.plugins.UserEpisodeProgress
import org.ensodai.avalonmediacard.contract.plugins.UserEpisodeProvider
import org.ensodai.avalonmediacard.database.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Single
class UserEpisodeRepository : UserEpisodeProvider {
    override suspend fun getEpisodesProgress(
        userId: Uuid,
        mediaId: String,
        catalogId: String
    ): List<UserEpisodeProgress> = dbQuery {
        val internalMediaId = MediaTable.selectAll()
            .where { MediaTable.externalId eq mediaId }
            .limit(1)
            .map { it[MediaTable.id].value }
            .singleOrNull() ?: return@dbQuery emptyList()

        (UserEpisodeTable innerJoin MediaEpisodeTable innerJoin MediaSeasonTable)
            .selectAll()
            .where {
                (UserEpisodeTable.userId eq userId) and
                        (MediaSeasonTable.mediaId eq internalMediaId)
            }.map { row ->
                UserEpisodeProgress(
                    season = row[MediaSeasonTable.seasonNumber],
                    episode = row[MediaEpisodeTable.episodeNumber],
                    progressSeconds = row[UserEpisodeTable.progressSeconds],
                    durationSeconds = row[UserEpisodeTable.durationSeconds],
                    isWatched = row[UserEpisodeTable.isWatched],
                    userRating = row[UserEpisodeTable.userRating],
                    lastWatchedAtEpochMs = row[UserEpisodeTable.lastWatchedAt].toEpochMilliseconds()
                )
            }
    }

    override suspend fun saveEpisodeProgress(
        userId: Uuid,
        catalogId: String,
        mediaId: String,
        season: Int,
        episode: Int,
        progressSeconds: Long,
        durationSeconds: Long,
        isWatched: Boolean
    ) {
        dbQuery {
            val internalMediaId = MediaTable.selectAll()
                .where { MediaTable.externalId eq mediaId }
                .limit(1)
                .map { it[MediaTable.id].value }
                .singleOrNull() ?: return@dbQuery

            val episodeId = (MediaEpisodeTable innerJoin MediaSeasonTable)
                .selectAll()
                .where {
                    (MediaSeasonTable.mediaId eq internalMediaId) and
                            (MediaSeasonTable.seasonNumber eq season) and
                            (MediaEpisodeTable.episodeNumber eq episode)
                }
                .singleOrNull()
                ?.get(MediaEpisodeTable.id)
                ?.value ?: return@dbQuery

            val existing = UserEpisodeTable.selectAll().where {
                (UserEpisodeTable.userId eq userId) and
                        (UserEpisodeTable.episodeId eq episodeId)
            }.singleOrNull()

            if (existing != null) {
                UserEpisodeTable.update({
                    (UserEpisodeTable.userId eq userId) and
                            (UserEpisodeTable.episodeId eq episodeId)
                }) {
                    it[UserEpisodeTable.progressSeconds] = progressSeconds
                    it[UserEpisodeTable.durationSeconds] = durationSeconds
                    val wasWatched = existing[UserEpisodeTable.isWatched]
                    it[UserEpisodeTable.isWatched] = isWatched || wasWatched
                    it[UserEpisodeTable.lastWatchedAt] = Clock.System.now()
                }
            } else {
                UserEpisodeTable.insert {
                    it[UserEpisodeTable.id] = Uuid.random()
                    it[UserEpisodeTable.userId] = userId
                    it[UserEpisodeTable.episodeId] = episodeId
                    it[UserEpisodeTable.progressSeconds] = progressSeconds
                    it[UserEpisodeTable.durationSeconds] = durationSeconds
                    it[UserEpisodeTable.isWatched] = isWatched
                    it[UserEpisodeTable.lastWatchedAt] = Clock.System.now()
                }
            }
        }
    }
}
