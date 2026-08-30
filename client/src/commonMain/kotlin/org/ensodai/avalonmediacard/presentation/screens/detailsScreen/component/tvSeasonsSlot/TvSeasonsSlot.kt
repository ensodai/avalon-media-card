package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.tvSeasonsSlot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SlotUiState


@Composable
fun TvSeasonsSlot(
    state: SlotUiState<SlotData.TvSeasons>?,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state == null) return
    if (state.hasError && state.error != null) {
        org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard(
            message = state.error,
            retryAction = state.retryAction,
            onAction = onAction,
            modifier = modifier
        )
        return
    }
    val data = state.data ?: SlotData.TvSeasons(emptyList(), 1, emptyMap())
    TvSeasonsSlotContent(
        component = data,
        isLoading = state.isInitialLoading,
        onAction = onAction,
        modifier = modifier
    )
}