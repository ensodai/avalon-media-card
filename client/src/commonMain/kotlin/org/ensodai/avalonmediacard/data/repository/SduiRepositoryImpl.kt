package org.ensodai.avalonmediacard.data.repository

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.rpc.ActionRpcService
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.rpc.SduiRpcService
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.domain.repository.SduiRepository
import org.koin.core.annotation.Single

@Single
class SduiRepositoryImpl(
    private val sduiRpcService: SduiRpcService,
    private val actionRpcService: ActionRpcService
) : SduiRepository {
    override fun streamScreen(screen: Screen): Flow<org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent> {
        return sduiRpcService.streamScreen(screen)
    }

    override suspend fun executeAction(action: ServerAction): ActionResult {
        return actionRpcService.handleAction(action)
    }
}
