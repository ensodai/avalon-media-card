package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Single

@Single
class ResetUserPasswordUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(userId: String, newPasswordRaw: String): Result<Unit> {
        return try {
            val response = adminRepository.resetUserPassword(userId, newPasswordRaw)
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
