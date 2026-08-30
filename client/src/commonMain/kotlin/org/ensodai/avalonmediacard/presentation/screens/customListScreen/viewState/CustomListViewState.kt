package org.ensodai.avalonmediacard.presentation.screens.customListScreen.viewState

import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SduiViewState

data class CustomListViewState(
    override val loadingActions: Set<ServerAction> = emptySet(),
    val grids: List<SduiSlot<SlotData.Grid>> = emptyList()
) : SduiViewState
