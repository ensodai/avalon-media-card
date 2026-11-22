package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.contract.slot.SeasonItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvTvSeasonsSection(
    state: SlotUiState<SlotData.TvSeasons>?,
    onAction: (Action) -> Unit,
    onHeaderFocus: (() -> Unit)? = null,
    onBannerFocus: (() -> Unit)? = null,
    onEpisodesFocus: (() -> Unit)? = null,
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

    // Stable focused episode ID per season
    var focusedEpisodeId by remember(component.selectedSeasonNumber) {
        mutableStateOf<String?>(null)
    }

    // Modal drawer states and focus requesters for season selection & episode description
    var isSeasonDrawerOpen by remember { mutableStateOf(false) }
    var isEpisodeDescriptionDrawerOpen by remember { mutableStateOf(false) }
    var drawerEpisodeId by remember { mutableStateOf<String?>(null) }
    val seasonButtonFocusRequester = remember { FocusRequester() }
    val bannerFocusRequester = remember { FocusRequester() }

    // Modal dialog for episode options (play, toggle watch)
    var episodeMenuTarget by remember { mutableStateOf<EpisodeItem?>(null) }

    val episodesRowState = rememberLazyListState()

    // Auto-scroll carousel to first unwatched episode on initial season load
    LaunchedEffect(component.selectedSeasonNumber, isEpisodesLoading) {
        if (!isEpisodesLoading && episodes.isNotEmpty() && focusedEpisodeId == null) {
            val firstUnwatchedIdx = episodes.indexOfFirst { !it.isWatched }
            val targetIdx = if (firstUnwatchedIdx >= 0) firstUnwatchedIdx else 0
            if (targetIdx < episodes.size) {
                focusedEpisodeId = episodes[targetIdx].id
                episodesRowState.animateScrollToItem(targetIdx)
            }
        }
    }

    // Resolved focused episode
    val focusedEpisode = remember(episodes, focusedEpisodeId) {
        episodes.find { it.id == focusedEpisodeId } ?: episodes.firstOrNull()
    }

    val drawerEpisode = remember(episodes, drawerEpisodeId, focusedEpisode) {
        drawerEpisodeId?.let { id -> episodes.find { it.id == id } } ?: focusedEpisode
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusGroup()
    ) {
        // === 1. TOP HEADER & SEASON SELECTOR BAR ===
        TvSeasonsHeader(
            isLoading = isLoading,
            component = component,
            seasonsCount = seasonsCount,
            totalEpisodes = totalEpisodes,
            hasSpecials = hasSpecials,
            regularSeasons = regularSeasons,
            selectedSeason = selectedSeason,
            seasonButtonFocusRequester = seasonButtonFocusRequester,
            onHeaderFocus = onHeaderFocus,
            onOpenSeasonDrawer = { isSeasonDrawerOpen = true },
            onAction = onAction
        )

        Spacer(modifier = Modifier.height(16.dp))

        // === 2. DYNAMIC FOCUSED EPISODE INFO BANNER ===
        TvEpisodeInfoBanner(
            focusedEpisode = focusedEpisode,
            isEpisodesLoading = isEpisodesLoading,
            bannerFocusRequester = bannerFocusRequester,
            onBannerFocus = onBannerFocus,
            onOpenDescriptionDrawer = {
                drawerEpisodeId = focusedEpisode?.id
                focusedEpisode?.id?.let { focusedEpisodeId = it }
                isEpisodeDescriptionDrawerOpen = true
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // === 3. HORIZONTAL EPISODES CAROUSEL (LAZY ROW) ===
        if (isEpisodesLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(6) {
                    TvEpisodeCardSkeleton()
                }
            }
        } else if (episodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                AvalonText(
                    text = stringResource(Res.string.details_seasons_not_available),
                    color = AvalonTheme.colors.textSecondary,
                    style = AvalonTheme.typography.bodyMedium
                )
            }
        } else {
            LazyRow(
                state = episodesRowState,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.hasFocus) onEpisodesFocus?.invoke() }
            ) {
                items(
                    items = episodes,
                    key = { it.id.ifBlank { "ep_${it.episodeNumber}" } }
                ) { episode ->
                    TvEpisodeCard(
                        episode = episode,
                        isCurrentFocused = focusedEpisode?.id == episode.id,
                        onFocus = { focusedEpisodeId = episode.id },
                        onPlay = { episode.playAction?.let(onAction) },
                        onOptions = { episodeMenuTarget = episode }
                    )
                }
            }
        }
    }

    // === 4. SEASON SELECTOR (TV RIGHT DRAWER) ===
    TvSeasonPickerDrawer(
        isOpen = isSeasonDrawerOpen,
        seasons = component.seasons,
        selectedSeasonNumber = component.selectedSeasonNumber,
        callerFocusRequester = seasonButtonFocusRequester,
        onDismiss = { isSeasonDrawerOpen = false },
        onAction = onAction
    )

    // === 5. EPISODE ACTIONS MODAL (TV OPTIONS) ===
    TvEpisodeActionsDialog(
        targetEpisode = episodeMenuTarget,
        onDismiss = { episodeMenuTarget = null },
        onAction = onAction
    )

    // === 6. FULL EPISODE OVERVIEW (TV RIGHT DRAWER) ===
    TvEpisodeDetailsDrawer(
        isOpen = isEpisodeDescriptionDrawerOpen,
        episode = drawerEpisode,
        selectedSeasonNumber = component.selectedSeasonNumber,
        callerFocusRequester = bannerFocusRequester,
        onDismiss = {
            isEpisodeDescriptionDrawerOpen = false
            drawerEpisodeId = null
        },
        onAction = onAction
    )
}

