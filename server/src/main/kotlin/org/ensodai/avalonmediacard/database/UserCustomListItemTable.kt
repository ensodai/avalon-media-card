package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.datetime.timestamp

object UserCustomListItemTable : BaseUuidTable("user_custom_list_items") {
    val listId = uuid("list_id").references(UserCustomListTable.id)
    val mediaId = reference("media_id", MediaTable)
    val rank = integer("rank").default(0)
    val listedAt = timestamp("listed_at").nullable()
    val isSynced = bool("is_synced").default(false)
}
