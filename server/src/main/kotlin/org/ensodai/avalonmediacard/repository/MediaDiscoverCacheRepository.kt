package org.ensodai.avalonmediacard.repository

import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.database.MediaDiscoverCacheTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import java.security.MessageDigest
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Single
class MediaDiscoverCacheRepository {
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    private fun computeParamsHash(params: Map<String, String>): String {
        val canonical = params.entries
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(canonical.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun buildCacheKey(paramsHash: String, targetType: String, language: String, page: Int): String {
        return "${paramsHash}_${targetType.lowercase()}_${language.lowercase()}_$page"
    }

    suspend fun get(
        params: Map<String, String>,
        targetType: EntityType,
        page: Int,
        language: String = "ru-RU"
    ): List<TmdbMovieDto>? = dbQuery {
        val paramsHash = computeParamsHash(params)
        val key = buildCacheKey(paramsHash, targetType.name, language, page)
        val now = Clock.System.now()

        val row = MediaDiscoverCacheTable.selectAll()
            .where { (MediaDiscoverCacheTable.cacheKey eq key) and (MediaDiscoverCacheTable.expiresAt greater now) }
            .firstOrNull() ?: return@dbQuery null

        try {
            json.decodeFromString<List<TmdbMovieDto>>(row[MediaDiscoverCacheTable.resultsJson])
        } catch (e: Exception) {
            null
        }
    }

    suspend fun put(
        params: Map<String, String>,
        targetType: EntityType,
        page: Int,
        language: String = "ru-RU",
        results: List<TmdbMovieDto>,
        ttl: Duration = 24.hours
    ): Unit = dbQuery {
        val paramsHash = computeParamsHash(params)
        val key = buildCacheKey(paramsHash, targetType.name, language, page)
        val now = Clock.System.now()
        val expiresAt = now.plus(ttl)
        val jsonStr = json.encodeToString(results)

        val existing = MediaDiscoverCacheTable.selectAll()
            .where { MediaDiscoverCacheTable.cacheKey eq key }
            .firstOrNull()

        if (existing != null) {
            MediaDiscoverCacheTable.update({ MediaDiscoverCacheTable.cacheKey eq key }) {
                it[MediaDiscoverCacheTable.resultsJson] = jsonStr
                it[MediaDiscoverCacheTable.expiresAt] = expiresAt
                it[MediaDiscoverCacheTable.updatedAt] = now
            }
        } else {
            MediaDiscoverCacheTable.insert {
                it[MediaDiscoverCacheTable.cacheKey] = key
                it[MediaDiscoverCacheTable.paramsHash] = paramsHash
                it[MediaDiscoverCacheTable.targetType] = targetType.name
                it[MediaDiscoverCacheTable.language] = language
                it[MediaDiscoverCacheTable.page] = page
                it[MediaDiscoverCacheTable.resultsJson] = jsonStr
                it[MediaDiscoverCacheTable.expiresAt] = expiresAt
                it[MediaDiscoverCacheTable.createdAt] = now
                it[MediaDiscoverCacheTable.updatedAt] = now
            }
        }
    }

    suspend fun cleanExpired(): Int = dbQuery {
        val now = Clock.System.now()
        MediaDiscoverCacheTable.deleteWhere { MediaDiscoverCacheTable.expiresAt less now }
    }

    suspend fun clearAll(): Int = dbQuery {
        MediaDiscoverCacheTable.deleteAll()
    }

    suspend fun count(): Long = dbQuery {
        MediaDiscoverCacheTable.selectAll().count()
    }
}
