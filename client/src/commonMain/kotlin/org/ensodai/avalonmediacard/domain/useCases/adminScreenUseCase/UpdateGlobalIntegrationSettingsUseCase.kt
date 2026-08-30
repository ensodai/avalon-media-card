package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.contract.admin.UpdateGlobalIntegrationSettingsRequest
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateGlobalIntegrationSettingsUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(request: UpdateGlobalIntegrationSettingsRequest): Result<Unit> {
        return try {
            val response = adminRepository.updateGlobalIntegrationSettings(request)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
