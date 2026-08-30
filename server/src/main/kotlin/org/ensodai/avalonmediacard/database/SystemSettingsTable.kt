package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object SystemSettingsTable : Table("system_settings") {
    val key = text("key")
    val value = text("value")
    val updatedAt = timestamp("updated_at").nullable()

    override val primaryKey = PrimaryKey(key)
}
