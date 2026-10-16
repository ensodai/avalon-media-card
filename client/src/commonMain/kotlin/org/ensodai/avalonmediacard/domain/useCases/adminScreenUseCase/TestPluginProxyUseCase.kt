package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.contract.admin.TestPluginProxyRequest
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class TestPluginProxyUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(request: TestPluginProxyRequest): Result<AdminActionResponse> {
        return try {
            val response = adminRepository.testPluginProxy(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
