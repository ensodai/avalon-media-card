package org.ensodai.avalonmediacard.plugins.torrserver.data.repository

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.SettingsRepository
import kotlin.uuid.Uuid

class SettingsRepositoryImpl(
    private val context: PluginContext
) : SettingsRepository {

    override suspend fun saveGlobalSetting(key: String, value: String) {
        context.settings.setString(key, value)
    }

    override suspend fun saveUserSetting(userId: Uuid, key: String, value: String) {
        context.userSettings.setString(userId, key, value)
    }
}
