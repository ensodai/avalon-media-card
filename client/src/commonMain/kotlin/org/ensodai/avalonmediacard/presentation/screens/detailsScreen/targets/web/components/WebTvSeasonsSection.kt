package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.contract.slot.RateEpisodeCommand
import org.ensodai.avalonmediacard.contract.slot.SeasonItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.EpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val EPISODES_PAGE_SIZE = 6

@Composable
fun WebTvSeasonsSection(
    state: SlotUiState<SlotData.TvSeasons>?,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state == null) return

    if (state.hasError && state.error != null) {
        SlotErrorCard(
            message = state.error,
            retryAction = state.retryAction,
            onAction = onAction,
            modifier = modifier
        )
        return
    }

    val component = state.data ?: SlotData.TvSeasons(emptyList(), 1, emptyMap())
    val isLoading = state.isLoading || state.isInitialLoading

    if (component.seasons.isEmpty() && !isLoading) return

    val selectedSeason = component.seasons.find { it.seasonNumber == component.selectedSeasonNumber }
        ?: component.seasons.firstOrNull()

    val regularSeasons = remember(component.seasons) {
        component.seasons.filter { it.seasonNumber > 0 }
    }
    val hasSpecials = remember(component.seasons) {
        component.seasons.any { it.seasonNumber == 0 }
    }
    val totalRegularEpisodes = remember(regularSeasons) {
        regularSeasons.sumOf { it.episodeCount }
    }
    val seasonsCount = if (regularSeasons.isNotEmpty()) regularSeasons.size else component.seasons.size
    val totalEpisodes = if (regularSeasons.isNotEmpty()) totalRegularEpisodes else component.seasons.sumOf { it.episodeCount }

    val activeSeasonContent = component.seasonContents[component.selectedSeasonNumber]
    val isEpisodesLoading = isLoading || activeSeasonContent?.isLoading == true || (activeSeasonContent == null && component.seasons.isNotEmpty())
    val episodes = activeSeasonContent?.episodes.orEmpty()

    // Native HorizontalPager State
    val totalPages = remember(episodes) {
        if (episodes.isNotEmpty()) {
            (episodes.size + EPISODES_PAGE_SIZE - 1) / EPISODES_PAGE_SIZE
        } else 1
    }

    val pagerState = rememberPagerState(initialPage = 0) { totalPages }
    val coroutineScope = rememberCoroutineScope()

    val deviceTarget = LocalDeviceTarget.current
    val isTouch = deviceTarget.isTouch

    var activeTouchEpisodeId by remember { mutableStateOf<String?>(null) }

    // Reset touch selection when season or page changes
    LaunchedEffect(component.selectedSeasonNumber, pagerState.currentPage) {
        activeTouchEpisodeId = null
    }

    val density = LocalDensity.current
    var rightSectionHeightDp by remember { mutableStateOf<Dp?>(null) }

    val seasonsScrollState = rememberLazyListState()

    // Auto-scroll sidebar to the selected season when selected season changes
    val selectedSeasonIndex = remember(component.selectedSeasonNumber, component.seasons) {
        component.seasons.indexOfFirst { it.seasonNumber == component.selectedSeasonNumber }
    }
    LaunchedEffect(selectedSeasonIndex) {
        if (selectedSeasonIndex >= 0) {
            seasonsScrollState.animateScrollToItem(selectedSeasonIndex)
        }
    }

    var lastScrolledSeasonNumber by remember { mutableStateOf<Int?>(null) }

    // Auto-scroll to first unwatched episode ONCE per season load
    LaunchedEffect(component.selectedSeasonNumber, isEpisodesLoading) {
        if (!isEpisodesLoading && episodes.isNotEmpty() && lastScrolledSeasonNumber != component.selectedSeasonNumber) {
            lastScrolledSeasonNumber = component.selectedSeasonNumber
            val firstUnwatchedIdx = episodes.indexOfFirst { !it.isWatched }
            val targetPage = if (firstUnwatchedIdx > 0) {
                (firstUnwatchedIdx / EPISODES_PAGE_SIZE).coerceIn(0, totalPages - 1)
            } else 0
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    // Outer Glassmorphic Card Container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // === CARD HEADER ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.details_seasons_header),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (!isLoading && component.seasons.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            val seasonRes = getPluralSeasonRes(seasonsCount)
                            val episodeRes = getPluralEpisodeRes(totalEpisodes)
                            val seasonCountText = stringResource(seasonRes, seasonsCount)
                            val episodesText = if (totalEpisodes > 0) " • " + stringResource(episodeRes, totalEpisodes) else ""
                            val specialsText = if (hasSpecials && regularSeasons.isNotEmpty()) " • " + stringResource(Res.string.details_seasons_specials) else ""
                            Text(
                                text = "$seasonCountText$episodesText$specialsText",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Page Navigation Arrows (Top Right)
                if (!isEpisodesLoading && episodes.size > EPISODES_PAGE_SIZE) {
                    val startIndex = pagerState.currentPage * EPISODES_PAGE_SIZE + 1
                    val endIndex = ((pagerState.currentPage + 1) * EPISODES_PAGE_SIZE).coerceAtMost(episodes.size)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.details_seasons_page_range_fmt, startIndex, endIndex, episodes.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        // Previous Page Button
                        val canGoPrev = pagerState.currentPage > 0
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (canGoPrev) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.03f))
                                .border(1.dp, if (canGoPrev) Color.White.copy(alpha = 0.20f) else Color.Transparent, CircleShape)
                                .tvAndWebHoverEffect(
                                    scaleTarget = if (canGoPrev) 1.08f else 1f,
                                    shape = CircleShape,
                                    tiltEnabled = false,
                                    onClick = {
                                        if (canGoPrev) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.ChevronLeft,
                                contentDescription = "Назад",
                                tint = if (canGoPrev) Color.White else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Next Page Button
                        val canGoNext = pagerState.currentPage < totalPages - 1
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (canGoNext) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.03f))
                                .border(1.dp, if (canGoNext) Color.White.copy(alpha = 0.20f) else Color.Transparent, CircleShape)
                                .tvAndWebHoverEffect(
                                    scaleTarget = if (canGoNext) 1.08f else 1f,
                                    shape = CircleShape,
                                    tiltEnabled = false,
                                    onClick = {
                                        if (canGoNext) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.ChevronRight,
                                contentDescription = "Вперед",
                                tint = if (canGoNext) Color.White else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // === TWO-COLUMN BODY ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top
            ) {
                // --- LEFT COLUMN: SEASONS SIDEBAR (~240dp) ---
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .then(
                            if (rightSectionHeightDp != null) Modifier.height(rightSectionHeightDp!!)
                            else Modifier.heightIn(min = 320.dp, max = 560.dp)
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .shimmerPlaceholder(true, RoundedCornerShape(10.dp))
                            )
                        }
                    } else {
                        // Scrollable seasons list container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            LazyColumn(
                                state = seasonsScrollState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(
                                    items = component.seasons,
                                    key = { it.id.ifBlank { "season_${it.seasonNumber}" } }
                                ) { season ->
                                    val isSelected = season.seasonNumber == component.selectedSeasonNumber
                                    WebSeasonSidebarItem(
                                        season = season,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (!isSelected || activeSeasonContent?.episodes.isNullOrEmpty()) {
                                                season.selectAction?.let(onAction)
                                            }
                                        }
                                    )
                                }
                            }

                            // Top subtle fade indicator
                            if (seasonsScrollState.canScrollBackward) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp)
                                        .align(Alignment.TopCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF141418).copy(alpha = 0.95f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }

                            // Bottom subtle fade indicator
                            if (seasonsScrollState.canScrollForward) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFF141418).copy(alpha = 0.95f)
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        // Mark season watched action button (pinned at bottom of sidebar)
                        if (selectedSeason != null) {
                            WebMarkSeasonButton(
                                season = selectedSeason,
                                onAction = onAction
                            )
                        }
                    }
                }

                // --- VERTICAL DIVIDER ---
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .then(
                            if (rightSectionHeightDp != null) Modifier.height(rightSectionHeightDp!!)
                            else Modifier.height(400.dp)
                        )
                        .background(Color.White.copy(alpha = 0.08f))
                )

                // --- RIGHT COLUMN: NATIVE HORIZONTAL PAGER ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            val heightDp = with(density) { coordinates.size.height.toDp() }
                            if (rightSectionHeightDp != heightDp) {
                                rightSectionHeightDp = heightDp
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Crossfade(
                        targetState = isEpisodesLoading,
                        animationSpec = tween(durationMillis = 250),
                        label = "WebEpisodesCrossfade"
                    ) { loading ->
                        if (loading) {
                            WebEpisodesGridSkeleton()
                        } else if (episodes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.details_seasons_not_available),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 1,
                                pageSpacing = 16.dp,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) { page ->
                                val start = page * EPISODES_PAGE_SIZE
                                val end = (start + EPISODES_PAGE_SIZE).coerceAtMost(episodes.size)
                                val pageEpisodes = if (start < episodes.size) episodes.subList(start, end) else emptyList()

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Row 1 (up to 3 cards)
                                    val row1 = pageEpisodes.take(3)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        row1.forEach { ep ->
                                            val epId = ep.id.ifBlank { "${component.selectedSeasonNumber}_${ep.episodeNumber}" }
                                            Box(modifier = Modifier.weight(1f)) {
                                                WebEpisodeGridCard(
                                                    episode = ep,
                                                    seasonNumber = component.selectedSeasonNumber,
                                                    isSelectedOnTouch = isTouch && activeTouchEpisodeId == epId,
                                                    onTouchSelect = {
                                                        activeTouchEpisodeId = if (activeTouchEpisodeId == epId) null else epId
                                                    },
                                                    onPlay = { ep.playAction?.let(onAction) },
                                                    onToggleWatch = { ep.toggleWatchedAction?.let(onAction) },
                                                    onAction = onAction
                                                )
                                            }
                                        }
                                        repeat(3 - row1.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }

                                    // Row 2 (up to 3 cards)
                                    if (pageEpisodes.size > 3) {
                                        val row2 = pageEpisodes.drop(3).take(3)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            row2.forEach { ep ->
                                                val epId = ep.id.ifBlank { "${component.selectedSeasonNumber}_${ep.episodeNumber}" }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    WebEpisodeGridCard(
                                                        episode = ep,
                                                        seasonNumber = component.selectedSeasonNumber,
                                                        isSelectedOnTouch = isTouch && activeTouchEpisodeId == epId,
                                                        onTouchSelect = {
                                                            activeTouchEpisodeId = if (activeTouchEpisodeId == epId) null else epId
                                                        },
                                                        onPlay = { ep.playAction?.let(onAction) },
                                                        onToggleWatch = { ep.toggleWatchedAction?.let(onAction) },
                                                        onAction = onAction
                                                    )
                                                }
                                            }
                                            repeat(3 - row2.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// EPISODE CARD (WITH COMFORTABLE ACTION PILL & PLAY BUTTON)
// ============================================================================

@Composable
private fun WebEpisodeGridCard(
    episode: EpisodeItem,
    seasonNumber: Int,
    isSelectedOnTouch: Boolean = false,
    onTouchSelect: () -> Unit = {},
    onPlay: () -> Unit,
    onToggleWatch: () -> Unit,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTouch = LocalDeviceTarget.current.isTouch
    var isHovered by remember { mutableStateOf(false) }
    var isPlayHovered by remember { mutableStateOf(false) }
    var isRatingPopupOpen by remember { mutableStateOf(false) }
    var isDetailsDialogOpen by remember { mutableStateOf(false) }

    val isControlsVisible = if (isTouch) isSelectedOnTouch || isRatingPopupOpen else isHovered || isRatingPopupOpen

    val cardBgColor by animateColorAsState(
        targetValue = if (isControlsVisible) Color(0xFF1E1E24) else Color(0xFF141418),
        label = "CardBgColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .tvAndWebHoverEffect(
                scaleTarget = if (isTouch) 1f else 1.02f,
                activeBorderWidth = 1.5.dp,
                activeBorderColor = Color.White.copy(alpha = 0.35f),
                defaultBorderWidth = 1.dp,
                defaultBorderColor = if (isSelectedOnTouch) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                tiltEnabled = false,
                clickEnabled = isTouch,
                onClick = if (isTouch) onTouchSelect else null,
                onStateChange = { if (!isTouch) isHovered = it }
            )
            .background(cardBgColor, RoundedCornerShape(12.dp))
    ) {
        // 1. Thumbnail Container 16:9
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color(0xFF0D0D10)),
            contentAlignment = Alignment.Center
        ) {
            val imageAlpha = if (episode.isWatched && !isControlsVisible) 0.65f else 1.0f

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

            // Hover / Touch Scrim Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Play Button (High Contrast, Hand Cursor on Play)
                    val playBg by animateColorAsState(
                        targetValue = if (isPlayHovered) Color.White else Color.Black.copy(alpha = 0.7f),
                        label = "PlayBg"
                    )
                    val playIconTint by animateColorAsState(
                        targetValue = if (isPlayHovered) Color.Black else Color.White,
                        label = "PlayIconTint"
                    )

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(8.dp, CircleShape)
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.12f,
                                shape = CircleShape,
                                activeBorderWidth = 2.dp,
                                activeBorderColor = Color.White,
                                defaultBorderWidth = 1.5.dp,
                                defaultBorderColor = Color.White.copy(alpha = 0.8f),
                                tiltEnabled = false,
                                onStateChange = { isPlayHovered = it },
                                onClick = onPlay
                            )
                            .background(playBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = "Смотреть серию",
                            tint = playIconTint,
                            modifier = Modifier.size(24.dp).offset(x = 1.5.dp)
                        )
                    }
                }
            }

            // Runtime Duration Badge (bottom-left)
            val runtime = episode.runtime
            if (runtime != null && runtime > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.player_duration_mins_single_fmt, runtime),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Top-Right Action Pill (Large 34-36dp Buttons)
            androidx.compose.animation.AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                var isWatchHovered by remember { mutableStateOf(false) }
                var isRatingHovered by remember { mutableStateOf(false) }

                val isWatched = episode.isWatched
                val watchColor = if (isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.9f)
                val currentWatchBg by animateColorAsState(
                    targetValue = if (isWatched) {
                        if (isWatchHovered) Color(0xFF4CAF50).copy(alpha = 0.35f) else Color(0xFF4CAF50).copy(alpha = 0.2f)
                    } else {
                        if (isWatchHovered) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)
                    },
                    label = "WatchBg"
                )
                val currentWatchBorder by animateColorAsState(
                    targetValue = if (isWatched) {
                        if (isWatchHovered) Color(0xFF4CAF50).copy(alpha = 0.7f) else Color(0xFF4CAF50).copy(alpha = 0.4f)
                    } else {
                        if (isWatchHovered) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                    },
                    label = "WatchBorder"
                )

                val userRating = episode.userRating
                val hasRating = userRating != null && userRating > 0
                val ratingColor = if (hasRating) Color(0xFFFFC107) else Color.White.copy(alpha = 0.9f)
                val currentRatingBg by animateColorAsState(
                    targetValue = if (hasRating) {
                        if (isRatingHovered) Color(0xFFFFC107).copy(alpha = 0.35f) else Color(0xFFFFC107).copy(alpha = 0.2f)
                    } else {
                        if (isRatingHovered) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)
                    },
                    label = "RatingBg"
                )
                val currentRatingBorder by animateColorAsState(
                    targetValue = if (hasRating) {
                        if (isRatingHovered) Color(0xFFFFC107).copy(alpha = 0.7f) else Color(0xFFFFC107).copy(alpha = 0.4f)
                    } else {
                        if (isRatingHovered) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                    },
                    label = "RatingBorder"
                )

                Row(
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(20.dp))
                        .background(Color(0xFF141418).copy(alpha = 0.96f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Watch Status Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.08f,
                                shape = CircleShape,
                                activeBorderWidth = 1.5.dp,
                                activeBorderColor = currentWatchBorder,
                                defaultBorderWidth = 1.dp,
                                defaultBorderColor = currentWatchBorder,
                                tiltEnabled = false,
                                onStateChange = { isWatchHovered = it },
                                onClick = onToggleWatch
                            )
                            .background(currentWatchBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWatched) Lucide.CheckCheck else Lucide.Eye,
                            contentDescription = if (isWatched) "Просмотрено" else "Отметить",
                            tint = watchColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(Color.White.copy(alpha = 0.18f))
                    )

                    // Rating Button (34dp height with label)
                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.06f,
                                    shape = RoundedCornerShape(17.dp),
                                    activeBorderWidth = 1.5.dp,
                                    activeBorderColor = currentRatingBorder,
                                    defaultBorderWidth = 1.dp,
                                    defaultBorderColor = currentRatingBorder,
                                    tiltEnabled = false,
                                    onStateChange = { isRatingHovered = it },
                                    onClick = { isRatingPopupOpen = true }
                                )
                                .background(currentRatingBg, RoundedCornerShape(17.dp))
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Star,
                                contentDescription = "Оценить",
                                tint = ratingColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (hasRating) "$userRating" else "Оценить",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ratingColor
                            )
                        }

                        if (isRatingPopupOpen) {
                            EpisodeRatingPopup(
                                currentRating = userRating,
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(0, -60),
                                onDismiss = { isRatingPopupOpen = false },
                                onRate = { rating ->
                                    isRatingPopupOpen = false
                                    val playAct = episode.playAction as? org.ensodai.avalonmediacard.contract.slot.ActionPreparePlayer
                                    if (playAct != null) {
                                        onAction(RateEpisodeCommand(playAct.key, seasonNumber, episode.episodeNumber, rating))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Green Progress Bar at Bottom of Thumbnail (if watched)
            if (episode.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF4CAF50))
                )
            }
        }

        // 2. Card Content (Title, Meta, Synopsis)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Title Row with Watched Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (episode.isWatched) {
                    Icon(
                        imageVector = Lucide.CheckCheck,
                        contentDescription = "Просмотрено",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Text(
                    text = "${episode.episodeNumber}. ${episode.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata Row: Air Date & Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val airDate = episode.airDate
                if (!airDate.isNullOrEmpty()) {
                    Text(
                        text = airDate,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                val rating = episode.voteAverage
                if (rating != null && rating > 0.0) {
                    val formattedRating = ((rating * 10).toInt() / 10.0).toString()
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formattedRating,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFC107)
                        )
                    }
                }
            }

            // Overview Synopsis (2 lines) with Clickable "Подробнее"
            val overview = episode.overview?.trim()
            if (!overview.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.0f,
                            shape = RoundedCornerShape(4.dp),
                            tiltEnabled = false,
                            onClick = { isDetailsDialogOpen = true }
                        ),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = overview,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.White.copy(alpha = if (isHovered) 0.8f else 0.5f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Modal Dialog with Full Synopsis
    if (isDetailsDialogOpen) {
        EpisodeDetailsDialog(
            episode = episode,
            seasonNumber = seasonNumber,
            onDismiss = { isDetailsDialogOpen = false },
            onPlay = {
                isDetailsDialogOpen = false
                onPlay()
            },
            onToggleWatch = onToggleWatch
        )
    }
}

