package org.ensodai.avalonmediacard.presentation.screens.dynamic.viewState

import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SduiViewState

data class DynamicViewState(
    override val loadingActions: Set<ServerAction> = emptySet(),
    val grid: SduiSlot<SlotData.Grid>? = null,
    val description: List<SduiSlot<SlotData.Text>> = emptyList()
) : SduiViewState
