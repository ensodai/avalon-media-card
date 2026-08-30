package org.ensodai.avalonmediacard.data.rpc

import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.model.UserSettingsDto
import org.ensodai.avalonmediacard.contract.rpc.UserSettingsRpcService

class ReconnectingUserSettingsRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : UserSettingsRpcService {

    private suspend fun getService(): UserSettingsRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun getUserSettings(): UserSettingsDto =
        executor.execute("getUserSettings", getService = { getService() }) { getUserSettings() }

    override suspend fun updateUserSettings(settings: UserSettingsDto): Boolean =
        executor.execute("updateUserSettings", getService = { getService() }) { updateUserSettings(settings) }
}
