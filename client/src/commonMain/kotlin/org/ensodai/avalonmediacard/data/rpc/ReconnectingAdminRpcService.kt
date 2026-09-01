package org.ensodai.avalonmediacard.data.rpc

import kotlinx.rpc.withService
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

class ReconnectingAdminRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : AdminRpcService {

    private suspend fun getService(): AdminRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun uploadPlugin(fileName: String, fileContent: ByteArray): Boolean =
        executor.execute("uploadPlugin", getService = { getService() }) { uploadPlugin(fileName, fileContent) }

    override suspend fun createUser(request: CreateUserRequest): AdminActionResponse =
        executor.execute("createUser", getService = { getService() }) { createUser(request) }

    override suspend fun getUsers(): List<UserDto> =
        executor.execute("getUsers", getService = { getService() }) { getUsers() }

    override suspend fun updateUserStatus(userId: String, status: UserStatus): AdminActionResponse =
        executor.execute("updateUserStatus", getService = { getService() }) { updateUserStatus(userId, status) }

    override suspend fun updateUserRole(userId: String, role: UserRole): AdminActionResponse =
        executor.execute("updateUserRole", getService = { getService() }) { updateUserRole(userId, role) }

    override suspend fun resetUserPassword(userId: String, newPasswordRaw: String): AdminActionResponse =
        executor.execute("resetUserPassword", getService = { getService() }) { resetUserPassword(userId, newPasswordRaw) }

    override suspend fun deleteUser(userId: String): AdminActionResponse =
        executor.execute("deleteUser", getService = { getService() }) { deleteUser(userId) }
        
    override suspend fun getGlobalIntegrationSettings(): GlobalIntegrationSettingsDto =
        executor.execute("getGlobalIntegrationSettings", getService = { getService() }) { getGlobalIntegrationSettings() }
        
    override suspend fun updateGlobalIntegrationSettings(request: UpdateGlobalIntegrationSettingsRequest): AdminActionResponse =
        executor.execute("updateGlobalIntegrationSettings", getService = { getService() }) { updateGlobalIntegrationSettings(request) }

    override suspend fun updateTmdbSettings(request: UpdateTmdbSettingsRequest): AdminActionResponse =
        executor.execute("updateTmdbSettings", getService = { getService() }) { updateTmdbSettings(request) }

    override suspend fun updateTorrServerSettings(request: UpdateTorrServerSettingsRequest): AdminActionResponse =
        executor.execute("updateTorrServerSettings", getService = { getService() }) { updateTorrServerSettings(request) }

    override suspend fun updateProwlarrSettings(request: UpdateProwlarrSettingsRequest): AdminActionResponse =
        executor.execute("updateProwlarrSettings", getService = { getService() }) { updateProwlarrSettings(request) }

    override suspend fun updateJackettSettings(request: UpdateJackettSettingsRequest): AdminActionResponse =
        executor.execute("updateJackettSettings", getService = { getService() }) { updateJackettSettings(request) }

    override suspend fun testTmdbConnection(token: String): AdminActionResponse =
        executor.execute("testTmdbConnection", getService = { getService() }) { testTmdbConnection(token) }

    override suspend fun testTorrServerConnection(host: String, login: String?, password: String?): AdminActionResponse =
        executor.execute("testTorrServerConnection", getService = { getService() }) { testTorrServerConnection(host, login, password) }

    override suspend fun testProwlarrConnection(url: String, apiKey: String): AdminActionResponse =
        executor.execute("testProwlarrConnection", getService = { getService() }) { testProwlarrConnection(url, apiKey) }

    override suspend fun testJackettConnection(url: String, apiKey: String): AdminActionResponse =
        executor.execute("testJackettConnection", getService = { getService() }) { testJackettConnection(url, apiKey) }

    override suspend fun getSystemInfo(): ServerSystemInfoDto =
        executor.execute("getSystemInfo", getService = { getService() }) { getSystemInfo() }

    override suspend fun clearDiscoverCache(): AdminActionResponse =
        executor.execute("clearDiscoverCache", getService = { getService() }) { clearDiscoverCache() }

    override suspend fun clearFeedCache(): AdminActionResponse =
        executor.execute("clearFeedCache", getService = { getService() }) { clearFeedCache() }

    override suspend fun clearMediaCache(): AdminActionResponse =
        executor.execute("clearMediaCache", getService = { getService() }) { clearMediaCache() }
}
