package org.ensodai.avalonmediacard.repository

import org.ensodai.avalonmediacard.contract.model.KeywordMetadata
import org.ensodai.avalonmediacard.database.MediaKeywordTable
import org.ensodai.avalonmediacard.database.MediaTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.koin.core.annotation.Single
import java.sql.ResultSet
import kotlin.uuid.Uuid

@Single
class MediaKeywordRepository {

    suspend fun getMediaIdsWithoutKeywords(limit: Int): List<Triple<String, String, String>> = dbQuery {
        val query = """
            SELECT m.id, m.media_type, m.external_id
            FROM media m
            LEFT JOIN media_keywords k 
              ON m.id = k.media_id
            WHERE k.keyword_id IS NULL
            LIMIT $limit
        """.trimIndent()

        val results = mutableListOf<Triple<String, String, String>>()

        TransactionManager.current().exec(query) { rs: ResultSet ->
            while (rs.next()) {
                val mId = rs.getString("id")
                val mType = rs.getString("media_type")
                val mExt = rs.getString("external_id")
                if (mId != null && mType != null && mExt != null) {
                    results.add(Triple(mId, mType, mExt))
                }
            }
        }
        results
    }

    suspend fun saveKeywords(mediaId: String, keywords: List<KeywordMetadata>) = dbQuery {
        val parsed = MediaTable.insertIgnore {
            it[id] = Uuid.random()
            it[catalogId] = "tmdb"
            it[externalId] = mediaId
            it[mediaType] = "movie"
        }
        val internalMediaId =
            MediaTable.selectAll().where { MediaTable.externalId eq mediaId }.singleOrNull()?.get(MediaTable.id)?.value
                ?: return@dbQuery

        if (keywords.isEmpty()) {
            val exists = MediaKeywordTable.selectAll().where {
                (MediaKeywordTable.mediaId eq internalMediaId) and
                        (MediaKeywordTable.keywordId eq 0)
            }.count() > 0

            if (!exists) {
                MediaKeywordTable.insert {
                    it[this.mediaId] = internalMediaId
                    it[keywordId] = 0
                    it[keywordName] = "NONE"
                }
            }
        } else {
            keywords.forEach { kw ->
                val exists = MediaKeywordTable.selectAll().where {
                    (MediaKeywordTable.mediaId eq internalMediaId) and
                            (MediaKeywordTable.keywordId eq kw.id)
                }.count() > 0

                if (!exists) {
                    MediaKeywordTable.insert {
                        it[this.mediaId] = internalMediaId
                        it[keywordId] = kw.id
                        it[keywordName] = kw.name
                    }
                }
            }
        }
    }
}