@Composable
private fun TvSeasonsHeader(
    isLoading: Boolean,
    component: SlotData.TvSeasons,
    seasonsCount: Int,
    totalEpisodes: Int,
    hasSpecials: Boolean,
    regularSeasons: List<SeasonItem>,
    selectedSeason: SeasonItem?,
    seasonButtonFocusRequester: FocusRequester,
    onHeaderFocus: (() -> Unit)?,
    onOpenSeasonDrawer: () -> Unit,
    onAction: (Action) -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val typography = AvalonTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.hasFocus) onHeaderFocus?.invoke() }
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Title + Count Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AvalonText(
                text = stringResource(Res.string.details_seasons_header),
                style = typography.sectionTitle,
                color = colors.textPrimary
            )

            if (!isLoading && component.seasons.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(shapes.micro)
                        .background(colors.bgElevated)
                        .border(1.dp, colors.borderSubtle, shapes.micro)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val seasonRes = getPluralSeasonRes(seasonsCount)
                    val episodeRes = getPluralEpisodeRes(totalEpisodes)
                    val seasonCountText = stringResource(seasonRes, seasonsCount)
                    val episodesText = if (totalEpisodes > 0) " • " + stringResource(episodeRes, totalEpisodes) else ""
                    val specialsText = if (hasSpecials && regularSeasons.isNotEmpty()) " • " + stringResource(Res.string.details_seasons_specials) else ""
                    AvalonText(
                        text = "$seasonCountText$episodesText$specialsText",
                        style = typography.metadata,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Right: Season Picker Button + Mark Season Button
        if (!isLoading && selectedSeason != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Season Dropdown Button [ Season X ﹀ ]
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .focusRequester(seasonButtonFocusRequester)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.04f,
                            activeBorderWidth = 2.dp,
                            activeBorderColor = colors.accentPrimary,
                            defaultBorderWidth = 1.dp,
                            defaultBorderColor = colors.borderSubtle,
                            shape = shapes.control,
                            tiltEnabled = false,
                            onClick = onOpenSeasonDrawer
                        )
                        .background(colors.bgElevated, shapes.control)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvalonText(
                        text = getSeasonDisplayName(selectedSeason),
                        style = typography.uiLabel,
                        color = colors.textPrimary
                    )
                    AvalonIcon(
                        imageVector = Lucide.ChevronDown,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Mark Season Button
                val isFullyWatched = selectedSeason.isFullyWatched
                val markSeasonColor = if (isFullyWatched) colors.statusLive else colors.textPrimary
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.04f,
                            activeBorderWidth = 2.dp,
                            activeBorderColor = if (isFullyWatched) colors.statusLive else colors.borderStrong,
                            defaultBorderWidth = 1.dp,
                            defaultBorderColor = if (isFullyWatched) colors.statusLive.copy(alpha = 0.4f) else colors.borderSubtle,
                            shape = shapes.control,
                            tiltEnabled = false,
                            onClick = { selectedSeason.markWatchedAction?.let(onAction) }
                        )
                        .background(
                            if (isFullyWatched) colors.statusLive.copy(alpha = 0.15f) else colors.bgElevated,
                            shapes.control
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AvalonIcon(
                        imageVector = if (isFullyWatched) Lucide.CheckCheck else Lucide.Eye,
                        contentDescription = null,
                        tint = markSeasonColor,
                        modifier = Modifier.size(16.dp)
                    )
                    AvalonText(
                        text = if (isFullyWatched) stringResource(Res.string.details_seasons_unmark_btn) else stringResource(Res.string.details_seasons_mark_btn),
                        style = typography.labelSmall,
                        color = markSeasonColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TvEpisodeInfoBanner(
    focusedEpisode: EpisodeItem?,
    isEpisodesLoading: Boolean,
    bannerFocusRequester: FocusRequester,
    onBannerFocus: (() -> Unit)?,
    onOpenDescriptionDrawer: () -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val typography = AvalonTheme.typography

    val canOpenDescription = !isEpisodesLoading && focusedEpisode != null && focusedEpisode.overview?.isNotBlank() == true
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(144.dp)
            .focusRequester(bannerFocusRequester)
            .onFocusChanged { if (it.hasFocus) onBannerFocus?.invoke() }
            .tvAndWebHoverEffect(
                scaleTarget = 1.01f,
                activeBorderWidth = 1.5.dp,
                activeBorderColor = if (canOpenDescription) colors.accentPrimary else Color.Transparent,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = colors.borderSubtle,
                shape = shapes.container,
                tiltEnabled = false,
                clickEnabled = canOpenDescription,
                onClick = {
                    if (canOpenDescription) {
                        onOpenDescriptionDrawer()
                    }
                }
            )
            .background(colors.bgSurface, shapes.container)
            .padding(18.dp)
    ) {
        if (isEpisodesLoading) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(20.dp).shimmerPlaceholder(true, shapes.micro))
                Box(modifier = Modifier.fillMaxWidth(0.25f).height(14.dp).shimmerPlaceholder(true, shapes.micro))
                Box(modifier = Modifier.fillMaxWidth(0.85f).height(36.dp).shimmerPlaceholder(true, shapes.micro))
            }
        } else if (focusedEpisode != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Episode Title + Watched Indicator
                Row(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AvalonText(
                        text = "${focusedEpisode.episodeNumber}. ${focusedEpisode.name}",
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (focusedEpisode.isWatched) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(shapes.micro)
                                .background(colors.statusLive.copy(alpha = 0.15f))
                                .border(1.dp, colors.statusLive.copy(alpha = 0.35f), shapes.micro)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AvalonIcon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = colors.statusLive,
                                modifier = Modifier.size(12.dp)
                            )
                            AvalonText(
                                text = stringResource(Res.string.details_seasons_completed),
                                style = typography.labelSmall,
                                color = colors.statusLive
                            )
                        }
                    }
                }

                // Metadata Row: Air Date • Duration • Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!focusedEpisode.airDate.isNullOrEmpty()) {
                        AvalonText(
                            text = focusedEpisode.airDate.orEmpty(),
                            style = typography.metadata,
                            color = colors.accentPrimary
                        )
                    }

                    val runtime = focusedEpisode.runtime
                    if (runtime != null && runtime > 0) {
                        if (!focusedEpisode.airDate.isNullOrEmpty()) {
                            AvalonText(text = "•", style = typography.metadata, color = colors.textMuted)
                        }
                        AvalonText(
                            text = stringResource(Res.string.player_duration_mins_single_fmt, runtime),
                            style = typography.metadata,
                            color = colors.textSecondary
                        )
                    }

                    val voteAvg = focusedEpisode.voteAverage
                    if (voteAvg != null && voteAvg > 0.0) {
                        val formattedRating = ((voteAvg * 10).toInt() / 10.0).toString()
                        AvalonText(text = "•", style = typography.metadata, color = colors.textMuted)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            AvalonIcon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = colors.accentGold,
                                modifier = Modifier.size(12.dp)
                            )
                            AvalonText(
                                text = formattedRating,
                                style = typography.metadata,
                                color = colors.accentGold
                            )
                        }
                    }
                }

                // Description text (3 lines max, large & readable from couch)
                val descText = focusedEpisode.overview?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.details_seasons_no_desc)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AvalonText(
                        text = descText,
                        style = typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (canOpenDescription) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AvalonIcon(
                            imageVector = Lucide.Maximize2,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else {
            AvalonText(
                text = stringResource(Res.string.details_seasons_not_available),
                style = typography.bodyMedium,
                color = colors.textSecondary
            )
        }
    }
}
