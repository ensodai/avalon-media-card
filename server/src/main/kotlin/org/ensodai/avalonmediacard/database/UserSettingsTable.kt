package org.ensodai.avalonmediacard.database

object UserSettingsTable : BaseUuidTable("user_settings") {
    val userId = uuid("user_id").uniqueIndex()
    val locale = varchar("locale", 10).default("ru")
    val posterLanguage = varchar("poster_language", 20).nullable()
    val titleMode = varchar("title_mode", 20).default("LOCALIZED")
    val overviewLanguage = varchar("overview_language", 20).nullable()
}
