package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.criticsRatingsSlot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.slot.MediaRatingItem
import org.ensodai.avalonmediacard.contract.slot.SlotData


@Composable
fun CriticsRatingsSlotContent(
    data: SlotData.Header,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val ratingVal = data.rating
    val ratingsList = if (data.ratings.isNotEmpty()) {
        data.ratings
    } else if (ratingVal != null && ratingVal > 0.0) {
        val ratingText = ((ratingVal * 10).toInt() / 10.0).toString()
        listOf(MediaRatingItem(source = "TMDB", value = ratingText))
    } else {
        emptyList()
    }

    if (ratingsList.isNotEmpty() || isLoading) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                RatingPill(source = "IMDb", value = "8.5", tint = Color.Transparent, isLoading = true)
                RatingPill(source = "KP", value = "8.0", tint = Color.Transparent, isLoading = true)
            } else {
                ratingsList.forEach { r ->
                    val ratingColor = getRatingColor(r.source, r.value)
                    RatingPill(source = r.source, value = r.value, tint = ratingColor, isLoading = false)
                }
            }
        }
    }
}

fun getRatingColor(source: String, value: String): Color {
    val cleanValue = value.replace("%", "").replace(",", ".").trim()
    val num = cleanValue.toDoubleOrNull() ?: return Color(0xFFFFC107)

    val is100Scale = value.contains("%") ||
            source.equals("Rotten Tomatoes", ignoreCase = true) ||
            source.equals("Metacritic", ignoreCase = true)

    val normalized = if (is100Scale) num / 10.0 else num

    return when {
        normalized >= 7.0 -> Color(0xFF4CAF50)
        normalized >= 5.0 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}