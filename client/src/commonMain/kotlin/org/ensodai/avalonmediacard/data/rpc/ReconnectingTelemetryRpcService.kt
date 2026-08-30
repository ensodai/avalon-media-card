package org.ensodai.avalonmediacard.data.rpc

import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.model.TelemetryEvent
import org.ensodai.avalonmediacard.contract.rpc.TelemetryRpcService

class ReconnectingTelemetryRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : TelemetryRpcService {

    private suspend fun getService(): TelemetryRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun logEvent(event: TelemetryEvent) =
        executor.execute("logEvent", getService = { getService() }) { logEvent(event) }
}
