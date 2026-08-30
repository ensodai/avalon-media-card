package org.ensodai.avalonmediacard.database

object UserShowProgressTable : BaseUuidTable("user_show_progress") {
    val userId = uuid("user_id")
    val showTmdbId = integer("show_tmdb_id")
    val nextSeason = integer("next_season")
    val nextEpisode = integer("next_episode")
    val title = varchar("title", 255).nullable()
    val nextEpisodeTmdbId = integer("next_episode_tmdb_id").nullable()

    init {
        uniqueIndex("idx_user_show_progress_user_show", userId, showTmdbId)
    }
}
