package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.sync.SyncStatus
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.datetime.timestamp

object UserMediaSyncStatusTable : BaseUuidTable("user_media_sync_status") {
    val userId = uuid("user_id")
    val mediaType = enumerationByName("media_type", 20, MediaType::class)
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val service = varchar("service", 50)
    val status = enumerationByName("status", 20, SyncStatus::class)
    val lastSyncedAt = timestamp("last_synced_at").nullable()
    val errorMessage = text("error_message").nullable()
}
