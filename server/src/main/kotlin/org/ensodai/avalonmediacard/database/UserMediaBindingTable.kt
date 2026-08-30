package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.core.ReferenceOption

object UserMediaBindingTable : BaseUuidTable("user_media_bindings") {
    val userId = uuid("user_id").references(UserTable.id, onDelete = ReferenceOption.CASCADE)
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val sourceType = varchar("source_type", 64)
    val sourceId = text("source_id")

    init {
        uniqueIndex(userId, mediaId, sourceType)
    }
}
