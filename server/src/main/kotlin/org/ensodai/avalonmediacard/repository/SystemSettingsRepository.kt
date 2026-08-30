package org.ensodai.avalonmediacard.repository

import org.ensodai.avalonmediacard.database.SystemSettingsTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single

@Single
open class SystemSettingsRepository {
    open suspend fun getSetting(key: String): String? = dbQuery {
        SystemSettingsTable.selectAll()
            .where { SystemSettingsTable.key eq key }
            .map { it[SystemSettingsTable.value] }
            .firstOrNull()
    }

    open suspend fun saveSetting(key: String, value: String) = dbQuery {
        val exists = SystemSettingsTable.selectAll()
            .where { SystemSettingsTable.key eq key }
            .empty().not()

        if (exists) {
            SystemSettingsTable.update({ SystemSettingsTable.key eq key }) {
                it[SystemSettingsTable.value] = value
            }
        } else {
            SystemSettingsTable.insert {
                it[SystemSettingsTable.key] = key
                it[SystemSettingsTable.value] = value
            }
        }
    }
}
