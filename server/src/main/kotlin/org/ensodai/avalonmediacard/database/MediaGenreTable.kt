package org.ensodai.avalonmediacard.database

object MediaGenreTable : BaseUuidTable("media_genres") {
    val mediaId = reference("media_id", MediaTable)
    val genreId = integer("genre_id").default(0)
}
