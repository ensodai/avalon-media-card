package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.backdropImageSlot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.core.SlotUiState

private val logger = AppLogging.logger("BackdropImageSlot")

@Composable
fun BackdropImageSlot(
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
    
    val imageUrl = state.data?.let { it.backgroundUrl ?: it.posterUrl }
    if (imageUrl != null) {
        logger.d { "[PROFILING] BackdropImageSlot URL: $imageUrl (backgroundUrl: ${state.data?.backgroundUrl}, posterUrl: ${state.data?.posterUrl})" }
    }

    ShimmerImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        shape = RoundedCornerShape(16.dp),
        isLoading = state.isInitialLoading,
        errorBackground = Color.Black
    )
}
