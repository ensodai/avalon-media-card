package org.ensodai.avalonmediacard.data.rpc

import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.rpc.ActionRpcService
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ServerAction

class ReconnectingActionRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : ActionRpcService {

    private suspend fun getService(): ActionRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun handleAction(action: ServerAction): ActionResult =
        executor.execute("handleAction", getService = { getService() }) { handleAction(action) }
}
