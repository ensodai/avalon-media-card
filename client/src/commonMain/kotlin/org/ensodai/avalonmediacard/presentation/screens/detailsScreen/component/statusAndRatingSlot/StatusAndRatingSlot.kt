package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.statusAndRatingSlot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.contract.model.MediaStatus

@Composable
fun StatusAndRatingSlot(
    state: SlotUiState<SlotData.UserActions>?,
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
    val data = state.data ?: SlotData.UserActions(
        MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "skeleton"),
        MediaStatus.NONE,
        null
    )
    StatusAndRatingSlotContent(
        component = data,
        isLoading = state.isInitialLoading,
        onAction = onAction
    )
}