package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.GlobalIntegrationSettingsDto
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class GetGlobalIntegrationSettingsUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(): Result<GlobalIntegrationSettingsDto> {
        return try {
            val result = adminRepository.getGlobalIntegrationSettings()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
