package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.castSlot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.jetbrains.compose.resources.stringResource

@Composable
fun CastSlot(
    state: SlotUiState<SlotData.Cast>?,
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
    val defaultTitle = stringResource(Res.string.details_cast_title)
    val data = state.data ?: SlotData.Cast(title = defaultTitle, members = emptyList())
    CastSlotContent(
        component = data,
        isLoading = state.isInitialLoading,
        onAction = onAction,
        modifier = modifier
    )
}