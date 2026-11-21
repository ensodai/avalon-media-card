package org.ensodai.avalonmediacard.data.rpc

import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.model.DayActivityDto
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

    override suspend fun getDayActivity(date: String): DayActivityDto =
        executor.execute("getDayActivity", getService = { getService() }) { getDayActivity(date) }
}
