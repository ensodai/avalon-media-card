package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    var wasSeasonDrawerOpen by remember { mutableStateOf(false) }
    var wasBannerDrawerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(isSeasonDrawerOpen) {
        if (isSeasonDrawerOpen) {
            wasSeasonDrawerOpen = true
        } else if (wasSeasonDrawerOpen) {
            wasSeasonDrawerOpen = false
            runCatching { seasonButtonFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(isEpisodeDescriptionDrawerOpen) {
        if (isEpisodeDescriptionDrawerOpen) {
            wasBannerDrawerOpen = true
        } else if (wasBannerDrawerOpen) {
            wasBannerDrawerOpen = false
            runCatching { bannerFocusRequester.requestFocus() }
        }
    }

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
                Text(
                    text = stringResource(Res.string.details_seasons_not_available),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
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
            Text(
                text = stringResource(Res.string.details_seasons_header),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (!isLoading && component.seasons.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val seasonRes = getPluralSeasonRes(seasonsCount)
                    val episodeRes = getPluralEpisodeRes(totalEpisodes)
                    val seasonCountText = stringResource(seasonRes, seasonsCount)
                    val episodesText = if (totalEpisodes > 0) " • " + stringResource(episodeRes, totalEpisodes) else ""
                    val specialsText = if (hasSpecials && regularSeasons.isNotEmpty()) " • " + stringResource(Res.string.details_seasons_specials) else ""
                    Text(
                        text = "$seasonCountText$episodesText$specialsText",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f)
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
                            activeBorderColor = Color.White,
                            defaultBorderWidth = 1.dp,
                            defaultBorderColor = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            tiltEnabled = false,
                            onClick = onOpenSeasonDrawer
                        )
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = getSeasonDisplayName(selectedSeason),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Lucide.ChevronDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Mark Season Button
                val isFullyWatched = selectedSeason.isFullyWatched
                val markSeasonColor = if (isFullyWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.85f)
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.04f,
                            activeBorderWidth = 2.dp,
                            activeBorderColor = if (isFullyWatched) Color(0xFF4CAF50) else Color.White,
                            defaultBorderWidth = 1.dp,
                            defaultBorderColor = if (isFullyWatched) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            tiltEnabled = false,
                            onClick = { selectedSeason.markWatchedAction?.let(onAction) }
                        )
                        .background(
                            if (isFullyWatched) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isFullyWatched) Lucide.CheckCheck else Lucide.Eye,
                        contentDescription = null,
                        tint = markSeasonColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFullyWatched) stringResource(Res.string.details_seasons_unmark_btn) else stringResource(Res.string.details_seasons_mark_btn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
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
                activeBorderColor = if (canOpenDescription) Color.White else Color.Transparent,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp),
                tiltEnabled = false,
                clickEnabled = canOpenDescription,
                onClick = {
                    if (canOpenDescription) {
                        onOpenDescriptionDrawer()
                    }
                }
            )
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        if (isEpisodesLoading) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(20.dp).shimmerPlaceholder(true, RoundedCornerShape(4.dp)))
                Box(modifier = Modifier.fillMaxWidth(0.25f).height(14.dp).shimmerPlaceholder(true, RoundedCornerShape(4.dp)))
                Box(modifier = Modifier.fillMaxWidth(0.85f).height(36.dp).shimmerPlaceholder(true, RoundedCornerShape(4.dp)))
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
                    Text(
                        text = "${focusedEpisode.episodeNumber}. ${focusedEpisode.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (focusedEpisode.isWatched) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(Res.string.details_seasons_completed),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4CAF50)
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
                        Text(
                            text = focusedEpisode.airDate.orEmpty(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val runtime = focusedEpisode.runtime
                    if (runtime != null && runtime > 0) {
                        if (!focusedEpisode.airDate.isNullOrEmpty()) {
                            Text(text = "•", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f))
                        }
                        Text(
                            text = stringResource(Res.string.player_duration_mins_single_fmt, runtime),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    val voteAvg = focusedEpisode.voteAverage
                    if (voteAvg != null && voteAvg > 0.0) {
                        val formattedRating = ((voteAvg * 10).toInt() / 10.0).toString()
                        Text(text = "•", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC107)
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
                    Text(
                        text = descText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (canOpenDescription) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Lucide.Maximize2,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else {
            Text(
                text = stringResource(Res.string.details_seasons_not_available),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
