package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.metadataSlot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun MetadataSlotContent(
    data: SlotData.Header,
    isLoading: Boolean,
    onAction: (Action) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            // Rating skeleton
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(18.dp)
                    .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
            )
            MetaDot()
            // Year skeleton
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(18.dp)
                    .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
            )
            MetaDot()
            // MediaType skeleton
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(22.dp)
                    .shimmerPlaceholder(true, RoundedCornerShape(50))
            )
            MetaDot()
            // Genres skeleton
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(18.dp)
                    .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
            )
        } else {
            val rating = data.rating ?: data.ratings.firstOrNull()?.value?.replace(',', '.')?.toDoubleOrNull()
            val release = data.releaseDate
            val year = release?.split("-")?.firstOrNull() ?: release?.take(4)

            // Rating with Golden Star (Web style)
            if (rating != null && rating > 0.0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(15.dp)
                    )
                    val formattedRating = if (rating % 1.0 == 0.0) {
                        rating.toInt().toString()
                    } else {
                        ((rating * 10).toInt() / 10.0).toString()
                    }
                    Text(
                        text = formattedRating,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                MetaDot()
            }

            if (!year.isNullOrBlank()) {
                Text(
                    text = year,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                MetaDot()
            }

            if (data.mediaType == "MOVIE") {
                MetadataPill(
                    text = stringResource(Res.string.details_meta_movie),
                    backgroundColor = Color(0xFF1E88E5).copy(alpha = 0.15f),
                    contentColor = Color(0xFF90CAF9),
                    borderColor = Color(0xFF1E88E5).copy(alpha = 0.3f)
                )
            } else if (data.mediaType == "TV") {
                MetadataPill(
                    text = stringResource(Res.string.details_meta_series),
                    backgroundColor = Color(0xFF8E24AA).copy(alpha = 0.15f),
                    contentColor = Color(0xFFE1BEE7),
                    borderColor = Color(0xFF8E24AA).copy(alpha = 0.3f)
                )
                if (!data.status.isNullOrBlank()) {
                    val statusText = when (data.status?.lowercase()) {
                        "идет", "ongoing", "returning series" -> stringResource(Res.string.details_meta_status_ongoing)
                        "завершен", "ended", "completed" -> stringResource(Res.string.details_meta_status_completed)
                        "отменен", "canceled", "cancelled" -> stringResource(Res.string.details_meta_status_canceled)
                        else -> data.status ?: ""
                    }
                    val (bg, fg, border) = when (data.status?.lowercase()) {
                        "идет", "ongoing", "returning series" -> Triple(
                            Color(0xFF4CAF50).copy(alpha = 0.15f),
                            Color(0xFFA5D6A7),
                            Color(0xFF4CAF50).copy(alpha = 0.3f)
                        )
                        "завершен", "ended", "completed" -> Triple(
                            Color(0xFF9E9E9E).copy(alpha = 0.15f),
                            Color(0xFFE0E0E0),
                            Color(0xFF9E9E9E).copy(alpha = 0.3f)
                        )
                        "отменен", "canceled", "cancelled" -> Triple(
                            Color(0xFFF44336).copy(alpha = 0.15f),
                            Color(0xFFEF9A9A),
                            Color(0xFFF44336).copy(alpha = 0.3f)
                        )
                        else -> Triple(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    }
                    MetadataPill(
                        text = statusText,
                        backgroundColor = bg,
                        contentColor = fg,
                        borderColor = border
                    )
                }
            }

            if (data.genres.isNotEmpty()) {
                MetaDot()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val displayGenres = data.genres.take(4)
                    displayGenres.forEachIndexed { index, genre ->
                        val clickAction = genre.clickAction
                        Text(
                            text = genre.name + if (index < displayGenres.lastIndex) "," else "",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .then(
                                    if (clickAction != null) {
                                        Modifier.clickable { onAction(clickAction) }
                                    } else Modifier
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaDot() {
    Text(
        text = "·",
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}