package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.GenreItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder

@Composable
fun WebMetaLine(
    headerData: SlotData.Header?,
    isLoading: Boolean = false,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = isLoading,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        modifier = modifier,
        label = "WebMetaLineCrossfade"
    ) { loading ->
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rating skeleton
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(18.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
                WebMetaDot()
                // Year skeleton
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(18.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
                WebMetaDot()
                // MediaType skeleton
                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .height(22.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(50))
                )
                // Status skeleton
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(22.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(50))
                )
                WebMetaDot()
                // Genres skeleton
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(18.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
            }
        } else {
            val header = headerData ?: return@Crossfade
            val rating = header.rating ?: header.ratings.firstOrNull()?.value?.replace(',', '.')?.toDoubleOrNull()
            val year = header.releaseDate?.split("-")?.firstOrNull() ?: header.releaseDate?.take(4)
            val genres = header.genres
            val mediaType = header.mediaType?.uppercase()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Rating with Golden Star
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
                    WebMetaDot()
                }

                // 2. Release Year
                if (!year.isNullOrBlank()) {
                    Text(
                        text = year,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    WebMetaDot()
                }

                // 3. Media Type Badge (Фильм / Сериал)
                if (mediaType == "MOVIE") {
                    WebMetaBadge(
                        text = stringResource(Res.string.details_meta_movie),
                        backgroundColor = Color(0xFF1E88E5).copy(alpha = 0.15f),
                        contentColor = Color(0xFF90CAF9),
                        borderColor = Color(0xFF1E88E5).copy(alpha = 0.3f)
                    )
                } else if (mediaType == "TV") {
                    WebMetaBadge(
                        text = stringResource(Res.string.details_meta_series),
                        backgroundColor = Color(0xFF8E24AA).copy(alpha = 0.15f),
                        contentColor = Color(0xFFE1BEE7),
                        borderColor = Color(0xFF8E24AA).copy(alpha = 0.3f)
                    )

                    // 4. Status Badge (Идет / Завершен / Отменен)
                    val status = header.status
                    if (!status.isNullOrBlank()) {
                        val statusText = when (status.lowercase()) {
                            "идет", "ongoing", "returning series" -> stringResource(Res.string.details_meta_status_ongoing)
                            "завершен", "ended", "completed" -> stringResource(Res.string.details_meta_status_completed)
                            "отменен", "canceled", "cancelled" -> stringResource(Res.string.details_meta_status_canceled)
                            else -> status
                        }
                        val (bg, fg, border) = when (status.lowercase()) {
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
                        WebMetaBadge(
                            text = statusText,
                            backgroundColor = bg,
                            contentColor = fg,
                            borderColor = border
                        )
                    }
                }

                // 5. Genres
                if (genres.isNotEmpty()) {
                    WebMetaDot()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        genres.take(4).forEachIndexed { index, genre ->
                            Text(
                                text = genre.name + if (index < genres.take(4).lastIndex) "," else "",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (genre.clickAction != null) {
                                            Modifier
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable { onAction(genre.clickAction!!) }
                                        } else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebMetaBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(50))
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WebMetaDot() {
    Text(
        text = "·",
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}
