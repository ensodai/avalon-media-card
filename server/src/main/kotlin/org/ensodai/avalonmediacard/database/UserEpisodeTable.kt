package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.datetime.timestamp

object UserEpisodeTable : BaseUuidTable("user_episodes") {
    val userId = uuid("user_id")
    val episodeId = reference("episode_id", MediaEpisodeTable)
    val progressSeconds = long("progress_seconds").default(0)
    val durationSeconds = long("duration_seconds").default(0)
    val isWatched = bool("is_watched").default(false)
    val inCollection = bool("in_collection").default(false)
    val userRating = integer("user_rating").nullable()
    val lastWatchedAt = timestamp("last_watched_at")
    val lastSourceProviderId = varchar("last_source_provider_id", 100).nullable()
    val lastSourceId = varchar("last_source_id", 255).nullable()
    val lastSourcePayload = text("last_source_payload").nullable()
}
