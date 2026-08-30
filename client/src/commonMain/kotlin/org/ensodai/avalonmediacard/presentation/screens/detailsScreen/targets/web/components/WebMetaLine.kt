package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
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
        modifier = modifier
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
                        .width(52.dp)
                        .height(18.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
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
            val rating = header.rating ?: header.ratings.firstOrNull()?.value?.toDoubleOrNull()
            val year = header.releaseDate?.take(4)
            val genres = header.genres
            val mediaType = header.mediaType

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rating with Golden Star
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

                // Release Year
                if (!year.isNullOrBlank()) {
                    Text(
                        text = year,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (genres.isNotEmpty() || !mediaType.isNullOrBlank()) {
                        WebMetaDot()
                    }
                }

                // Media Type (if provided)
                val localizedMediaType = when (mediaType?.lowercase()) {
                    "movie" -> "Фильм"
                    "tv", "series" -> "Сериал"
                    else -> mediaType
                }
                if (!localizedMediaType.isNullOrBlank()) {
                    Text(
                        text = localizedMediaType,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (genres.isNotEmpty()) {
                        WebMetaDot()
                    }
                }

                // Genres
                if (genres.isNotEmpty()) {
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
private fun WebMetaDot() {
    Text(
        text = "·",
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}
