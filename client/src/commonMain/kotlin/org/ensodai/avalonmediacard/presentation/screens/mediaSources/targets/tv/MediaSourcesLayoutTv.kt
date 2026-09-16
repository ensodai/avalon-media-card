package org.ensodai.avalonmediacard.presentation.screens.mediaSources.targets.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.common_close
import avalonmediacard.client.generated.resources.details_sources_add_torrent
import avalonmediacard.client.generated.resources.details_sources_all
import avalonmediacard.client.generated.resources.details_sources_not_found
import avalonmediacard.client.generated.resources.details_sources_refresh
import avalonmediacard.client.generated.resources.details_sources_select_file
import avalonmediacard.client.generated.resources.details_sources_torrent_analyzing
import avalonmediacard.client.generated.resources.details_sources_torrent_processing
import avalonmediacard.client.generated.resources.details_sources_torrents
import avalonmediacard.client.generated.resources.details_sources_upload_torrent_desc
import avalonmediacard.client.generated.resources.details_sources_upload_torrent_title
import com.composables.icons.lucide.FileUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RotateCw
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.presentation.components.shimmerEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.horizontalScrollWithMouseAndTouch
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.MediaSourcesActions
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.components.MovieSourceCard
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.components.SeasonGroupSourceCard
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.components.SingleEpisodeSourceCard
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.components.SourceTab
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.components.SubFilterChip
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.components.TorrentSourceCard
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.MovieSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SeasonGroupSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SingleEpisodeSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.TorrentSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.viewState.MediaSourcesViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun MediaSourcesLayoutTv(
    state: MediaSourcesViewState,
    actions: MediaSourcesActions,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = 24.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isAddTorrentMode) {
                // Шапка режима добавления торрента
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = horizontalPadding, end = horizontalPadding, top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onClose)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.X,
                                contentDescription = stringResource(Res.string.common_close),
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            Lucide.FileUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.details_sources_add_torrent),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onToggleAddTorrent)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.X,
                            contentDescription = stringResource(Res.string.common_close),
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = horizontalPadding, vertical = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isUploadingFile) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(Res.string.details_sources_torrent_processing),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            state.uploadingFileName?.let { name ->
                                Text(
                                    text = name,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = stringResource(Res.string.details_sources_torrent_analyzing),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Lucide.FileUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = stringResource(Res.string.details_sources_upload_torrent_title),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(Res.string.details_sources_upload_torrent_desc),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                            state.uploadErrorMessage?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .tvAndWebHoverEffect(scaleTarget = 1.05f, shape = RoundedCornerShape(8.dp), onClick = actions.onPickTorrentFile)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.details_sources_select_file),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Строка управления: кнопки действий слева + динамические табы справа
                val providerTabsScrollState = rememberLazyListState()
                val torrentsLabel = stringResource(Res.string.details_sources_torrents)

                LaunchedEffect(state.selectedTabIndex) {
                    if (state.selectedTabIndex in state.visibleSlots.indices) {
                        providerTabsScrollState.animateScrollToItem(state.selectedTabIndex)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = horizontalPadding, end = horizontalPadding, top = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. КНОПКА ЗАКРЫТЬ
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onClose)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.X,
                                contentDescription = stringResource(Res.string.common_close),
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 2. КНОПКА ОБНОВИТЬ (ПОИСК ЗАНОВО)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onRefresh)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.RotateCw,
                                contentDescription = stringResource(Res.string.details_sources_refresh),
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 3. КНОПКА ДОБАВИТЬ СВОЙ ТОРРЕНТ
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onToggleAddTorrent)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.FileUp,
                                contentDescription = stringResource(Res.string.details_sources_add_torrent),
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 4. ДИНАМИЧЕСКИЕ ТАБЫ ПРОВАЙДЕРОВ С ГОРИЗОНТАЛЬНЫМ СКРОЛЛОМ
                    LazyRow(
                        state = providerTabsScrollState,
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScrollWithMouseAndTouch(providerTabsScrollState, wheelSpeedMultiplier = 0.35f),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(state.visibleSlots, key = { index, slot -> slot.nodeId }) { index, slot ->
                            val isTorrentsSlot = slot.nodeId.contains("torrserver", ignoreCase = true) ||
                                    slot.nodeId.contains("jackett", ignoreCase = true) ||
                                    slot.nodeId.contains("prowlarr", ignoreCase = true)

                            val providerTitle = if (isTorrentsSlot) {
                                torrentsLabel
                            } else {
                                slot.state.data?.providerTitle ?: slot.nodeId.replace("-plugin", "")
                            }

                            val isSlotLoading = slot.state.isLoading || slot.state.isInitialLoading
                            val count = slot.state.data?.sources?.size ?: 0

                            SourceTab(
                                text = providerTitle,
                                count = count,
                                isLoading = isSlotLoading,
                                isSelected = state.selectedTabIndex == index,
                                isTv = true,
                                onClick = { actions.onSelectTab(index) }
                            )
                        }
                    }
                }

                // Подчипсы фильтрации
                val allFilterLabel = stringResource(Res.string.details_sources_all)
                if (state.subFilters.isNotEmpty()) {
                    val subFiltersScrollState = rememberLazyListState()
                    LazyRow(
                        state = subFiltersScrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (horizontalPadding - 4.dp).coerceAtLeast(0.dp), end = (horizontalPadding - 4.dp).coerceAtLeast(0.dp), bottom = 12.dp)
                            .horizontalScrollWithMouseAndTouch(subFiltersScrollState, wheelSpeedMultiplier = 0.35f),
                        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "subfilter_all") {
                            SubFilterChip(
                                label = allFilterLabel,
                                count = state.currentSources.size,
                                isSelected = state.selectedSubFilter == null,
                                isTv = true,
                                onClick = { actions.onSelectSubFilter(null) }
                            )
                        }
                        items(state.subFilters, key = { it.id }) { filter ->
                            SubFilterChip(
                                label = filter.title,
                                count = filter.count,
                                isSelected = state.selectedSubFilter?.id == filter.id,
                                isTv = true,
                                onClick = { actions.onSelectSubFilter(filter) }
                            )
                        }
                    }
                }

                // Контент источников (на весь доступный вес экрана ТВ)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (state.isCurrentLoading && state.currentSources.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = horizontalPadding, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .shimmerEffect()
                                )
                            }
                        }
                    } else if (state.currentSources.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.details_sources_not_found),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 18.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.displayItems, key = { it.id.ifBlank { "${it.stream.url}_${it.stream.title}_${it.hashCode()}" } }) { item ->
                                val isThisLoading = state.loadingSourceId == item.id || state.loadingTorrentUrl == item.stream.url
                                val isOtherLoading = (state.loadingSourceId != null && state.loadingSourceId != item.id) ||
                                        (state.loadingTorrentUrl != null && state.loadingTorrentUrl != item.stream.url)

                                when (item) {
                                    is TorrentSourceUiItem -> TorrentSourceCard(
                                        item = item,
                                        isTv = true,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = { actions.onItemClick(item) }
                                    )
                                    is MovieSourceUiItem -> MovieSourceCard(
                                        item = item,
                                        isTv = true,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = { actions.onItemClick(item) }
                                    )
                                    is SeasonGroupSourceUiItem -> SeasonGroupSourceCard(
                                        item = item,
                                        isTv = true,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = { actions.onItemClick(item) }
                                    )
                                    is SingleEpisodeSourceUiItem -> SingleEpisodeSourceCard(
                                        item = item,
                                        isTv = true,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = { actions.onItemClick(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
