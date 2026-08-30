package org.ensodai.avalonmediacard.presentation.screens.player.component.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Star
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvHorizontalFocusProvider
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

/**
 * Горизонтальная полка серий под ТВ-таргет (D-Pad навигация).
 * Сквозная прокрутка всех сезонов с автоматической синхронизацией активного сезона.
 */
@Composable
fun TvBottomEpisodeShelf(
    seasonEpisodes: Map<Int, List<MediaStream>>,
    currentStreamId: String = "",
    currentUrl: String? = null,
    currentEpisode: MediaStream? = null,
    onEpisodeClick: ((MediaStream) -> Unit)? = null,
    modifier: Modifier = Modifier,
    upTarget: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    if (seasonEpisodes.isEmpty()) return

    val grouped = remember(seasonEpisodes) {
        seasonEpisodes.toList().sortedBy { it.first }.toMap()
    }

    val episodesListState = rememberLazyListState()
    val tabState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Индексы начала каждого сезона в общем горизонтальном списке
    val seasonStartIndices = remember(grouped) {
        val indices = mutableMapOf<Int, Int>()
        var currentIndex = 0
        grouped.forEach { (season, eps) ->
            indices[season] = currentIndex
            if (grouped.size > 1) {
                currentIndex += 1 // Для карточки-разделителя сезона
            }
            currentIndex += eps.size
        }
        indices
    }

    val firstVisibleItemIndex by derivedStateOf { episodesListState.firstVisibleItemIndex }

    val activeSeason by remember(firstVisibleItemIndex, seasonStartIndices) {
        derivedStateOf {
            seasonStartIndices.entries
                .filter { it.value <= firstVisibleItemIndex }
                .maxByOrNull { it.value }?.key ?: seasonStartIndices.keys.firstOrNull() ?: 1
        }
    }

    LaunchedEffect(activeSeason) {
        val tabIndex = grouped.keys.toList().indexOf(activeSeason)
        if (tabIndex >= 0) {
            tabState.animateScrollToItem(tabIndex)
        }
    }

    var hasInitialScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(seasonEpisodes, currentStreamId, currentUrl) {
        if (!hasInitialScrolled && (currentStreamId.isNotBlank() || !currentUrl.isNullOrBlank()) && seasonEpisodes.isNotEmpty()) {
            var currentIndex = 0
            var targetIndex = -1
            for ((_, eps) in grouped) {
                if (grouped.size > 1) {
                    currentIndex += 1
                }
                val epIndex = eps.indexOfFirst {
                    (currentStreamId.isNotBlank() && it.canonicalId == currentStreamId) ||
                    (!currentUrl.isNullOrBlank() && it.url == currentUrl)
                }
                if (epIndex != -1) {
                    targetIndex = currentIndex + epIndex
                    break
                }
                currentIndex += eps.size
            }
            if (targetIndex != -1) {
                episodesListState.scrollToItem((targetIndex - 1).coerceAtLeast(0))
                hasInitialScrolled = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .onFocusChanged { state ->
                onFocusChanged(state.hasFocus)
            }
            .focusProperties {
                if (upTarget != null) {
                    up = upTarget
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(vertical = 8.dp)
        ) {
            // 1. Выбор сезона (Чипсы сезонов с автоматической синхронизацией)
            if (grouped.size > 1) {
                TvHorizontalFocusProvider(pivotFraction = 0.5f) {
                    LazyRow(
                        state = tabState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 2.dp)
                            .focusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(grouped.keys.toList(), key = { it }) { seasonNum ->
                            val isSelected = seasonNum == activeSeason
                            Box(
                                modifier = Modifier
                                    .tvAndWebHoverEffect(
                                        scaleTarget = 1.08f,
                                        activeBorderWidth = 2.dp,
                                        activeBorderColor = MaterialTheme.colorScheme.primary,
                                        defaultBorderWidth = 1.dp,
                                        defaultBorderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(14.dp),
                                        onClick = {
                                            coroutineScope.launch {
                                                val targetIndex = seasonStartIndices[seasonNum]
                                                if (targetIndex != null) {
                                                    episodesListState.animateScrollToItem(targetIndex)
                                                }
                                            }
                                        }
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.player_season_fmt, seasonNum),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // 2. Сквозная горизонтальная лента всех серий
            TvHorizontalFocusProvider(pivotFraction = 0.5f) {
                LazyRow(
                    state = episodesListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    grouped.forEach { (seasonNum, eps) ->
                        if (grouped.size > 1) {
                            item(key = "season_header_$seasonNum") {
                                SeasonDividerCard(seasonNumber = seasonNum)
                            }
                        }
                        items(eps, key = { it.canonicalId.ifBlank { it.url } }) { episode ->
                            val isPlayingThis = if (currentStreamId.isNotBlank()) {
                                episode.canonicalId == currentStreamId
                            } else if (!currentUrl.isNullOrBlank()) {
                                episode.url == currentUrl
                            } else false

                            TvEpisodeCard(
                                episode = episode,
                                isPlaying = isPlayingThis,
                                onClick = { onEpisodeClick?.invoke(episode) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка-разделитель сезонов в сквозном горизонтальном списке.
 */
@Composable
private fun SeasonDividerCard(seasonNumber: Int) {
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(vertical = 20.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.player_season_uppercase),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$seasonNumber",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * ТВ-карточка серии под D-Pad фокус.
 */
@Composable
private fun TvEpisodeCard(
    episode: MediaStream,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(235.dp)
            .tvAndWebHoverEffect(
                scaleTarget = 1.04f,
                activeBorderColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
                onClick = onClick
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .then(
                if (isPlaying) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            )
            .padding(5.dp)
    ) {
        // 1. Превью серии (16:9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            val posterUrl = episode.episodePosterUrl
            if (!posterUrl.isNullOrBlank()) {
                ShimmerImage(
                    model = posterUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(30.dp)
                )
            }

            if (isPlaying) {
                // Затемнение и яркая иконка воспроизведения
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = stringResource(Res.string.player_playing_now),
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Индикатор "Просмотрено" в правом верхнем углу
            if (episode.isWatched == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = stringResource(Res.string.player_watched),
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Прогресс-бар просмотра снизу постера
            val dur = episode.durationSeconds ?: 0.0
            val prog = episode.watchedProgressSeconds ?: 0L
            if (dur > 0 && prog > 0) {
                val fraction = (prog / dur).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Инфо о серии снизу
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            val epNumStr = episode.episodeNumber?.let { stringResource(Res.string.player_episode_fmt, it) } ?: ""
            val epTitle = episode.episodeName ?: episode.title
            val primaryText = if (epNumStr.isNotEmpty()) "$epNumStr: $epTitle" else epTitle

            Text(
                text = primaryText,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.95f),
                fontSize = 12.5.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Метаданные: длительность, оценка
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val duration = episode.durationSeconds
                val progress = episode.watchedProgressSeconds?.toDouble()

                if (duration != null && duration > 0) {
                    val mins = (duration / 60).toInt()
                    val text = if (progress != null && progress > 0 && episode.isWatched != true) {
                        val pMins = (progress / 60).toInt()
                        stringResource(Res.string.player_duration_mins_fmt, pMins, mins)
                    } else {
                        stringResource(Res.string.player_duration_mins_single_fmt, mins)
                    }
                    Text(
                        text = text,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                if (episode.userRating != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = stringResource(Res.string.player_rating_btn),
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = episode.userRating.toString(),
                        color = Color(0xFFFFC107),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
