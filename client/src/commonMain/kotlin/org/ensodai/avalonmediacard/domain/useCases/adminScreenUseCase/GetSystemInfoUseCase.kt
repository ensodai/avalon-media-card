package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.ServerSystemInfoDto
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class GetSystemInfoUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(): Result<ServerSystemInfoDto> {
        return try {
            val info = adminRepository.getSystemInfo()
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
