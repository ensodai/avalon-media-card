package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.core.ReferenceOption

object UserFeedSectionCacheTable : BaseUuidTable("user_feed_section_cache") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val scope = varchar("scope", 32)
    val language = varchar("language", 10)
    val sectionsJson = text("sections_json")

    init {
        uniqueIndex("user_feed_section_cache_user_scope_lang", userId, scope, language)
    }
}
