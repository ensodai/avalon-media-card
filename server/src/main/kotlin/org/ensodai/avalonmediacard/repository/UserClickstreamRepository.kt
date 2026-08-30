package org.ensodai.avalonmediacard.repository

import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.ClickstreamPayload
import org.ensodai.avalonmediacard.contract.model.TelemetryEvent
import org.ensodai.avalonmediacard.contract.plugins.TelemetryProvider
import org.ensodai.avalonmediacard.database.UserClickstreamTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [org.ensodai.avalonmediacard.repository.UserClickstreamRepository::class, TelemetryProvider::class])
class UserClickstreamRepository : TelemetryProvider {

    suspend fun logEvent(userId: Uuid, event: TelemetryEvent) = dbQuery {
        UserClickstreamTable.insert {
            it[this.id] = Uuid.random()
            it[this.userId] = userId
            it[this.eventType] = event.eventType
            it[this.targetType] = event.targetType
            it[this.targetId] = event.targetId
            it[this.context] = event.context
            it[this.dwellTimeMs] = event.dwellTimeMs
            it[this.payload] = Json.encodeToString(event.payload)
        }
    }

    override suspend fun getUserEvents(userId: Uuid, limit: Int): List<TelemetryEvent> = dbQuery {
        UserClickstreamTable
            .selectAll()
            .where { UserClickstreamTable.userId eq userId }
            .orderBy(UserClickstreamTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map {
                val payloadString = it[UserClickstreamTable.payload]
                val payloadObj = if (!payloadString.isNullOrEmpty()) {
                    try {
                        Json.decodeFromString<ClickstreamPayload>(payloadString)
                    } catch (e: Exception) {
                        ClickstreamPayload.Empty
                    }
                } else {
                    ClickstreamPayload.Empty
                }

                TelemetryEvent(
                    eventType = it[UserClickstreamTable.eventType],
                    targetType = it[UserClickstreamTable.targetType],
                    targetId = it[UserClickstreamTable.targetId],
                    context = it[UserClickstreamTable.context],
                    dwellTimeMs = it[UserClickstreamTable.dwellTimeMs],
                    payload = payloadObj,
                    timestamp = it[UserClickstreamTable.createdAt]
                )
            }
    }
}
