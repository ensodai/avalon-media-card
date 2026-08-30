package org.ensodai.avalonmediacard.repository

import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.DynamicSection
import org.ensodai.avalonmediacard.contract.plugins.UserFeedCacheProvider
import org.ensodai.avalonmediacard.database.UserFeedSectionCacheTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Single(binds = [UserFeedCacheProvider::class, UserFeedCacheRepository::class])
class UserFeedCacheRepository : UserFeedCacheProvider {
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    override suspend fun getSections(
        userId: Uuid,
        scope: String,
        language: String
    ): List<DynamicSection>? = dbQuery {
        val row = UserFeedSectionCacheTable.selectAll()
            .where { 
                (UserFeedSectionCacheTable.userId eq userId) and 
                (UserFeedSectionCacheTable.scope eq scope.lowercase()) and 
                (UserFeedSectionCacheTable.language eq language.lowercase()) 
            }
            .firstOrNull() ?: return@dbQuery null

        try {
            json.decodeFromString<List<DynamicSection>>(row[UserFeedSectionCacheTable.sectionsJson])
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveSections(
        userId: Uuid,
        scope: String,
        language: String,
        sections: List<DynamicSection>
    ): Unit = dbQuery {
        val now = Clock.System.now()
        val jsonStr = json.encodeToString(sections)
        val cleanScope = scope.lowercase()
        val cleanLang = language.lowercase()

        val existing = UserFeedSectionCacheTable.selectAll()
            .where { 
                (UserFeedSectionCacheTable.userId eq userId) and 
                (UserFeedSectionCacheTable.scope eq cleanScope) and 
                (UserFeedSectionCacheTable.language eq cleanLang) 
            }
            .firstOrNull()

        if (existing != null) {
            UserFeedSectionCacheTable.update({
                (UserFeedSectionCacheTable.userId eq userId) and 
                (UserFeedSectionCacheTable.scope eq cleanScope) and 
                (UserFeedSectionCacheTable.language eq cleanLang)
            }) {
                it[UserFeedSectionCacheTable.sectionsJson] = jsonStr
                it[UserFeedSectionCacheTable.updatedAt] = now
            }
        } else {
            UserFeedSectionCacheTable.insert {
                it[UserFeedSectionCacheTable.userId] = userId
                it[UserFeedSectionCacheTable.scope] = cleanScope
                it[UserFeedSectionCacheTable.language] = cleanLang
                it[UserFeedSectionCacheTable.sectionsJson] = jsonStr
                it[UserFeedSectionCacheTable.createdAt] = now
                it[UserFeedSectionCacheTable.updatedAt] = now
            }
        }
    }

    override suspend fun invalidateUser(userId: Uuid): Unit = dbQuery {
        UserFeedSectionCacheTable.deleteWhere { UserFeedSectionCacheTable.userId eq userId }
    }

    override suspend fun invalidateAll(): Unit = dbQuery {
        UserFeedSectionCacheTable.deleteAll()
    }

    suspend fun count(): Long = dbQuery {
        UserFeedSectionCacheTable.selectAll().count()
    }
}
