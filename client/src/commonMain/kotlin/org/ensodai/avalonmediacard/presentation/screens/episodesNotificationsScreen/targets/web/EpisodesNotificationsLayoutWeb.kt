package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.episodes_notifications_missed_title
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.NotificationType
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.EpisodesNotificationsEmptyState
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.EpisodesNotificationsHeader
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.MissedShowRollupCard
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.MovieNotificationCard
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.SectionGroupItem
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.ShowGroupCard
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.ShowGroupCardSkeleton
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.groupEpisodesByShow
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodeFilter
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodesNotificationsViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun EpisodesNotificationsLayoutWeb(
    state: EpisodesNotificationsViewState,
    onAction: (Action) -> Unit,
    onFilterSelected: (EpisodeFilter) -> Unit,
    expectedItemsCount: Int? = null,
    modifier: Modifier = Modifier
) {
    val slotData = state.feedSlot?.state?.data
    val isLoading = state.feedSlot?.state?.isLoading == true

    val countAll = slotData?.totalUnreadCount ?: 0
    val countRecent = slotData?.recentUnreadCount ?: 0
    val countUnwatched = slotData?.unwatchedCount ?: 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp)
        ) {
            // Верхняя панель и фильтры
            EpisodesNotificationsHeader(
                totalUnreadCount = slotData?.totalUnreadCount ?: 0,
                selectedFilter = state.selectedFilter,
                countAll = countAll,
                countRecent = countRecent,
                countUnwatched = countUnwatched,
                onFilterSelected = onFilterSelected,
                onMarkAllReadClicked = {
                    slotData?.markAllReadAction?.let(onAction)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            when {
                isLoading && slotData == null -> {
                    val count = (expectedItemsCount ?: 3).coerceAtLeast(1)
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(count) {
                            ShowGroupCardSkeleton()
                        }
                    }
                }

                slotData == null || (slotData.sections.isEmpty() && slotData.rollups.isEmpty()) -> {
                    EpisodesNotificationsEmptyState(
                        onAction = onAction,
                        modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Секции хронологического таймлайна
                            filteredSections.forEach { section ->
                                item(key = "header_${section.sectionId}") {
                                    Text(
                                        text = section.title.uppercase(),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = TextUnit(1.2f, TextUnitType.Sp)
                                        ),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }

                                val groupedItems = groupEpisodesByShow(section.episodes)

                                items(
                                    items = groupedItems,
                                    key = { item ->
                                        when (item) {
                                            is SectionGroupItem.Show -> "show_${section.sectionId}_${item.mediaKey}"
                                            is SectionGroupItem.Movie -> "movie_${section.sectionId}_${item.item.id}"
                                        }
                                    }
                                ) { groupItem ->
                                    when (groupItem) {
                                        is SectionGroupItem.Show -> {
                                            ShowGroupCard(
                                                showTitle = groupItem.title,
                                                showPosterUrl = groupItem.posterUrl,
                                                episodes = groupItem.episodes,
                                                onAction = onAction
                                            )
                                        }
                                        is SectionGroupItem.Movie -> {
                                            MovieNotificationCard(
                                                item = groupItem.item,
                                                onAction = onAction
                                            )
                                        }
                                    }
                                }
                            }

                            // Секция пачек пропущенных серий (Smart Rollup)
                            if (slotData.rollups.isNotEmpty() && state.selectedFilter == EpisodeFilter.ALL) {
                                item(key = "header_missed_rollups") {
                                    Text(
                                        text = stringResource(Res.string.episodes_notifications_missed_title).uppercase(),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = TextUnit(1.2f, TextUnitType.Sp)
                                        ),
                                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                    )
                                }

                                items(
                                    items = slotData.rollups,
                                    key = { it.mediaKey.toString() }
                                ) { rollup ->
                                    MissedShowRollupCard(
                                        rollup = rollup,
                                        onAction = onAction
                                    )
                                }
                            }

                            item(key = "bottom_spacer") {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
