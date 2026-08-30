package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
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
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.horizontalScrollWithMouseAndTouch
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource
import kotlin.collections.iterator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeListPanel(
    seasonEpisodes: Map<Int, List<MediaStream>>,
    currentStreamId: String = "",
    currentUrl: String? = null,
    currentEpisode: MediaStream? = null,
    listState: LazyListState = rememberLazyListState(),
    tabState: LazyListState = rememberLazyListState(),
    onEpisodeClick: ((MediaStream) -> Unit)?
) {
    val grouped = remember(seasonEpisodes) {
        seasonEpisodes.toList().sortedBy { it.first }.toMap()
    }
    val seasonIndices = remember(grouped) {
        val indices = mutableMapOf<Int, Int>()
        var currentIndex = 0
        grouped.forEach { (season, eps) ->
            indices[season] = currentIndex
            currentIndex += 1 // For header
            currentIndex += eps.size // For items
        }
        indices
    }
    val coroutineScope = rememberCoroutineScope()

    val firstVisibleItemIndex by derivedStateOf { listState.firstVisibleItemIndex }

    val activeSeason by remember(firstVisibleItemIndex, seasonIndices) {
        derivedStateOf {
            seasonIndices.entries
                .filter { it.value <= firstVisibleItemIndex }
                .maxByOrNull { it.value }?.key ?: seasonIndices.keys.firstOrNull() ?: 1
        }
    }

    LaunchedEffect(activeSeason) {
        val tabIndex = grouped.keys.toList().indexOf(activeSeason)
        if (tabIndex >= 0) {
            tabState.animateScrollToItem(tabIndex)
        }
    }

    var lastScrolledEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentStreamId, currentUrl, currentEpisode?.canonicalId, grouped.keys) {
        val targetEp = currentEpisode
            ?: grouped.values.flatten().find { ep ->
                (currentStreamId.isNotBlank() && ep.canonicalId == currentStreamId) ||
                (!currentUrl.isNullOrBlank() && ep.url == currentUrl)
            }

        val epId = targetEp?.canonicalId?.takeIf { it.isNotBlank() }
            ?: targetEp?.let { "${it.seasonNumber}_${it.episodeNumber}" }

        if (targetEp != null && epId != null && grouped.isNotEmpty()) {
            val isNewEpisode = lastScrolledEpisodeId != epId

            if (isNewEpisode) {
                var currentIndex = 0
                var targetIndex = -1

                for ((_, eps) in grouped) {
                    currentIndex += 1 // Заголовок сезона
                    val episodeIndex = eps.indexOfFirst { ep ->
                        (ep.canonicalId.isNotBlank() && ep.canonicalId == targetEp.canonicalId) ||
                        (ep.seasonNumber == targetEp.seasonNumber && ep.episodeNumber == targetEp.episodeNumber) ||
                        (!currentUrl.isNullOrBlank() && ep.url == currentUrl)
                    }
                    if (episodeIndex != -1) {
                        targetIndex = currentIndex + episodeIndex
                        break
                    }
                    currentIndex += eps.size
                }

                if (targetIndex != -1) {
                    val isAlreadyVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
                    if (lastScrolledEpisodeId == null) {
                        listState.scrollToItem(targetIndex)
                    } else if (!isAlreadyVisible) {
                        listState.animateScrollToItem(targetIndex)
                    }
                }
                lastScrolledEpisodeId = epId
            }
        }
    }


    val isMapped = remember(seasonEpisodes) { seasonEpisodes.values.flatten().any { it.isMapped } }

    Column(modifier = Modifier.fillMaxSize()) {
        if (grouped.size > 1) {
            LazyRow(
                state = tabState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isMapped) 16.dp else 24.dp, bottom = 16.dp)
                    .horizontalScrollWithMouseAndTouch(tabState, wheelSpeedMultiplier = 0.35f),
                horizontalArrangement = Arrangement.spacedBy(12.dp), // Чуть больше воздуха между самими кнопками
                contentPadding = PaddingValues(horizontal = 24.dp) // Чуть больше отступ от краев экрана
            ) {
                items(grouped.keys.toList(), key = { it }) { season ->
                    val isActive = season == activeSeason
                    Box(
                        modifier = Modifier
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.05f,
                                shape = RoundedCornerShape(16.dp),
                                activeBorderColor = Color.Transparent
                            ,
    onClick = {
                                coroutineScope.launch {
                                    val index = seasonIndices[season]
                                    if (index != null) {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            })
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Transparent)
                            .then(
                                if (isActive) Modifier.border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                )
                                else Modifier
                            )
                            
                            .padding(horizontal = 20.dp, vertical = 10.dp), // Сами кнопки делаем чуть пухлее
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.player_season_fmt, season),
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // 2. Увеличиваем отступ до первого элемента списка
            contentPadding = PaddingValues(bottom = 24.dp, top = if (grouped.size > 1) 8.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            grouped.forEach { (season, eps) ->
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 12.dp, start = 24.dp, end = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.player_season_fmt, season),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                items(eps, key = { it.canonicalId.ifBlank { it.url } }) { episode ->
                    val isPlaying = if (currentStreamId.isNotBlank()) {
                        episode.canonicalId == currentStreamId
                    } else if (!currentUrl.isNullOrBlank()) {
                        episode.url == currentUrl
                    } else false

                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        PlayerEpisodeCard(
                            source = episode,
                            isPlaying = isPlaying,
                            onClick = { onEpisodeClick?.invoke(episode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerEpisodeCard(
    source: MediaStream,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = if (isHovered) Color.White.copy(alpha = 0.08f) else Color.Transparent,
        label = "episode_bg_color"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(vertical = 2.dp) // Небольшой отступ между элементами
            .tvAndWebHoverEffect(
                scaleTarget = 1.0f, // Без увеличения
                shape = RoundedCornerShape(8.dp),
                activeBorderColor = Color.Transparent,
                onStateChange = { isHovered = it }
            ,
    onClick = { onClick() })
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            
            .padding(4.dp), // Внутренний отступ, чтобы картинка не прилипала к краям выделения
        verticalAlignment = Alignment.CenterVertically
    ) {
        val posterUrl = source.episodePosterUrl

        Box(
            modifier = Modifier
                .width(114.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            if (!posterUrl.isNullOrBlank()) {
                ShimmerImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isPlaying) {
                // Затемняем картинку, чтобы иконка Play была лучше видна
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = stringResource(Res.string.player_now_playing),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            } else if (posterUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }

        }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
            val epNum = source.episodeNumber
            val epName = source.episodeName

            val primaryText = if (!epName.isNullOrBlank() && epName != source.title) {
                if (epNum != null) {
                    val cleanName = if (epName.startsWith("$epNum. ")) epName.removePrefix("$epNum. ").trim() else epName
                    "$epNum. $cleanName"
                } else {
                    epName
                }
            } else if (epNum != null) {
                stringResource(Res.string.player_episode_fmt, epNum)
            } else {
                source.title
            }

            Text(
                text = primaryText,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White.copy(
                    alpha = 0.9f
                ),
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            val duration = source.durationSeconds
            val progress = source.watchedProgressSeconds?.toDouble()

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (duration != null && duration > 0) {
                    val mins = (duration / 60).toInt()
                    val text = if (progress != null && progress > 0 && !source.isWatched) {
                        val pMins = (progress / 60).toInt()
                        stringResource(Res.string.player_duration_mins_fmt, pMins, mins)
                    } else {
                        stringResource(Res.string.player_duration_mins_single_fmt, mins)
                    }
                    Text(
                        text = text,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                if (source.isWatched) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = stringResource(Res.string.player_watched),
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                if (source.userRating != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = stringResource(Res.string.player_rate),
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = source.userRating.toString(),
                        color = Color(0xFFFFC107),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (progress != null && duration != null && duration > 0 && !source.isWatched) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (progress / duration).toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodesShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(top = 24.dp)) {
        // Shimmer header (Seasons tab bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .height(40.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        )
        // Shimmer items
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                // Poster shimmer
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(80.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                // Text shimmer
                Column(
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.8f).height(16.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(0.5f).height(12.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
