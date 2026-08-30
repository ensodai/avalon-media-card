package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.jetbrains.exposed.v1.datetime.timestamp

object UserMovieTable : BaseUuidTable("user_movies") {
    val userId = uuid("user_id")
    val mediaId = reference("media_id", MediaTable)
    val mediaType = enumerationByName("media_type", 20, MediaType::class)
    val status = enumerationByName("status", 20, MediaStatus::class).default(MediaStatus.NONE)
    val userRating = integer("user_rating").nullable()
    val progressSeconds = long("progress_seconds").default(0)
    val durationSeconds = long("duration_seconds").default(0)
    val inCollection = bool("in_collection").default(false)
    val lastWatchedAt = timestamp("last_watched_at")
    val lastSourceProviderId = varchar("last_source_provider_id", 100).nullable()
    val lastSourceId = varchar("last_source_id", 255).nullable()
    val lastSourcePayload = text("last_source_payload").nullable()
}
