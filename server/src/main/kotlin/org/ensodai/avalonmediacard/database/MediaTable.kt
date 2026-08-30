package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.classification.AnimeSubType

object MediaTable : BaseUuidTable("media") {
    val catalogId = varchar("catalog_id", 50)
    val externalId = varchar("external_id", 100)

    val mediaType = varchar("media_type", 20) // "movie" или "tv"
    val imdbId = varchar("imdb_id", 30).nullable()
    val releaseYear = integer("release_year").nullable()
    val tmdbRating = double("tmdb_rating").nullable()
    val status = varchar("status", 50).nullable()
    val animeSubType = enumerationByName("anime_sub_type", 30, AnimeSubType::class).default(AnimeSubType.NOT_ANIME)

    init {
        uniqueIndex(catalogId, externalId)
        index(false, imdbId)
    }
}
