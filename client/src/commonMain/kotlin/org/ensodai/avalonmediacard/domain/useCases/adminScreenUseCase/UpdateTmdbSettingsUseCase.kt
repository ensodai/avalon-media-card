package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.UpdateTmdbSettingsRequest
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateTmdbSettingsUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(request: UpdateTmdbSettingsRequest): Result<Unit> {
        return try {
            val response = adminRepository.updateTmdbSettings(request)
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
