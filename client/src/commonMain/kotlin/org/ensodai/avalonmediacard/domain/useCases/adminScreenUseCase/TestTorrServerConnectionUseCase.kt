package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class TestTorrServerConnectionUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(host: String, login: String?, password: String?): Result<AdminActionResponse> {
        return try {
            val response = adminRepository.testTorrServerConnection(host, login, password)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
