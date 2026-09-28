package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Tv
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.jetbrains.compose.resources.stringResource

fun formatSourceProviderTitle(sourceType: String?): String? {
    if (sourceType.isNullOrBlank()) return null
    return when (sourceType.lowercase().trim()) {
        "vk", "vk_video", "vkvideo" -> "VK Video"
        "rutube" -> "Rutube"
        "torrserver", "torrent", "torrents", "jackett", "prowlarr" -> "TorrServer"
        "anilibria" -> "AniLibria"
        "lampac" -> "Lampac"
        "collaps" -> "Collaps"
        else -> sourceType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

@Composable
fun WatchPartyMediaHeroCard(
    mediaTitle: String,
    seasonNum: Int?,
    episodeNum: Int?,
    sourceProvider: String?,
    sourceDetails: String?,
    backdropUrl: String?,
    isHost: Boolean,
    isSelectingSource: Boolean,
    onToggleSelectSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A22))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
    ) {
        // 1. Артворк бэкдропа
        if (!backdropUrl.isNullOrBlank()) {
            ShimmerImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.35f),
                                0.45f to Color.Black.copy(alpha = 0.55f),
                                1.0f to Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF222230),
                                Color(0xFF121218)
                            )
                        )
                    )
            )
        }

        // 2. Контент карточки
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхняя плашка типа медиа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF6C63FF).copy(alpha = 0.25f))
                        .border(1.dp, Color(0xFF6C63FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (seasonNum != null) Lucide.Tv else Lucide.Film,
                            contentDescription = null,
                            tint = Color(0xFFA59EFF),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (seasonNum != null) {
                                if (episodeNum != null) {
                                    stringResource(Res.string.watch_party_media_info_season_ep, seasonNum, episodeNum)
                                } else {
                                    stringResource(Res.string.watch_party_season_label) + " $seasonNum"
                                }
                            } else {
                                "Фильм"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Средний блок с названием фильма / сериала
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = mediaTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Нижняя строка источника + кнопка смены для хоста
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!sourceProvider.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF6C63FF).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF6C63FF).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = sourceProvider,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB4B0FF)
                            )
                        }
                    }

                    val detailsText = sourceDetails?.takeIf { it.isNotBlank() }
                        ?: if (sourceProvider.isNullOrBlank()) stringResource(Res.string.watch_party_source_selected) else null

                    if (!detailsText.isNullOrBlank()) {
                        Text(
                            text = detailsText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isHost) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelectingSource) Color.White.copy(alpha = 0.18f)
                                else Color(0xFF6C63FF).copy(alpha = 0.35f)
                            )
                            .border(
                                1.dp,
                                if (isSelectingSource) Color.White.copy(alpha = 0.3f)
                                else Color(0xFF6C63FF).copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = onToggleSelectSource)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.RefreshCw,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isSelectingSource) {
                                stringResource(Res.string.watch_party_source_close_selector)
                            } else {
                                stringResource(Res.string.watch_party_source_change)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
