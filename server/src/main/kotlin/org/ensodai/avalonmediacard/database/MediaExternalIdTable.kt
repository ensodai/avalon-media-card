package org.ensodai.avalonmediacard.database

object MediaExternalIdTable : BaseUuidTable("media_external_ids") {
    val userMovieId = varchar("user_movie_id", 36)
    val externalSource = varchar("external_source", 50)
    val externalId = varchar("external_id", 100)
}
