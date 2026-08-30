package org.ensodai.avalonmediacard.data.repository

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.model.UserSettingsDto
import org.ensodai.avalonmediacard.contract.rpc.UserSettingsRpcService
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.domain.repository.UserSettingsRepository
import org.koin.core.annotation.Single

@Single
class UserSettingsRepositoryImpl(
    private val appSettingsStorage: AppSettingsStorage,
    private val userSettingsRpcService: UserSettingsRpcService
) : UserSettingsRepository {

    override val language: Flow<String> = appSettingsStorage.language

    override val cachedLanguage: String
        get() = appSettingsStorage.cachedLanguage

    override suspend fun getUserSettings(): UserSettingsDto {
        return runCatching {
            userSettingsRpcService.getUserSettings()
        }.getOrDefault(UserSettingsDto(uiLocale = appSettingsStorage.cachedLanguage))
    }

    override suspend fun updateUserSettings(settings: UserSettingsDto): Boolean {
        appSettingsStorage.saveLanguage(settings.uiLocale)
        return runCatching {
            userSettingsRpcService.updateUserSettings(settings)
        }.getOrDefault(false)
    }
}
