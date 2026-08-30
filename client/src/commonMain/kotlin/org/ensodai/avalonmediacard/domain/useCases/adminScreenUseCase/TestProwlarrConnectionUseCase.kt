package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class TestProwlarrConnectionUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(url: String, apiKey: String): Result<AdminActionResponse> {
        return try {
            val response = adminRepository.testProwlarrConnection(url, apiKey)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
