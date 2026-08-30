package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaDescriptionSlot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SlotUiState

@Composable
fun MediaDescriptionSlot(
    modifier: Modifier = Modifier,
    state: SlotUiState<SlotData.Text>?
) {
    if (state == null) return
    if (state.hasError && state.error != null) {
        org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard(
            message = state.error,
            retryAction = state.retryAction,
            onAction = null,
            modifier = modifier
        )
        return
    }

    if (state.data == null && !state.isInitialLoading) return

    val data = state.data ?: SlotData.Text(content = "Loading description... This placeholder is used for skeleton layout sizing.")
    
    MediaDescriptionSlotContent(
        component = data,
        isLoading = state.isInitialLoading,
        modifier = modifier
    )
}

