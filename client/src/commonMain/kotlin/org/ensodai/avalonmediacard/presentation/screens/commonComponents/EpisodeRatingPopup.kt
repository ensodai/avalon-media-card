package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star

@Composable
fun EpisodeRatingPopup(
    currentRating: Int?,
    maxRating: Int = 10,
    alignment: Alignment = Alignment.CenterEnd,
    offset: IntOffset = IntOffset(0, 0),
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit
) {
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss
    ) {
        var hoveredRating by remember { mutableStateOf<Int?>(null) }

        Row(
            modifier = Modifier
                .background(Color(0xCC111111), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..maxRating) {
                key(i) {
                    val isHoverActive = hoveredRating != null && i <= hoveredRating!!
                    val isDbActive = currentRating != null && i <= currentRating
                    val isActive = isHoverActive || (hoveredRating == null && isDbActive)

                    val starColor by animateColorAsState(if (isActive) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f))
                    val scale by animateFloatAsState(if (hoveredRating != null && i == hoveredRating) 1.25f else if (isActive) 1.1f else 1.0f)

                    Box(
                        modifier = Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .pointerInput(i) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.type) {
                                            PointerEventType.Enter -> hoveredRating =
                                                i

                                            PointerEventType.Exit -> hoveredRating =
                                                null
                                        }
                                    }
                                }
                            }
                            .clickable { onRate(i) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Star, contentDescription = null, tint = starColor, modifier = Modifier.size(24.dp))
                    }
                }
            }

            val displayedRating = hoveredRating ?: currentRating
            if (displayedRating != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$displayedRating",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}