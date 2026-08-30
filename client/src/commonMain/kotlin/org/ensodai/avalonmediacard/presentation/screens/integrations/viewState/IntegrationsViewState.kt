package org.ensodai.avalonmediacard.presentation.screens.integrations.viewState

import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SduiViewState

data class IntegrationsViewState(
    override val loadingActions: Set<ServerAction> = emptySet(),
    val settingsGroups: List<SduiSlot<SlotData.SettingsGroup>> = emptyList()
) : SduiViewState
