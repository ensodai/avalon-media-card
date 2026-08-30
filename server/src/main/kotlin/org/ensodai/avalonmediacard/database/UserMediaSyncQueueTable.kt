package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.sync.SyncAction
import org.ensodai.avalonmediacard.contract.sync.SyncStatus
import org.jetbrains.exposed.v1.datetime.timestamp

object UserMediaSyncQueueTable : BaseUuidTable("user_media_sync_queue") {
    val userId = uuid("user_id")
    val mediaType = enumerationByName("media_type", 20, MediaType::class)
    val mediaId = reference("media_id", MediaTable)
    val service = varchar("service", 50)
    val action = enumerationByName("action", 50, SyncAction::class)
    val progressSeconds = long("progress_seconds").nullable()
    val durationSeconds = long("duration_seconds").nullable()
    val rating = integer("rating").nullable()
    val episodeId = reference("episode_id", MediaEpisodeTable).nullable()
    val status = enumerationByName("status", 20, SyncStatus::class).default(SyncStatus.PENDING)
    val attempts = integer("attempts").default(0)
    val lastAttemptAt = timestamp("last_attempt_at").nullable()
}
