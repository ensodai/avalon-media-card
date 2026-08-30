package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TvEpisodeCard(
    episode: EpisodeItem,
    isCurrentFocused: Boolean,
    onFocus: () -> Unit,
    onPlay: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
            .tvAndWebHoverEffect(
                scaleTarget = 1.06f,
                activeBorderWidth = 2.dp,
                activeBorderColor = Color.White,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = if (isCurrentFocused) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                tiltEnabled = false,
                onClick = onPlay,
                onStateChange = { active ->
                    isFocused = active
                    if (active) onFocus()
                }
            )
            .background(Color(0xFF141418), RoundedCornerShape(12.dp))
    ) {
        // 16:9 Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color(0xFF0D0D10)),
            contentAlignment = Alignment.Center
        ) {
            val imageAlpha = if (episode.isWatched && !isFocused) 0.65f else 1.0f

            if (!episode.stillUrl.isNullOrEmpty() && episode.stillUrl != "placeholder") {
                ShimmerImage(
                    model = episode.stillUrl,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = imageAlpha }
                )
            } else {
                Icon(
                    imageVector = Lucide.Film,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Scrim Overlay when focused with center Play button
            androidx.compose.animation.AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = "Смотреть",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp).offset(x = 1.5.dp)
                        )
                    }
                }
            }

            // Duration badge (bottom-left)
            val runtime = episode.runtime
            if (runtime != null && runtime > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.player_duration_mins_single_fmt, runtime),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Watched checkmark badge (top-right)
            if (episode.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = "Просмотрено",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Title text under thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(
                text = "${episode.episodeNumber}. ${episode.name}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun TvEpisodeCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
            .background(Color(0xFF141418), RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shimmerPlaceholder(true, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(0.7f)
                .height(14.dp)
                .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}
