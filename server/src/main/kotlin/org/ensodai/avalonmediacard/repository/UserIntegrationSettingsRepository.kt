package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.flow.*
import org.ensodai.avalonmediacard.database.UserIntegrationSettingsTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class UserIntegrationSettingsRepository {

    private val changeEvents = MutableSharedFlow<UserSettingChangeEvent>(extraBufferCapacity = 64)

    data class UserSettingChangeEvent(val userId: kotlin.uuid.Uuid, val pluginId: String, val key: String)

    suspend fun getSetting(userId: kotlin.uuid.Uuid, pluginId: String, key: String): String? {
        return dbQuery {
            UserIntegrationSettingsTable
                .selectAll()
                .where {
                    (UserIntegrationSettingsTable.userId eq userId) and
                            (UserIntegrationSettingsTable.pluginId eq pluginId) and
                            (UserIntegrationSettingsTable.settingKey eq key)
                }
                .map { it[UserIntegrationSettingsTable.settingValue] }
                .singleOrNull()
        }
    }

    suspend fun saveSetting(userId: kotlin.uuid.Uuid, pluginId: String, key: String, value: String?) {
        dbQuery {
            val exists = UserIntegrationSettingsTable
                .selectAll()
                .where {
                    (UserIntegrationSettingsTable.userId eq userId) and
                            (UserIntegrationSettingsTable.pluginId eq pluginId) and
                            (UserIntegrationSettingsTable.settingKey eq key)
                }
                .empty().not()

            if (exists) {
                UserIntegrationSettingsTable.update({
                    (UserIntegrationSettingsTable.userId eq userId) and
                            (UserIntegrationSettingsTable.pluginId eq pluginId) and
                            (UserIntegrationSettingsTable.settingKey eq key)
                }) {
                    it[settingValue] = value
                }
            } else {
                UserIntegrationSettingsTable.insert {
                    it[this.userId] = userId
                    it[this.pluginId] = pluginId
                    it[settingKey] = key
                    it[settingValue] = value
                }
            }
        }
        changeEvents.tryEmit(UserSettingChangeEvent(userId, pluginId, key))
    }

    fun observeSetting(userId: Uuid, pluginId: String, key: String, defaultValue: String? = null): Flow<String?> {
        return changeEvents
            .filter { it.userId == userId && it.pluginId == pluginId && it.key == key }
            .map { getSetting(userId, pluginId, key) }
            .onStart { emit(getSetting(userId, pluginId, key) ?: defaultValue) }
            .distinctUntilChanged()
    }
}
