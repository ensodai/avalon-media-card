package org.ensodai.avalonmediacard.database

object MediaSeasonTable : BaseUuidTable("seasons") {
    val mediaId = reference("media_id", MediaTable)
    val seasonNumber = integer("season_number")
    val episodeCount = integer("episode_count").nullable()
    val airDate = varchar("air_date", 50).nullable()

    init {
        uniqueIndex(mediaId, seasonNumber)
    }
}
