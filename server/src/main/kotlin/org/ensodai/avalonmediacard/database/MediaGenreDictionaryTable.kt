package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.core.Table

/**
 * Справочник жанров TMDB. 
 * Позволяет отвязать вектор предпочтений от языка локали (храним ID жанра, а не строку).
 */
object MediaGenreDictionaryTable : Table("media_genre_dictionary") {
    val genreId = integer("genre_id")
    val languageCode = varchar("language_code", 10)
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(genreId, languageCode, name = "pk_media_genre_dictionary")
}
