package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.episodes_notifications_missed_title
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.NotificationType
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvEdgeGatedFocusProvider
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.EpisodesNotificationsEmptyState
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.SectionGroupItem
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.ShowGroupCardSkeleton
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.groupEpisodesByShow
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components.TvEpisodeActionsDrawer
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components.TvMissedRollupCard
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components.TvMovieNotificationCard
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components.TvNotificationsHeader
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components.TvShowShelf
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodeFilter
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodesNotificationsViewState
import org.jetbrains.compose.resources.stringResource

/**
 * ТВ-лейаут экрана уведомлений: горизонтальные полки (TvShowShelf), широкие карточки фильмов,
 * D-Pad навигация, оверскан-отступы и выезжающая шторка действий (TvEpisodeActionsDrawer).
 */
@Composable
fun EpisodesNotificationsLayoutTv(
    modifier: Modifier = Modifier,
    state: EpisodesNotificationsViewState,
    onAction: (Action) -> Unit,
    onFilterSelected: (EpisodeFilter) -> Unit,
    expectedItemsCount: Int? = null,
) {
    val slotData = state.feedSlot?.state?.data
    val isLoading = state.feedSlot?.state?.isLoading == true
    val lazyListState = rememberLazyListState()

    var drawerEpisode by remember { mutableStateOf<NewEpisodeCardItem?>(null) }

    val countAll = slotData?.totalUnreadCount ?: 0
    val countRecent = slotData?.recentUnreadCount ?: 0
    val countUnwatched = slotData?.unwatchedCount ?: 0

    val headerFocusRequester = remember { FocusRequester() }

    TvEdgeGatedFocusProvider(pivotFraction = 0.35f, safeViewportFraction = 0.85f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // ТВ-шапка с фильтрами и кнопкой "Прочитать все"
                TvNotificationsHeader(
                    totalUnreadCount = slotData?.totalUnreadCount ?: 0,
                    selectedFilter = state.selectedFilter,
                    countAll = countAll,
                    countRecent = countRecent,
                    countUnwatched = countUnwatched,
                    onFilterSelected = onFilterSelected,
                    onMarkAllReadClicked = {
                        slotData?.markAllReadAction?.let(onAction)
                    },
                    firstTabFocusRequester = headerFocusRequester,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                when {
                    isLoading && slotData == null -> {
                        val count = (expectedItemsCount ?: 3).coerceAtLeast(1)
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 36.dp)
                        ) {
                            items(count) {
                                ShowGroupCardSkeleton()
                            }
                        }
                    }

                    slotData == null || (slotData.sections.isEmpty() && slotData.rollups.isEmpty()) -> {
                        EpisodesNotificationsEmptyState(
                            onAction = onAction,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                        )
                    }

                    else -> {
                        val filteredSections = remember(slotData.sections, state.selectedFilter) {
                            when (state.selectedFilter) {
                                EpisodeFilter.ALL -> slotData.sections
                                EpisodeFilter.RECENT_7_DAYS -> slotData.sections.filter { it.sectionId == "today" || it.sectionId == "this_week" }
                                EpisodeFilter.UNWATCHED -> slotData.sections.map { sec ->
                                    sec.copy(episodes = sec.episodes.filter { !it.isWatched })
                                }.filter { it.episodes.isNotEmpty() }
                            }
                        }

                        if (filteredSections.isEmpty() && slotData.rollups.isEmpty()) {
                            EpisodesNotificationsEmptyState(
                                onAction = onAction,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                            ) {
                                filteredSections.forEach { section ->
                                    item(key = "header_${section.sectionId}") {
                                        Text(
                                            text = section.title.uppercase(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = TextUnit(1.2f, TextUnitType.Sp),
                                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp)
                                        )
                                    }

                                    val groupedItems = groupEpisodesByShow(section.episodes)

                                    items(
                                        items = groupedItems,
                                        key = { item ->
                                            when (item) {
                                                is SectionGroupItem.Show -> "tv_show_${section.sectionId}_${item.mediaKey}"
                                                is SectionGroupItem.Movie -> "tv_movie_${section.sectionId}_${item.item.id}"
                                            }
                                        }
                                    ) { groupItem ->
                                        when (groupItem) {
                                            is SectionGroupItem.Show -> {
                                                TvShowShelf(
                                                    showTitle = groupItem.title,
                                                    showPosterUrl = groupItem.posterUrl,
                                                    episodes = groupItem.episodes,
                                                    onAction = onAction,
                                                    onEpisodeOptionsClick = { episode ->
                                                        drawerEpisode = episode
                                                    }
                                                )
                                            }
                                            is SectionGroupItem.Movie -> {
                                                TvMovieNotificationCard(
                                                    item = groupItem.item,
                                                    onAction = onAction,
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Секция пачек пропущенных серий (Smart Rollup)
                                if (slotData.rollups.isNotEmpty() && state.selectedFilter == EpisodeFilter.ALL) {
                                    item(key = "header_tv_missed_rollups") {
                                        Text(
                                            text = stringResource(Res.string.episodes_notifications_missed_title).uppercase(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.5f),
                                            letterSpacing = TextUnit(1.2f, TextUnitType.Sp),
                                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
                                        )
                                    }

                                    items(
                                        items = slotData.rollups,
                                        key = { "tv_rollup_${it.mediaKey}" }
                                    ) { rollup ->
                                        TvMissedRollupCard(
                                            rollup = rollup,
                                            onAction = onAction,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                item(key = "tv_bottom_spacer") {
                                    Spacer(modifier = Modifier.height(40.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Выезжающая шторка действий серии
            TvEpisodeActionsDrawer(
                isOpen = drawerEpisode != null,
                episode = drawerEpisode,
                onDismiss = { drawerEpisode = null },
                onAction = onAction
            )
        }
    }
}
