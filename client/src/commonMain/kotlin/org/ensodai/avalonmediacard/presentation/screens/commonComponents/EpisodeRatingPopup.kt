package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import org.ensodai.avalonmediacard.designsystem.components.AvalonStarRating
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

@Composable
fun EpisodeRatingPopup(
    currentRating: Int?,
    maxRating: Int = 10,
    alignment: Alignment = Alignment.CenterEnd,
    offset: IntOffset = IntOffset.Zero,
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .clip(shapes.control)
                .background(colors.bgOverlay)
                .border(1.dp, colors.borderOverlay, shapes.control)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            AvalonStarRating(
                rating = currentRating ?: 0,
                maxRating = maxRating,
                starSize = 22.dp,
                showValueLabel = true,
                onRatingSelected = onRate
            )
        }
    }
}

