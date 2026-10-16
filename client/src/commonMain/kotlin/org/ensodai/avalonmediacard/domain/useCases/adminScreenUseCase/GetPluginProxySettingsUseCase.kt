package org.ensodai.avalonmediacard.domain.useCases.adminScreenUseCase

import org.ensodai.avalonmediacard.contract.admin.PluginProxySettingDto
import org.ensodai.avalonmediacard.data.repository.AdminRepository
import org.koin.core.annotation.Factory

@Factory
class GetPluginProxySettingsUseCase(private val adminRepository: AdminRepository) {
    suspend operator fun invoke(): Result<List<PluginProxySettingDto>> {
        return try {
            val settings = adminRepository.getPluginProxySettings()
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
