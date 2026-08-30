package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.model.UserStatus
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Single

@Single
class UpdateUserStatusUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(userId: String, status: UserStatus): Result<Unit> {
        return try {
            val response = adminRepository.updateUserStatus(userId, status)
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
