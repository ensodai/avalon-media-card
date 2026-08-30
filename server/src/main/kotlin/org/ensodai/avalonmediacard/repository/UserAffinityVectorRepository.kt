package org.ensodai.avalonmediacard.repository

import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.plugins.AffinityVectorStore
import org.ensodai.avalonmediacard.database.UserAffinityVectorTable
import org.ensodai.avalonmediacard.database.UserClickstreamTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Single
class UserAffinityVectorRepository : AffinityVectorStore {
    private val json = Json { ignoreUnknownKeys = true }

    private val _vectorUpdates = kotlinx.coroutines.flow.MutableSharedFlow<Uuid>(extraBufferCapacity = 10)
    override val vectorUpdates: kotlinx.coroutines.flow.Flow<Uuid> = _vectorUpdates

    override suspend fun getVector(userId: Uuid): AffinityVector? = dbQuery {
        val row = UserAffinityVectorTable.selectAll().where { UserAffinityVectorTable.userId eq userId }.firstOrNull()
        row?.let {
            json.decodeFromString<AffinityVector>(it[UserAffinityVectorTable.vectorJson])
        }
    }

    override suspend fun saveVector(userId: Uuid, vector: AffinityVector, eventCount: Int): Unit {
        dbQuery {
            val jsonStr = json.encodeToString(vector)
            val now = Clock.System.now()

            UserAffinityVectorTable.upsert(UserAffinityVectorTable.userId) {
                it[UserAffinityVectorTable.userId] = userId
                it[UserAffinityVectorTable.vectorJson] = jsonStr
                it[UserAffinityVectorTable.calculatedAt] = now
                it[UserAffinityVectorTable.eventCount] = eventCount
            }
        }
        _vectorUpdates.tryEmit(userId)
    }

    override suspend fun getPendingUsers(limit: Int): List<Uuid> = dbQuery {
        // Юзеры, у которых есть клики
        val activeUsers = UserClickstreamTable.selectAll()
            .withDistinct()
            .map { it[UserClickstreamTable.userId] }

        if (activeUsers.isEmpty()) return@dbQuery emptyList()

        val existingVectors = UserAffinityVectorTable.selectAll()
            .associateBy { it[UserAffinityVectorTable.userId] }

        // Сначала те, у кого вообще нет вектора
        val pending = activeUsers.filter { !existingVectors.containsKey(it) }.toMutableList()

        if (pending.size >= limit) {
            return@dbQuery pending.take(limit)
        }

        // Затем остальные, отсортированные по дате расчета (старые сначала)
        val existing = existingVectors.values.sortedBy { it[UserAffinityVectorTable.calculatedAt] }
            .map { it[UserAffinityVectorTable.userId] }

        pending.addAll(existing)
        pending.take(limit)
    }

    override suspend fun getUserEventCount(userId: Uuid): Int = dbQuery {
        UserClickstreamTable.selectAll().where { UserClickstreamTable.userId eq userId }.count().toInt()
    }

    override suspend fun getCachedEventCount(userId: Uuid): Int? = dbQuery {
        UserAffinityVectorTable.selectAll()
            .where { UserAffinityVectorTable.userId eq userId }
            .firstOrNull()?.get(UserAffinityVectorTable.eventCount)
    }
}
