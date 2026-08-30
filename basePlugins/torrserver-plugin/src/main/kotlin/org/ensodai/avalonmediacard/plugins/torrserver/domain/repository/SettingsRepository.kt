package org.ensodai.avalonmediacard.plugins.torrserver.domain.repository

import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.JackettSettingsUpdate
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ProwlarrSettingsUpdate
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerSettingsUpdate
import kotlin.uuid.Uuid

interface SettingsRepository {
    suspend fun saveGlobalSetting(key: String, value: String)
    suspend fun saveUserSetting(userId: Uuid, key: String, value: String)
}

suspend fun SettingsRepository.saveSetting(userId: Uuid?, key: String, value: String?) {
    if (value != null) {
        saveGlobalSetting(key, value)
        if (userId != null) {
            saveUserSetting(userId, key, value)
        }
    }
}
