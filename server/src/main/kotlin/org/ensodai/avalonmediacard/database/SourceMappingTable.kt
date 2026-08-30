package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.core.ReferenceOption

object SourceMappingTable : BaseUuidTable("source_mappings") {
    val sourceType = varchar("source_type", 64).default("torrserver").index()
    val sourceId = varchar("source_id", 256).index()
    val itemKey = text("item_key")

    val seasons = text("seasons").nullable()
    val episodes = text("episodes").nullable()

    val isAbsolute = bool("is_absolute").default(false)
    val isManual = bool("is_manual").default(false)
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.SET_NULL).nullable().index()
    val fileIndex = integer("file_index").nullable()
    val fileSize = long("file_size").nullable()
    val streamUrl = text("stream_url").nullable()
    val quality = varchar("quality", 32).nullable()

    init {
        uniqueIndex(sourceId, itemKey)
    }
}
