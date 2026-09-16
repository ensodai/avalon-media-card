package org.ensodai.avalonmediacard.presentation.screens.mediaSources.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.details_sources_found_episodes
import avalonmediacard.client.generated.resources.details_sources_found_episodes_all
import avalonmediacard.client.generated.resources.details_sources_found_episodes_of
import avalonmediacard.client.generated.resources.details_sources_watch
import avalonmediacard.client.generated.resources.player_duration_mins_single_fmt
import avalonmediacard.client.generated.resources.player_episode_fmt
import com.composables.icons.lucide.CircleArrowDown
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.HardDrive
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Tv
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.MovieSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SeasonGroupSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SingleEpisodeSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.TorrentSourceUiItem
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ln
import kotlin.math.pow

@Composable
internal fun SourceTab(
    text: String,
    count: Int,
    isSelected: Boolean,
    isTv: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)
    )
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White

    Row(
        modifier = modifier
            .tvAndWebHoverEffect(scaleTarget = 1.05f, shape = RoundedCornerShape(24.dp), onClick = onClick)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .padding(horizontal = if (isTv) 20.dp else 16.dp, vertical = if (isTv) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (isTv) 18.sp else 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (isTv) 16.dp else 12.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else if (count > 0) {
            Box(
                modifier = Modifier.background(
                    if (isSelected) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(4.dp)
                ).padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = textColor,
                    fontSize = if (isTv) 14.sp else 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun SubFilterChip(
    label: String,
    count: Int?,
    isSelected: Boolean,
    isTv: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else Color.White.copy(alpha = 0.05f)
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else Color.White.copy(alpha = 0.12f)
    )
    val textColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else Color.White.copy(alpha = 0.75f)
    )

    Row(
        modifier = Modifier
            .tvAndWebHoverEffect(scaleTarget = 1.05f, shape = RoundedCornerShape(20.dp), onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (isTv) 13.sp else 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
        if (count != null && count > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$count",
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.9f),
                    fontSize = if (isTv) 11.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun MovieSourceCard(
    item: MovieSourceUiItem,
    isTv: Boolean,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.4f else 1f
    val titleSize = if (isTv) 18.sp else 16.sp
    val subtitleSize = if (isTv) 14.sp else 13.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (!isDisabled && !isLoading) Modifier.tvAndWebHoverEffect(scaleTarget = 1.02f, shape = RoundedCornerShape(12.dp), onClick = onClick)
                else Modifier
            )
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(if (isTv) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!item.durationFormatted.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Lucide.Clock,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(subtitleSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.durationFormatted,
                            color = Color(0xFF4CAF50),
                            fontSize = subtitleSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (!item.channel.isNullOrBlank()) {
                    Text(
                        text = "•  ${item.channel}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = subtitleSize,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            QualityPill(quality = item.quality, isTv = isTv)
        }
    }
}

@Composable
internal fun SeasonGroupSourceCard(
    item: SeasonGroupSourceUiItem,
    isTv: Boolean,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.4f else 1f
    val titleSize = if (isTv) 18.sp else 16.sp
    val subtitleSize = if (isTv) 14.sp else 13.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (!isDisabled && !isLoading) Modifier.tvAndWebHoverEffect(scaleTarget = 1.02f, shape = RoundedCornerShape(12.dp), onClick = onClick)
                else Modifier
            )
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(if (isTv) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Lucide.Tv,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(subtitleSize.value.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val episodesSummary = when {
                        item.episodesTotal != null && item.episodesTotal > 0 && item.episodesCount >= item.episodesTotal ->
                            stringResource(Res.string.details_sources_found_episodes_all, item.episodesCount)
                        item.episodesTotal != null && item.episodesTotal > 0 ->
                            stringResource(Res.string.details_sources_found_episodes_of, item.episodesCount, item.episodesTotal)
                        else ->
                            stringResource(Res.string.details_sources_found_episodes, item.episodesCount)
                    }
                    Text(
                        text = episodesSummary,
                        color = Color(0xFF4CAF50),
                        fontSize = subtitleSize,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!item.channel.isNullOrBlank()) {
                    Text(
                        text = "•  ${item.channel}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = subtitleSize,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            QualityPill(quality = item.quality, isTv = isTv)
        }
    }
}

@Composable
internal fun SingleEpisodeSourceCard(
    item: SingleEpisodeSourceUiItem,
    isTv: Boolean,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.4f else 1f
    val titleSize = if (isTv) 18.sp else 16.sp
    val subtitleSize = if (isTv) 14.sp else 13.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (!isDisabled && !isLoading) Modifier.tvAndWebHoverEffect(scaleTarget = 1.02f, shape = RoundedCornerShape(12.dp), onClick = onClick)
                else Modifier
            )
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(if (isTv) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!item.channel.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Lucide.Tv,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(subtitleSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.channel,
                            color = Color(0xFF4CAF50),
                            fontSize = subtitleSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!item.durationFormatted.isNullOrBlank()) {
                    Text(
                        text = "•  ${item.durationFormatted}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = subtitleSize,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            QualityPill(quality = item.quality, isTv = isTv)
        }
    }
}

@Composable
internal fun TorrentSourceCard(
    item: TorrentSourceUiItem,
    isTv: Boolean,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.4f else 1f
    val titleSize = if (isTv) 18.sp else 15.sp
    val statSize = if (isTv) 14.sp else 12.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (!isDisabled && !isLoading) Modifier.tvAndWebHoverEffect(
                    scaleTarget = 1.02f,
                    shape = RoundedCornerShape(12.dp),
                    onClick = onClick
                )
                else Modifier
            )
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(if (isTv) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (item.sizeBytes != null && item.sizeBytes > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Lucide.HardDrive,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(statSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatBytes(item.sizeBytes),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = statSize
                        )
                    }
                }

                if (item.seeders != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Lucide.CircleArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(statSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.seeders}",
                            color = Color(0xFF4CAF50),
                            fontSize = statSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (item.leechers != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Lucide.CircleArrowDown,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(statSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.leechers}",
                            color = Color(0xFFF44336),
                            fontSize = statSize
                        )
                    }
                }
            }
        }

        if (!item.quality.isNullOrEmpty() || item.format != null || item.videoCodec != null || item.audioCodec != null || item.isHdr || item.sourceName.isNotBlank()) {
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!item.quality.isNullOrEmpty()) {
                    QualityPill(quality = item.quality, isTv = isTv)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.sourceName.isNotBlank()) MetadataBadge(item.sourceName, Color(0xFF2196F3), isTv = isTv)
                    if (item.isHdr) MetadataBadge("HDR", Color(0xFFE0A96D), isTv = isTv)
                    if (item.videoCodec != null) MetadataBadge(item.videoCodec, Color.White.copy(alpha = 0.6f), isTv = isTv)
                    if (item.audioCodec != null) MetadataBadge(item.audioCodec, Color.White.copy(alpha = 0.6f), isTv = isTv)
                    if (item.format != null) {
                        val isUnsupported = item.format == "AVI"
                        MetadataBadge(
                            item.format,
                            if (isUnsupported) Color(0xFFF44336) else Color.White.copy(alpha = 0.6f),
                            isTv = isTv
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MetadataBadge(text: String, tint: Color, isTv: Boolean) {
    Box(
        modifier = Modifier
            .background(tint.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .border(1.dp, tint.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = if (isTv) 6.dp else 4.dp, vertical = if (isTv) 4.dp else 2.dp)
    ) {
        Text(text = text, color = tint, fontSize = if (isTv) 12.sp else 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun QualityPill(quality: String, isTv: Boolean) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = if (isTv) 8.dp else 6.dp, vertical = if (isTv) 4.dp else 2.dp)
    ) {
        Text(text = quality, color = Color.White, fontSize = if (isTv) 14.sp else 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun MappedEpisodeCard(source: MediaStream, isTv: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(scaleTarget = 1.02f, shape = RoundedCornerShape(12.dp), onClick = { onClick() })
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        Color(0xFF1A1A2E).copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val posterUrl = source.episodePosterUrl
        if (posterUrl != null && posterUrl.isNotBlank()) {
            ShimmerImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.Play,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            val epName = source.episodeName
            val primaryText = if (!epName.isNullOrBlank() && epName != source.title) {
                if (source.episodeNumber != null) "${source.episodeNumber}. $epName" else epName
            } else if (source.seasonNumber != null && source.episodeNumber != null) {
                stringResource(Res.string.player_episode_fmt, source.episodeNumber ?: 0)
            } else {
                source.title
            }

            Text(
                text = primaryText,
                color = Color.White,
                fontSize = if (isTv) 16.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (source.durationSeconds != null && source.durationSeconds!! > 0) {
                val mins = (source.durationSeconds!! / 60).toInt()
                Text(
                    text = stringResource(Res.string.player_duration_mins_single_fmt, mins),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = if (isTv) 14.sp else 12.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.details_sources_watch),
                color = MaterialTheme.colorScheme.primary,
                fontSize = if (isTv) 14.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    val value = bytes / 1024.0.pow(exp.toDouble())

    return "${(value * 10).toInt() / 10.0} ${pre}B"
}
