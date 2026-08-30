package org.ensodai.avalonmediacard.database

object MediaEpisodeTable : BaseUuidTable("episodes") {
    val seasonId = reference("season_id", MediaSeasonTable)
    val episodeNumber = integer("episode_number")
    val runtime = integer("runtime").nullable()
    val airDate = varchar("air_date", 50).nullable()

    init {
        uniqueIndex(seasonId, episodeNumber)
    }
}
