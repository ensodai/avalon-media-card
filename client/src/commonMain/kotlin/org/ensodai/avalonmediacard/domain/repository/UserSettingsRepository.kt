package org.ensodai.avalonmediacard.domain.repository

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.model.UserSettingsDto

interface UserSettingsRepository {
    val language: Flow<String>
    val cachedLanguage: String
    suspend fun getUserSettings(): UserSettingsDto
    suspend fun updateUserSettings(settings: UserSettingsDto): Boolean
}
