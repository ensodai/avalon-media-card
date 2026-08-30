package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.core.Table

/**
 * Плоская таблица для кэширования связи "Медиа <-> Ключевое слово (Keyword/Тег)".
 * Используется рекомендательным движком (Этап 7) для кросс-жанрового опыления.
 */
object MediaKeywordTable : Table("media_keywords") {
    val mediaType = varchar("media_type", 50)
    val mediaId = reference("media_id", MediaTable)
    val keywordId = integer("keyword_id")
    val keywordName = varchar("keyword_name", 255)

    override val primaryKey = PrimaryKey(mediaId, keywordId, name = "pk_media_keywords")
}
