package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonSize
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonVariant
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonIconButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonInteractiveRole
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonSurfaceColors
import org.ensodai.avalonmediacard.designsystem.interaction.avalonInteractive
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.EpisodeDetailsDialog
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
    val expectedEpisodesCount = selectedSeason?.episodeCount ?: episodes.size
    val totalPages = remember(episodes, expectedEpisodesCount) {
        val count = if (episodes.isNotEmpty()) episodes.size else expectedEpisodesCount
        if (count > 0) {
            (count + EPISODES_PAGE_SIZE - 1) / EPISODES_PAGE_SIZE
        } else 1
    }

    val pagerState = rememberPagerState(initialPage = 0) { totalPages }
    val coroutineScope = rememberCoroutineScope()

    // Reset pager page to 0 when season changes
    LaunchedEffect(component.selectedSeasonNumber) {
        if (pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    val deviceTarget = LocalDeviceTarget.current
    val isTouch = deviceTarget.isTouch

    var activeTouchEpisodeId by remember { mutableStateOf<String?>(null) }

    // Reset touch selection when season or page changes
    LaunchedEffect(component.selectedSeasonNumber, pagerState.currentPage) {
        activeTouchEpisodeId = null
    }

    val seasonsScrollState = rememberLazyListState()

    // Auto-scroll sidebar only if selected season is not already in the visible range
    val selectedSeasonIndex = remember(component.selectedSeasonNumber, component.seasons) {
        component.seasons.indexOfFirst { it.seasonNumber == component.selectedSeasonNumber }
    }
    LaunchedEffect(selectedSeasonIndex) {
        if (selectedSeasonIndex >= 0) {
            val visible = seasonsScrollState.layoutInfo.visibleItemsInfo.map { it.index }
            if (visible.isNotEmpty() && selectedSeasonIndex !in visible) {
                seasonsScrollState.animateScrollToItem(selectedSeasonIndex)
            }
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

    // Free-flowing layout without heavy outer glass box (Obsidian Cinema 7.5)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
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
                    AvalonText(
                        text = stringResource(Res.string.details_seasons_header),
                        style = AvalonTheme.typography.sectionTitle,
                        color = AvalonTheme.colors.textPrimary
                    )

                    if (!isLoading && component.seasons.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(AvalonTheme.shapes.micro)
                                .background(AvalonTheme.colors.bgElevated)
                                .border(1.dp, AvalonTheme.colors.borderSubtle, AvalonTheme.shapes.micro)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            val seasonRes = getPluralSeasonRes(seasonsCount)
                            val episodeRes = getPluralEpisodeRes(totalEpisodes)
                            val seasonCountText = stringResource(seasonRes, seasonsCount)
                            val episodesText = if (totalEpisodes > 0) " • " + stringResource(episodeRes, totalEpisodes) else ""
                            val specialsText = if (hasSpecials && regularSeasons.isNotEmpty()) " • " + stringResource(Res.string.details_seasons_specials) else ""
                            AvalonText(
                                text = "$seasonCountText$episodesText$specialsText",
                                style = AvalonTheme.typography.metadata,
                                color = AvalonTheme.colors.textSecondary
                            )
                        }
                    }
                }

                // Page Navigation Arrows (Top Right)
                if (totalPages > 1 || episodes.size > EPISODES_PAGE_SIZE) {
                    val count = if (episodes.isNotEmpty()) episodes.size else expectedEpisodesCount
                    val startIndex = pagerState.currentPage * EPISODES_PAGE_SIZE + 1
                    val endIndex = ((pagerState.currentPage + 1) * EPISODES_PAGE_SIZE).coerceAtMost(count.coerceAtLeast(1))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (count > 0) {
                            AvalonText(
                                text = stringResource(Res.string.details_seasons_page_range_fmt, startIndex, endIndex, count),
                                style = AvalonTheme.typography.labelSmall,
                                color = AvalonTheme.colors.textSecondary
                            )
                        }

                        val canGoPrev = !isEpisodesLoading && pagerState.currentPage > 0
                        AvalonIconButton(
                            icon = {
                                AvalonIcon(
                                    imageVector = Lucide.ChevronLeft,
                                    contentDescription = "Назад",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                if (canGoPrev) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            variant = AvalonButtonVariant.Secondary,
                            size = AvalonButtonSize.Compact,
                            enabled = canGoPrev
                        )

                        val canGoNext = !isEpisodesLoading && pagerState.currentPage < totalPages - 1
                        AvalonIconButton(
                            icon = {
                                AvalonIcon(
                                    imageVector = Lucide.ChevronRight,
                                    contentDescription = "Вперед",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                if (canGoNext) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            variant = AvalonButtonVariant.Secondary,
                            size = AvalonButtonSize.Compact,
                            enabled = canGoNext
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // === TWO-COLUMN BODY ===
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val availableWidth = maxWidth
                // 3 cards in right column:
                // sidebarWidth = 240.dp, dividerWidth = 1.dp, gaps = 24.dp * 2 = 48.dp
                // rightColumnWidth = availableWidth - 289.dp
                // inner grid padding = 8.dp * 2 = 16.dp -> gridWidth = availableWidth - 305.dp
                // 3 cards with 2 * 16.dp gaps = 32.dp -> (availableWidth - 337.dp) / 3
                val cardWidth = ((availableWidth - 337.dp) / 3).coerceAtLeast(160.dp)
                val thumbnailHeight = cardWidth * (9f / 16f)
                val singleCardHeight = thumbnailHeight + 112.dp + 8.dp
                val exactGridHeight = (singleCardHeight * 2 + 16.dp + 12.dp).coerceAtLeast(460.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(exactGridHeight),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // --- LEFT COLUMN: SEASONS SIDEBAR (~240dp) ---
                    Column(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                    if (isLoading && component.seasons.isEmpty()) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .shimmerPlaceholder(true, AvalonTheme.shapes.control)
                            )
                        }
                    } else {
                        // Scrollable seasons list container (fills available height above button)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            LazyColumn(
                                state = seasonsScrollState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp),
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
                                                    AvalonTheme.colors.bgCanvas,
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
                                                    AvalonTheme.colors.bgCanvas
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        // Mark season watched action button (pinned at the bottom of the sidebar)
                        if (selectedSeason != null) {
                            Spacer(modifier = Modifier.height(10.dp))
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
                        .fillMaxHeight()
                        .background(AvalonTheme.colors.borderSubtle)
                )

                // --- RIGHT COLUMN: EPISODES CONTENT (FIXED HEIGHT, TOP-ALIGNED) ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Crossfade(
                        targetState = isEpisodesLoading,
                        animationSpec = tween(durationMillis = 200),
                        label = "WebEpisodesCrossfade",
                        modifier = Modifier.fillMaxSize()
                    ) { loading ->
                        if (loading) {
                            WebEpisodesGridSkeleton(modifier = Modifier.fillMaxWidth())
                        } else if (episodes.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                AvalonText(
                                    text = stringResource(Res.string.details_seasons_not_available),
                                    color = AvalonTheme.colors.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 1,
                                pageSpacing = 16.dp,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxSize(),
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
// EPISODE CARD (CLICK TO OPEN DETAILS POPUP, DIRECT WATCH & RATE ACTIONS)
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
    var isRatingPopupOpen by remember { mutableStateOf(false) }
    var isDetailsDialogOpen by remember { mutableStateOf(false) }

    val isControlsVisible = if (isTouch) isSelectedOnTouch || isRatingPopupOpen else isHovered || isRatingPopupOpen
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    val cardBgColor by animateColorAsState(
        targetValue = if (isControlsVisible) colors.bgElevated else colors.bgSurface,
        label = "CardBgColor"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (isControlsVisible) colors.borderStrong else colors.borderSubtle,
        label = "CardBorderColor"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isCardHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(isCardHovered) {
        if (!isTouch) isHovered = isCardHovered
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = if (isPressed) tween(50) else spring(),
        label = "CardScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(1.dp, cardBorderColor, shapes.media)
            .clip(shapes.media)
            .background(cardBgColor, shapes.media)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource = interactionSource, enabled = !isTouch)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (isTouch) onTouchSelect() else isDetailsDialogOpen = true
                }
            )
    ) {
        // 1. Thumbnail Container 16:9
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(colors.bgCanvas),
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
                AvalonIcon(
                    imageVector = Lucide.Film,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Runtime Duration Badge (bottom-left)
            val runtime = episode.runtime
            if (runtime != null && runtime > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(shapes.micro)
                        .background(colors.bgCanvas.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    AvalonText(
                        text = stringResource(Res.string.player_duration_mins_single_fmt, runtime),
                        style = AvalonTheme.typography.monoTech,
                        color = colors.textPrimary
                    )
                }
            }

            // Top-Right Action Pill (Watch Status & Rating Buttons with Hand Cursor)
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
                val watchColor = if (isWatched) colors.statusLive else colors.textPrimary
                val currentWatchBg by animateColorAsState(
                    targetValue = if (isWatched) {
                        colors.statusLive.copy(alpha = if (isWatchHovered) 0.35f else 0.18f)
                    } else {
                        colors.bgElevated.copy(alpha = if (isWatchHovered) 0.8f else 0.4f)
                    },
                    label = "WatchBg"
                )
                val currentWatchBorder by animateColorAsState(
                    targetValue = if (isWatched) {
                        colors.statusLive.copy(alpha = if (isWatchHovered) 0.7f else 0.4f)
                    } else {
                        colors.borderStrong.copy(alpha = if (isWatchHovered) 0.6f else 0.3f)
                    },
                    label = "WatchBorder"
                )

                val userRating = episode.userRating
                val hasRating = userRating != null && userRating > 0
                val ratingColor = if (hasRating) colors.accentGold else colors.textPrimary
                val currentRatingBg by animateColorAsState(
                    targetValue = if (hasRating) {
                        colors.accentGold.copy(alpha = if (isRatingHovered) 0.35f else 0.18f)
                    } else {
                        colors.bgElevated.copy(alpha = if (isRatingHovered) 0.8f else 0.4f)
                    },
                    label = "RatingBg"
                )
                val currentRatingBorder by animateColorAsState(
                    targetValue = if (hasRating) {
                        colors.accentGold.copy(alpha = if (isRatingHovered) 0.7f else 0.4f)
                    } else {
                        colors.borderStrong.copy(alpha = if (isRatingHovered) 0.6f else 0.3f)
                    },
                    label = "RatingBorder"
                )

                Row(
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(20.dp))
                        .background(colors.bgOverlay.copy(alpha = 0.96f), RoundedCornerShape(20.dp))
                        .border(1.dp, colors.borderOverlay, RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Watch Status Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
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
                        AvalonIcon(
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
                            .background(colors.borderSubtle)
                    )

                    // Rating Button (34dp height)
                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .then(
                                    if (hasRating) Modifier.padding(horizontal = 8.dp)
                                    else Modifier.size(34.dp)
                                )
                                .pointerHoverIcon(PointerIcon.Hand)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.06f,
                                    shape = if (hasRating) RoundedCornerShape(17.dp) else CircleShape,
                                    activeBorderWidth = 1.5.dp,
                                    activeBorderColor = currentRatingBorder,
                                    defaultBorderWidth = 1.dp,
                                    defaultBorderColor = currentRatingBorder,
                                    tiltEnabled = false,
                                    onStateChange = { isRatingHovered = it },
                                    onClick = { isRatingPopupOpen = true }
                                )
                                .background(currentRatingBg, if (hasRating) RoundedCornerShape(17.dp) else CircleShape),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AvalonIcon(
                                imageVector = Lucide.Star,
                                contentDescription = if (hasRating) "$userRating" else null,
                                tint = ratingColor,
                                modifier = Modifier.size(16.dp)
                            )
                            if (hasRating) {
                                Spacer(modifier = Modifier.width(4.dp))
                                AvalonText(
                                    text = "$userRating",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ratingColor
                                )
                            }
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

            // Status Progress Bar at Bottom of Thumbnail (if watched)
            if (episode.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.statusLive)
                )
            }
        }

        // 2. Card Content (Title, Meta, Synopsis) with fixed 112dp height
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Title Row with Watched Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (episode.isWatched) {
                        AvalonIcon(
                            imageVector = Lucide.CheckCheck,
                            contentDescription = "Просмотрено",
                            tint = colors.statusLive,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    AvalonText(
                        text = "${episode.episodeNumber}. ${episode.name}",
                        style = AvalonTheme.typography.cardTitle,
                        color = colors.textPrimary,
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
                        AvalonText(
                            text = airDate,
                            style = AvalonTheme.typography.metadata,
                            color = colors.textSecondary
                        )
                    }

                    val rating = episode.voteAverage
                    if (rating != null && rating > 0.0) {
                        val formattedRating = ((rating * 10).toInt() / 10.0).toString()
                        if (!airDate.isNullOrEmpty()) {
                            AvalonText(
                                text = "•",
                                color = colors.textSecondary.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            AvalonIcon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = colors.accentGold,
                                modifier = Modifier.size(12.dp)
                            )
                            AvalonText(
                                text = formattedRating,
                                style = AvalonTheme.typography.metadata,
                                color = colors.accentGold
                            )
                        }
                    }
                }
            }

            // Overview Synopsis (2 lines max, graceful fallback if empty)
            val overview = episode.overview?.trim()
            AvalonText(
                text = overview.orEmpty(),
                style = AvalonTheme.typography.metadata,
                color = colors.textSecondary.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
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
// SIDEBAR & SUBCOMPONENTS
// ============================================================================

@Composable
private fun WebSeasonSidebarItem(
    season: SeasonItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val shape = shapes.control // 10.dp

    val surfaceColors = if (isSelected) {
        AvalonSurfaceColors(
            default = colors.bgElevated,
            hover = colors.bgElevated,
            pressed = colors.bgElevated
        )
    } else {
        AvalonSurfaceColors(
            default = Color.Transparent,
            hover = colors.bgSurface,
            pressed = colors.bgElevated
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .avalonInteractive(
                role = AvalonInteractiveRole.NavItem,
                shape = shape,
                surfaceColors = surfaceColors,
                onClick = onClick
            )
    ) {
        // Subtle accent edge indicator for selected season (pinned flush to the left edge)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(colors.accentPrimary)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (season.isFullyWatched) {
                    AvalonIcon(
                        imageVector = Lucide.CheckCheck,
                        contentDescription = stringResource(Res.string.details_seasons_completed),
                        tint = colors.statusLive,
                        modifier = Modifier.size(15.dp)
                    )
                } else if (season.isWatching) {
                    AvalonIcon(
                        imageVector = Lucide.Eye,
                        contentDescription = stringResource(Res.string.details_seasons_watching),
                        tint = colors.accentPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                AvalonText(
                    text = getSeasonDisplayName(season),
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (season.episodeCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(shapes.micro)
                        .background(if (isSelected) colors.bgSurface else Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    AvalonText(
                        text = "${season.episodeCount}",
                        color = if (isSelected) colors.textPrimary else colors.textSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun WebMarkSeasonButton(
    season: SeasonItem,
    onAction: (Action) -> Unit
) {
    val isWatched = season.isFullyWatched
    AvalonButton(
        text = if (isWatched) stringResource(Res.string.details_seasons_unmark_btn) else stringResource(Res.string.details_seasons_mark_btn),
        leadingIcon = {
            AvalonIcon(
                imageVector = if (isWatched) Lucide.CheckCheck else Lucide.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        onClick = {
            season.markWatchedAction?.let(onAction)
        },
        variant = AvalonButtonVariant.Secondary,
        size = AvalonButtonSize.Compact,
        modifier = Modifier.fillMaxWidth()
    )
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
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, colors.borderSubtle, shapes.media)
            .background(colors.bgSurface, shapes.media)
    ) {
        // 1. Thumbnail Container 16:9
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .shimmerPlaceholder(true, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        )

        // 2. Card Content (Matching exact 112dp height with WebEpisodeGridCard)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Title Shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(16.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata Shimmer (Air Date & Rating)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.40f)
                        .height(12.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
            }

            // Synopsis Shimmer (2 lines)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(10.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.60f)
                        .height(10.dp)
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
