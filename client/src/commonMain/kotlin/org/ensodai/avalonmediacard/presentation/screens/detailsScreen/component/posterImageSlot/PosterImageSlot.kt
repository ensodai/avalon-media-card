package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.posterImageSlot

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.core.SlotUiState

@Composable
fun PosterImageSlot(
    modifier: Modifier = Modifier,
    state: SlotUiState<SlotData.Header>?
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

    ShimmerImage(
        model = state.data?.posterUrl,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
        shape = RoundedCornerShape(12.dp),
        isLoading = state.isInitialLoading
    )
}
