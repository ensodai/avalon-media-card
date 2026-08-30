package org.ensodai.avalonmediacard.plugin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ensodai.avalonmediacard.contract.plugins.UserPluginSettings
import org.ensodai.avalonmediacard.repository.UserIntegrationSettingsRepository
import kotlin.uuid.Uuid

class UserPluginSettingsImpl(
    private val pluginId: String,
    private val repository: UserIntegrationSettingsRepository
) : UserPluginSettings {

    override suspend fun getString(userId: Uuid, key: String): String? {
        return repository.getSetting(userId, pluginId, key)
    }

    override suspend fun setString(userId: Uuid, key: String, value: String) {
        repository.saveSetting(userId, pluginId, key, value)
    }

    override suspend fun getBoolean(userId: Uuid, key: String, defaultValue: Boolean): Boolean {
        return getString(userId, key)?.toBooleanStrictOrNull() ?: defaultValue
    }

    override suspend fun setBoolean(userId: Uuid, key: String, value: Boolean) {
        setString(userId, key, value.toString())
    }

    override fun observeString(userId: Uuid, key: String, defaultValue: String?): Flow<String?> {
        return repository.observeSetting(userId, pluginId, key, defaultValue)
    }

    override fun observeBoolean(userId: Uuid, key: String, defaultValue: Boolean): Flow<Boolean> {
        return repository.observeSetting(userId, pluginId, key, defaultValue.toString())
            .map { it?.toBooleanStrictOrNull() ?: defaultValue }
    }
}
