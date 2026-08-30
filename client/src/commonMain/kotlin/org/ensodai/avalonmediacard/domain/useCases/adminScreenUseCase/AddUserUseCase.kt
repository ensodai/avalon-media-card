package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.contract.admin.CreateUserRequest
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Single

@Single
class AddUserUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(username: String, passwordRaw: String, role: UserRole): Result<AdminActionResponse> {
        return try {
            val request = CreateUserRequest(username, passwordRaw, role)
            val response = adminRepository.createUser(request)
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.error ?: "Failed to create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}