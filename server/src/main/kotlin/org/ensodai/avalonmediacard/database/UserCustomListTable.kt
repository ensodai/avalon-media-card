package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.model.IntegrationService

object UserCustomListTable : BaseUuidTable("user_custom_lists") {
    val userId = uuid("user_id")
    val service = enumerationByName("service", 50, IntegrationService::class).default(IntegrationService.TRAKT)
    val externalListId = varchar("external_list_id", 100)
    val slug = varchar("slug", 200)
    val name = varchar("name", 500)
    val privacy = varchar("privacy", 20).default("private")
    val itemCount = integer("item_count").default(0)
    val isSynced = bool("is_synced").default(false)
}
