package org.ensodai.avalonmediacard.presentation.screens.person.viewState

import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SduiViewState

data class PersonViewState(
    override val loadingActions: Set<ServerAction> = emptySet(),
    val header: SduiSlot<SlotData.Header>? = null,
    val bio: SduiSlot<SlotData.Text>? = null,
    val credits: List<SduiSlot<SlotData.Carousel>> = emptyList()
) : SduiViewState
