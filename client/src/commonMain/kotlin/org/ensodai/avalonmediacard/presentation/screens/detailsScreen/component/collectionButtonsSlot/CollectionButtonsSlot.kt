package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ButtonItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SlotUiState

@Composable
fun CollectionButtonsSlot(
    state: SlotUiState<SlotData.ButtonGroup>?,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state == null) return
    val data = state.data ?: SlotData.ButtonGroup(listOf(ButtonItem("")))
    CollectionButtonsSlotContent(
        data = data,
        isLoading = state.isInitialLoading,
        onAction = onAction,
        modifier = modifier
    )
}