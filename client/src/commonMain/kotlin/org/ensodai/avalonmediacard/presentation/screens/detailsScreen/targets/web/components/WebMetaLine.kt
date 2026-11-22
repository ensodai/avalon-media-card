package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.designsystem.components.AvalonMetaLine
import org.ensodai.avalonmediacard.designsystem.components.MetaSegment
import org.ensodai.avalonmediacard.designsystem.components.StatusTone
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
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(18.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(18.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .height(18.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(140.dp)
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

            val segments = buildList {
                // 1. Rating
                if (rating != null && rating > 0.0) {
                    val formattedRating = if (rating % 1.0 == 0.0) {
                        rating.toInt().toString()
                    } else {
                        ((rating * 10).toInt() / 10.0).toString()
                    }
                    add(MetaSegment.Rating(formattedRating))
                }

                // 2. Year
                if (!year.isNullOrBlank()) {
                    add(MetaSegment.Fact(year))
                }

                // 3. Media Type (Фильм / Сериал)
                if (mediaType == "MOVIE") {
                    add(MetaSegment.Fact(stringResource(Res.string.details_meta_movie)))
                } else if (mediaType == "TV") {
                    add(MetaSegment.Fact(stringResource(Res.string.details_meta_series)))

                    // 4. Ongoing status
                    val status = header.status
                    if (!status.isNullOrBlank()) {
                        val isOngoing = when (status.lowercase()) {
                            "идет", "ongoing", "returning series" -> true
                            else -> false
                        }
                        val isCanceled = when (status.lowercase()) {
                            "отменен", "canceled", "cancelled" -> true
                            else -> false
                        }
                        val statusText = when (status.lowercase()) {
                            "идет", "ongoing", "returning series" -> stringResource(Res.string.details_meta_status_ongoing)
                            "завершен", "ended", "completed" -> stringResource(Res.string.details_meta_status_completed)
                            "отменен", "canceled", "cancelled" -> stringResource(Res.string.details_meta_status_canceled)
                            else -> status
                        }
                        val tone = when {
                            isOngoing -> StatusTone.Live
                            isCanceled -> StatusTone.Danger
                            else -> StatusTone.Warn
                        }
                        add(MetaSegment.StatusDot(label = statusText, tone = tone))
                    }
                }

                // 5. Genres
                if (genres.isNotEmpty()) {
                    val genresText = genres.take(4).mapIndexed { idx, g ->
                        val name = g.name.trim()
                        if (idx == 0) {
                            name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        } else {
                            name.lowercase()
                        }
                    }.joinToString(", ")
                    add(MetaSegment.Fact(genresText))
                }
            }

            AvalonMetaLine(segments = segments)
        }
    }
}
