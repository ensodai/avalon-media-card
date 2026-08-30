package org.ensodai.avalonmediacard.presentation.components.textSlot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerEffect
import org.ensodai.avalonmediacard.presentation.core.SlotUiState


@Composable
fun TextSlot(
    state: SlotUiState<SlotData.Text>,
    modifier: Modifier = Modifier
) {
    if (state.hasError && state.error != null) {
        org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard(
            message = state.error,
            retryAction = state.retryAction,
            onAction = {},
            modifier = modifier
        )
        return
    }
    val data = state.data
    if (state.isInitialLoading || data == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .shimmerEffect()
        )
    } else {
        Text(
            text = data.content,
            color = Color.White,
            modifier = modifier
        )
    }
}
