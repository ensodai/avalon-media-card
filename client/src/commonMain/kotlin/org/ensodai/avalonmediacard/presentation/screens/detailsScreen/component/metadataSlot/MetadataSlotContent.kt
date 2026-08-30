package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.metadataSlot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.ClickstreamTargetType
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.telemetry.LocalTelemetryTracker

@Composable
fun MetadataSlotContent(
    data: SlotData.Header,
    isLoading: Boolean,
    onAction: (Action) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val telemetry = LocalTelemetryTracker.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val release = data.releaseDate
        if (!release.isNullOrEmpty() || isLoading) {
            val year = release?.split("-")?.firstOrNull() ?: release ?: "2024"
            MetadataPill(text = year, isLoading = isLoading)
        }
        if (isLoading) {
            MetadataPill(text = stringResource(Res.string.details_meta_series), isLoading = true)
        } else {
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
                if (!data.status.isNullOrEmpty()) {
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
            data.genres.forEach { genre ->
                MetadataPill(
                    text = genre.name
                )
            }
        }
    }
}