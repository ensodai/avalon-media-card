package org.ensodai.avalonmediacard.data.repository

import org.ensodai.avalonmediacard.contract.admin.AdminActionResponse
import org.ensodai.avalonmediacard.contract.admin.CreateUserRequest
import org.ensodai.avalonmediacard.contract.admin.GlobalIntegrationSettingsDto
import org.ensodai.avalonmediacard.contract.admin.ServerSystemInfoDto
import org.ensodai.avalonmediacard.contract.admin.UpdateGlobalIntegrationSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UpdateJackettSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UpdateProwlarrSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UpdateTmdbSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UpdateTorrServerSettingsRequest
import org.ensodai.avalonmediacard.contract.admin.UserDto
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.model.UserStatus
import org.ensodai.avalonmediacard.contract.rpc.AdminRpcService
import org.koin.core.annotation.Single

@Single
class AdminRepository(private val adminRpcService: AdminRpcService) {
    suspend fun createUser(request: CreateUserRequest): AdminActionResponse {
        return adminRpcService.createUser(request)
    }

    suspend fun getUsers(): List<UserDto> {
        return adminRpcService.getUsers()
    }

    suspend fun updateUserStatus(userId: String, status: UserStatus): AdminActionResponse {
        return adminRpcService.updateUserStatus(userId, status)
    }

    suspend fun updateUserRole(userId: String, role: UserRole): AdminActionResponse {
        return adminRpcService.updateUserRole(userId, role)
    }

    suspend fun resetUserPassword(userId: String, newPasswordRaw: String): AdminActionResponse {
        return adminRpcService.resetUserPassword(userId, newPasswordRaw)
    }

    suspend fun deleteUser(userId: String): AdminActionResponse {
        return adminRpcService.deleteUser(userId)
    }

    suspend fun getGlobalIntegrationSettings(): GlobalIntegrationSettingsDto {
        return adminRpcService.getGlobalIntegrationSettings()
    }

    suspend fun updateGlobalIntegrationSettings(request: UpdateGlobalIntegrationSettingsRequest): AdminActionResponse {
        return adminRpcService.updateGlobalIntegrationSettings(request)
    }

    suspend fun updateTmdbSettings(request: UpdateTmdbSettingsRequest): AdminActionResponse {
        return adminRpcService.updateTmdbSettings(request)
    }

    suspend fun updateTorrServerSettings(request: UpdateTorrServerSettingsRequest): AdminActionResponse {
        return adminRpcService.updateTorrServerSettings(request)
    }

    suspend fun updateProwlarrSettings(request: UpdateProwlarrSettingsRequest): AdminActionResponse {
        return adminRpcService.updateProwlarrSettings(request)
    }

    suspend fun updateJackettSettings(request: UpdateJackettSettingsRequest): AdminActionResponse {
        return adminRpcService.updateJackettSettings(request)
    }

    suspend fun testTmdbConnection(token: String): AdminActionResponse {
        return adminRpcService.testTmdbConnection(token)
    }

    suspend fun testTorrServerConnection(host: String, login: String?, password: String?): AdminActionResponse {
        return adminRpcService.testTorrServerConnection(host, login, password)
    }

    suspend fun testProwlarrConnection(url: String, apiKey: String): AdminActionResponse {
        return adminRpcService.testProwlarrConnection(url, apiKey)
    }

    suspend fun testJackettConnection(url: String, apiKey: String): AdminActionResponse {
        return adminRpcService.testJackettConnection(url, apiKey)
    }

    suspend fun getSystemInfo(): ServerSystemInfoDto {
        return adminRpcService.getSystemInfo()
    }

    suspend fun clearDiscoverCache(): AdminActionResponse {
        return adminRpcService.clearDiscoverCache()
    }

    suspend fun clearFeedCache(): AdminActionResponse {
        return adminRpcService.clearFeedCache()
    }

    suspend fun clearMediaCache(): AdminActionResponse {
        return adminRpcService.clearMediaCache()
    }

    suspend fun reloadPlugins(): AdminActionResponse {
        return adminRpcService.reloadPlugins()
    }
}