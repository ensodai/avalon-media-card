package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.core.ReferenceOption

object MediaImageTable : BaseUuidTable("media_images") {
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val seasonId = reference("season_id", MediaSeasonTable, onDelete = ReferenceOption.CASCADE).nullable()
    val episodeId = reference("episode_id", MediaEpisodeTable, onDelete = ReferenceOption.CASCADE).nullable()
    val personId = reference("person_id", MediaPersonTable, onDelete = ReferenceOption.CASCADE).nullable()
    val imageType = varchar("image_type", 20)
    val language = varchar("language", 10).nullable()
    val url = text("url")
}

object MediaTranslationTable : BaseUuidTable("media_translations") {
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val language = varchar("language", 10)
    val title = varchar("title", 255)
    val originalTitle = varchar("original_title", 255).nullable()
    val overview = text("overview").nullable()

    init {
        uniqueIndex(mediaId, language)
    }
}

object MediaSeasonTranslationTable : BaseUuidTable("season_translations") {
    val seasonId = reference("season_id", MediaSeasonTable, onDelete = ReferenceOption.CASCADE)
    val language = varchar("language", 10)
    val name = varchar("name", 255).nullable()
    val overview = text("overview").nullable()

    init {
        uniqueIndex(seasonId, language)
    }
}

object MediaEpisodeTranslationTable : BaseUuidTable("episode_translations") {
    val episodeId = reference("episode_id", MediaEpisodeTable, onDelete = ReferenceOption.CASCADE)
    val language = varchar("language", 10)
    val name = varchar("name", 255).nullable()
    val overview = text("overview").nullable()

    init {
        uniqueIndex(episodeId, language)
    }
}

object MediaPersonTranslationTable : BaseUuidTable("person_translations") {
    val personId = reference("person_id", MediaPersonTable, onDelete = ReferenceOption.CASCADE)
    val language = varchar("language", 10)
    val name = varchar("name", 255)
    val biography = text("biography").nullable()

    init {
        uniqueIndex(personId, language)
    }
}

object MediaCreditTranslationTable : BaseUuidTable("credit_translations") {
    val creditId = reference("credit_id", MediaCreditTable, onDelete = ReferenceOption.CASCADE)
    val language = varchar("language", 10)
    val characterName = varchar("character_name", 255).nullable()

    init {
        uniqueIndex(creditId, language)
    }
}
