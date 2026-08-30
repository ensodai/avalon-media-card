package org.ensodai.avalonmediacard.domain.repository

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ServerAction

interface SduiRepository {
    fun streamScreen(screen: Screen): Flow<org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent>
    suspend fun executeAction(action: ServerAction): ActionResult
}
