package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.datetime.timestamp

object UserAffinityVectorTable : BaseUuidTable("user_affinity_vector") {
    val userId = uuid("user_id").uniqueIndex().references(UserTable.id)
    val vectorJson = text("vector_json")
    val calculatedAt = timestamp("calculated_at")
    val eventCount = integer("event_count")
}
