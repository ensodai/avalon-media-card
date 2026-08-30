package org.ensodai.avalonmediacard.presentation.screens.search.viewState

import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SduiViewState

data class SearchViewState(
    override val loadingActions: Set<ServerAction> = emptySet(),
    val resultsGrid: SduiSlot<SlotData.Grid>? = null
) : SduiViewState
