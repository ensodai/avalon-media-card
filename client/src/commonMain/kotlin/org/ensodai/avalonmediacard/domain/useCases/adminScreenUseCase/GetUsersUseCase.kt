package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.UserDto
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Single

@Single
class GetUsersUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(): Result<List<UserDto>> {
        return try {
            val users = adminRepository.getUsers()
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
