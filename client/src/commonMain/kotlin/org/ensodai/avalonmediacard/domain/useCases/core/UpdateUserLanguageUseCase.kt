package org.ensodai.avalonmediacard.domain.useCases.core

import org.ensodai.avalonmediacard.contract.model.UserSettingsDto
import org.ensodai.avalonmediacard.domain.repository.UserSettingsRepository
import org.koin.core.annotation.Factory

@Factory
class GetUserSettingsUseCase(
    private val userSettingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke(): UserSettingsDto {
        return userSettingsRepository.getUserSettings()
    }
}

@Factory
class UpdateUserSettingsUseCase(
    private val userSettingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke(settings: UserSettingsDto): Boolean {
        return userSettingsRepository.updateUserSettings(settings)
    }
}

@Factory
class UpdateUserLanguageUseCase(
    private val userSettingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke(languageCode: String): Boolean {
        val current = userSettingsRepository.getUserSettings()
        return userSettingsRepository.updateUserSettings(current.copy(uiLocale = languageCode))
    }
}