// ============================================================================
// EPISODE DETAILS MODAL (FULL SYNOPSIS)
// ============================================================================

@Composable
private fun EpisodeDetailsDialog(
    episode: EpisodeItem,
    seasonNumber: Int,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleWatch: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF16161A))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Сезон $seasonNumber • Серия ${episode.episodeNumber}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.15f,
                                    shape = CircleShape,
                                    activeBorderWidth = 1.5.dp,
                                    activeBorderColor = Color.White.copy(alpha = 0.4f),
                                    defaultBorderWidth = 1.dp,
                                    defaultBorderColor = Color.White.copy(alpha = 0.1f),
                                    tiltEnabled = false,
                                    onClick = onDismiss
                                )
                                .background(Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Закрыть",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Large Still Image 16:9
                    if (!episode.stillUrl.isNullOrEmpty() && episode.stillUrl != "placeholder") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0F12))
                        ) {
                            ShimmerImage(
                                model = episode.stillUrl,
                                contentDescription = episode.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Title
                    Text(
                        text = episode.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Meta: Date, Runtime, Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val airDate = episode.airDate
                        if (!airDate.isNullOrEmpty()) {
                            Text(
                                text = airDate,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        val runtime = episode.runtime
                        if (runtime != null && runtime > 0) {
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "$runtime мин.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        val rating = episode.voteAverage
                        if (rating != null && rating > 0.0) {
                            val formattedRating = ((rating * 10).toInt() / 10.0).toString()
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 13.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formattedRating,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFC107)
                                )
                            }
                        }
                    }

                    // Full Synopsis
                    val overview = episode.overview?.trim()
                    if (!overview.isNullOrEmpty()) {
                        Text(
                            text = overview,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Action Buttons (Play & Watch Toggle)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play Button (High Contrast White Button with Black Text/Icon)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.03f,
                                    shape = RoundedCornerShape(10.dp),
                                    activeBorderWidth = 1.5.dp,
                                    activeBorderColor = Color.White,
                                    tiltEnabled = false,
                                    onClick = onPlay
                                )
                                .background(Color.White, RoundedCornerShape(10.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Play,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Смотреть серию",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        // Toggle Watch Button
                        val isWatched = episode.isWatched
                        val watchBg = if (isWatched) Color(0xFF4CAF50).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f)
                        val watchBorder = if (isWatched) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                        val watchColor = if (isWatched) Color(0xFF4CAF50) else Color.White

                        Box(
                            modifier = Modifier
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.03f,
                                    shape = RoundedCornerShape(10.dp),
                                    activeBorderWidth = 1.5.dp,
                                    activeBorderColor = if (isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f),
                                    defaultBorderWidth = 1.dp,
                                    defaultBorderColor = watchBorder,
                                    tiltEnabled = false,
                                    onClick = onToggleWatch
                                )
                                .background(watchBg, RoundedCornerShape(10.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWatched) Lucide.CheckCheck else Lucide.Eye,
                                    contentDescription = null,
                                    tint = watchColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isWatched) "Просмотрено" else "Отметить",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = watchColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SIDEBAR & SUBCOMPONENTS
// ============================================================================

@Composable
private fun WebSeasonSidebarItem(
    season: SeasonItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White.copy(alpha = 0.16f)
            isHovered -> Color.White.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        label = "SidebarBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White.copy(alpha = 0.35f)
            isHovered -> Color.White.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        label = "SidebarBorder"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White
            isHovered -> Color.White.copy(alpha = 0.95f)
            else -> Color.White.copy(alpha = 0.7f)
        },
        label = "SidebarText"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(
                scaleTarget = 1.01f,
                shape = RoundedCornerShape(10.dp),
                activeBorderWidth = 1.dp,
                activeBorderColor = borderColor,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = borderColor,
                tiltEnabled = false,
                onStateChange = { isHovered = it },
                onClick = onClick
            )
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (season.isFullyWatched) {
                Icon(
                    imageVector = Lucide.CheckCheck,
                    contentDescription = stringResource(Res.string.details_seasons_completed),
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(15.dp)
                )
            } else if (season.isWatching) {
                Icon(
                    imageVector = Lucide.Eye,
                    contentDescription = stringResource(Res.string.details_seasons_watching),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = getSeasonDisplayName(season),
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (season.episodeCount > 0) {
            Text(
                text = "${season.episodeCount}",
                color = textColor.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun WebMarkSeasonButton(
    season: SeasonItem,
    onAction: (Action) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val isWatched = season.isFullyWatched
    val buttonBgColor by animateColorAsState(
        targetValue = if (isWatched) {
            if (isHovered) Color(0xFF4CAF50).copy(alpha = 0.28f) else Color(0xFF4CAF50).copy(alpha = 0.16f)
        } else {
            if (isHovered) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f)
        },
        label = "MarkSeasonBg"
    )
    val buttonBorderColor by animateColorAsState(
        targetValue = if (isWatched) {
            if (isHovered) Color(0xFF4CAF50).copy(alpha = 0.6f) else Color(0xFF4CAF50).copy(alpha = 0.35f)
        } else {
            if (isHovered) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f)
        },
        label = "MarkSeasonBorder"
    )
    val buttonContentColor = if (isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.85f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f,
                shape = RoundedCornerShape(10.dp),
                activeBorderWidth = 1.dp,
                activeBorderColor = buttonBorderColor,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = buttonBorderColor,
                tiltEnabled = false,
                onStateChange = { isHovered = it },
                onClick = {
                    season.markWatchedAction?.let(onAction)
                }
            )
            .background(buttonBgColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isWatched) Lucide.CheckCheck else Lucide.Check,
            contentDescription = null,
            tint = buttonContentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isWatched) stringResource(Res.string.details_seasons_unmark_btn) else stringResource(Res.string.details_seasons_mark_btn),
            color = buttonContentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WebEpisodesGridSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(3) {
                    Box(modifier = Modifier.weight(1f)) {
                        WebEpisodeGridSkeletonCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun WebEpisodeGridSkeletonCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .background(Color(0xFF141418), RoundedCornerShape(12.dp))
    ) {
        // 1. Thumbnail Container 16:9
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .shimmerPlaceholder(true, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        )

        // 2. Card Content (Title, Meta, Synopsis)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Title Shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(18.dp)
                    .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata Shimmer (Air Date & Rating)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.40f)
                    .height(14.dp)
                    .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Synopsis Shimmer (2 lines)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(12.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.60f)
                        .height(12.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

private fun getPluralSeasonRes(count: Int): StringResource {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> Res.string.details_seasons_count_many_fmt
        mod10 == 1 -> Res.string.details_seasons_count_single_fmt
        mod10 in 2..4 -> Res.string.details_seasons_count_few_fmt
        else -> Res.string.details_seasons_count_many_fmt
    }
}

private fun getPluralEpisodeRes(count: Int): StringResource {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> Res.string.details_episodes_count_many_fmt
        mod10 == 1 -> Res.string.details_episodes_count_single_fmt
        mod10 in 2..4 -> Res.string.details_episodes_count_few_fmt
        else -> Res.string.details_episodes_count_many_fmt
    }
}

@Composable
private fun getSeasonDisplayName(season: SeasonItem): String {
    return if (season.seasonNumber == 0) {
        stringResource(Res.string.details_seasons_specials)
    } else {
        stringResource(Res.string.player_season_fmt, season.seasonNumber)
    }
}
