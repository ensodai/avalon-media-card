package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class ClearFeedCacheUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(): Result<AdminActionResponse> {
        return try {
            val response = adminRepository.clearFeedCache()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
