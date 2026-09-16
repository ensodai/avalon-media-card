package org.ensodai.avalonmediacard.presentation.screens.mediaSources.targets.web

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import avalonmediacard.client.generated.resources.details_sources_select
import avalonmediacard.client.generated.resources.details_sources_select_file
import avalonmediacard.client.generated.resources.details_sources_torrent_analyzing
import avalonmediacard.client.generated.resources.details_sources_torrent_processing
import avalonmediacard.client.generated.resources.details_sources_torrents
import avalonmediacard.client.generated.resources.details_sources_upload_torrent_desc
import avalonmediacard.client.generated.resources.details_sources_upload_torrent_title
import com.composables.icons.lucide.FileUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MonitorPlay
import com.composables.icons.lucide.RotateCw
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.presentation.components.shimmerEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
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
fun MediaSourcesLayoutWeb(
    state: MediaSourcesViewState,
    actions: MediaSourcesActions,
    modifier: Modifier = Modifier
) {
    val deviceTarget = LocalDeviceTarget.current
    val isTouch = deviceTarget.isTouch
    val maxWidth = if (isTouch) 800.dp else 640.dp
    val horizontalPadding = 24.dp

    Box(
        modifier = modifier
            .padding(top = 16.dp)
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Шапка диалогового окна
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.02f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Lucide.MonitorPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.details_sources_select),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val btnSize = if (isTouch) 44.dp else 32.dp
                    val iconSize = if (isTouch) 20.dp else 16.dp

                    // КНОПКА ДОБАВЛЕНИЯ СВОЕГО ТОРРЕНТА
                    Box(
                        modifier = Modifier
                            .size(btnSize)
                            .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onToggleAddTorrent)
                            .clip(CircleShape)
                            .background(if (state.isAddTorrentMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.FileUp,
                            contentDescription = stringResource(Res.string.details_sources_add_torrent),
                            tint = if (state.isAddTorrentMode) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    // КНОПКА ОБНОВИТЬ (ПОИСК ЗАНОВО)
                    Box(
                        modifier = Modifier
                            .size(btnSize)
                            .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onRefresh)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.RotateCw,
                            contentDescription = stringResource(Res.string.details_sources_refresh),
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    // КНОПКА ЗАКРЫТЬ
                    Box(
                        modifier = Modifier
                            .size(btnSize)
                            .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onClose)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.X,
                            contentDescription = stringResource(Res.string.common_close),
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }

            if (state.isAddTorrentMode) {
                // Вкладка добавления своего торрента
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 0.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isUploadingFile) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = stringResource(Res.string.details_sources_torrent_processing),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            state.uploadingFileName?.let { fname ->
                                Text(
                                    text = fname,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = stringResource(Res.string.details_sources_torrent_analyzing),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(Res.string.details_sources_upload_torrent_desc),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
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
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Динамические табы
                val providerTabsScrollState = rememberLazyListState()
                val torrentsLabel = stringResource(Res.string.details_sources_torrents)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        state = providerTabsScrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScrollWithMouseAndTouch(providerTabsScrollState, wheelSpeedMultiplier = 0.35f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(state.visibleSlots, key = { _, slot -> slot.nodeId }) { index, slot ->
                            val providerTitle = slot.state.data?.providerTitle
                                ?: if (slot.nodeId.contains("torrserver", ignoreCase = true)) torrentsLabel
                                else slot.nodeId.replaceFirstChar { it.uppercase() }
                            val isSlotLoading = slot.state.isLoading || slot.state.isInitialLoading
                            val count = slot.state.data?.sources?.size ?: 0

                            SourceTab(
                                text = providerTitle,
                                count = count,
                                isLoading = isSlotLoading,
                                isSelected = state.selectedTabIndex == index,
                                isTv = false,
                                onClick = { actions.onSelectTab(index) }
                            )
                        }
                    }
                }

                // Универсальные подчипсы фильтрации
                val allFilterLabel = stringResource(Res.string.details_sources_all)
                if (state.subFilters.isNotEmpty()) {
                    val subFiltersScrollState = rememberLazyListState()
                    LazyRow(
                        state = subFiltersScrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = horizontalPadding, end = horizontalPadding, bottom = 12.dp)
                            .horizontalScrollWithMouseAndTouch(subFiltersScrollState, wheelSpeedMultiplier = 0.35f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "subfilter_all") {
                            SubFilterChip(
                                label = allFilterLabel,
                                count = state.currentSources.size,
                                isSelected = state.selectedSubFilter == null,
                                isTv = false,
                                onClick = { actions.onSelectSubFilter(null) }
                            )
                        }
                        items(state.subFilters, key = { it.id }) { filter ->
                            SubFilterChip(
                                label = filter.title,
                                count = filter.count,
                                isSelected = state.selectedSubFilter?.id == filter.id,
                                isTv = false,
                                onClick = { actions.onSelectSubFilter(filter) }
                            )
                        }
                    }
                }

                // Контент провайдера
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp, max = 500.dp)
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
                                        .height(60.dp)
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
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.displayItems, key = { it.id.ifBlank { "${it.stream.url}_${it.stream.title}_${it.hashCode()}" } }) { item ->
                                val isThisLoading = state.loadingSourceId == item.id || state.loadingTorrentUrl == item.stream.url
                                val isOtherLoading = (state.loadingSourceId != null && state.loadingSourceId != item.id) ||
                                        (state.loadingTorrentUrl != null && state.loadingTorrentUrl != item.stream.url)

                                when (item) {
                                    is TorrentSourceUiItem -> TorrentSourceCard(
                                        item = item,
                                        isTv = false,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = { actions.onItemClick(item) }
                                    )
                                    is MovieSourceUiItem -> MovieSourceCard(
                                        item = item,
                                        isTv = false,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = { actions.onItemClick(item) }
                                    )
                                    is SeasonGroupSourceUiItem -> SeasonGroupSourceCard(
                                        item = item,
                                        isTv = false,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = { actions.onItemClick(item) }
                                    )
                                    is SingleEpisodeSourceUiItem -> SingleEpisodeSourceCard(
                                        item = item,
                                        isTv = false,
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
