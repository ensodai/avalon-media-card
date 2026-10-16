package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.contract.admin.UpdatePluginProxyRequest
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class UpdatePluginProxySettingUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(request: UpdatePluginProxyRequest): Result<AdminActionResponse> {
        return try {
            val response = adminRepository.updatePluginProxySetting(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
