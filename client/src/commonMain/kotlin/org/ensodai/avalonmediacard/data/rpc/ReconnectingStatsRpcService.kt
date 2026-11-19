package org.ensodai.avalonmediacard.data.rpc

import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.model.UserStatsDto
import org.ensodai.avalonmediacard.contract.rpc.StatsRpcService

class ReconnectingStatsRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : StatsRpcService {

    private suspend fun getService(): StatsRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun getUserStats(): UserStatsDto =
        executor.execute("getUserStats", getService = { getService() }) { getUserStats() }
}
