package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionPlayVideo
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.UploadCustomTorrentCommand
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerEffect
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.horizontalScrollWithMouseAndTouch
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot.model.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun MediaSourcesSection(
    mediaSourcesList: List<SduiSlot<SlotData.MediaSources>>,
    onAction: (Action) -> Unit,
    isExpanded: Boolean,
    onCloseSources: () -> Unit,
    onSelectSource: ((providerId: String, sourceId: String, seasonNumber: Int?, episodeNumber: Int?, onComplete: () -> Unit) -> Unit)? = null,
    onRefreshSources: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {

    val deviceTarget = LocalDeviceTarget.current
    val isTv = deviceTarget.isTv
    val isTouch = deviceTarget.isTouch

    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedSubFilterId by remember(selectedTabIndex) { mutableStateOf<String?>(null) }
    var isAddTorrentMode by remember { mutableStateOf(false) }

    // Show only slots that are either still loading or have discovered sources
    // If all are done loading and empty, fallback to mediaSourcesList so user sees the empty state
    val visibleSlots = remember(mediaSourcesList) {
        val nonEmptiesOrLoading = mediaSourcesList.filter { 
            val isSlotLoading = it.state.isLoading || it.state.isInitialLoading
            val hasSources = (it.state.data?.sources?.isNotEmpty() == true)
            isSlotLoading || hasSources
        }
        if (nonEmptiesOrLoading.isNotEmpty()) nonEmptiesOrLoading else mediaSourcesList
    }

    val safeTabIndex = selectedTabIndex.coerceIn(0, (visibleSlots.size - 1).coerceAtLeast(0))
    val currentSlot = visibleSlots.getOrNull(safeTabIndex)
    val currentData = currentSlot?.state?.data
    val currentSources = currentData?.sources ?: emptyList()
    val isCurrentLoading = currentSlot?.state?.isLoading == true || currentSlot?.state?.isInitialLoading == true

    var loadingTorrentUrl by remember { mutableStateOf<String?>(null) }
    var loadingSourceId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentSources) {
        loadingTorrentUrl = null
        loadingSourceId = null
    }

    var isUploadingFile by remember { mutableStateOf(false) }
    var uploadingFileName by remember { mutableStateOf<String?>(null) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val filePicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("torrent"))
    ) { file ->
        file?.let {
            uploadingFileName = it.name
            isUploadingFile = true
            uploadErrorMessage = null
            coroutineScope.launch {
                try {
                    val bytes = it.readBytes()
                    val key = currentData?.mediaKey
                    if (key != null) {
                        onAction(UploadCustomTorrentCommand(key, it.name, bytes))
                    } else {
                        isUploadingFile = false
                        uploadErrorMessage = "Media key not found"
                    }
                } catch (e: Exception) {
                    isUploadingFile = false
                    uploadErrorMessage = e.message ?: "File read error"
                }
            }
        }
    }

    val maxWidth = if (isTv || isTouch) 800.dp else 640.dp

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
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            // === ШАПКА ===
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
                        fontSize = if (isTv) 20.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val btnSize = if (isTouch || isTv) 44.dp else 32.dp
                    val iconSize = if (isTouch || isTv) 20.dp else 16.dp

                    // КНОПКА ДОБАВЛЕНИЯ СВОЕГО ТОРРЕНТА
                    Box(
                        modifier = Modifier
                            .size(btnSize)
                            .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = {
                                isAddTorrentMode = !isAddTorrentMode
                                if (!isAddTorrentMode) {
                                    isUploadingFile = false
                                    uploadErrorMessage = null
                                    uploadingFileName = null
                                }
                            })
                            .clip(CircleShape)
                            .background(if (isAddTorrentMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Lucide.FileUp,
                            contentDescription = stringResource(Res.string.details_sources_add_torrent),
                            tint = if (isAddTorrentMode) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    // КНОПКА ОБНОВИТЬ (ПОИСК ЗАНОВО)
                    Box(
                        modifier = Modifier
                            .size(btnSize)
                            .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = {
                                onRefreshSources?.invoke()
                            })
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
                            .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = onCloseSources)
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

            if (isAddTorrentMode) {
                // Вкладка добавления своего торрента
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingFile) {
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
                                fontSize = if (isTv) 20.sp else 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            uploadingFileName?.let { fname ->
                                Text(
                                    text = fname,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = if (isTv) 16.sp else 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = stringResource(Res.string.details_sources_torrent_analyzing),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = if (isTv) 14.sp else 13.sp,
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
                                fontSize = if (isTv) 20.sp else 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(Res.string.details_sources_upload_torrent_desc),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = if (isTv) 16.sp else 14.sp
                            )
                            uploadErrorMessage?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .tvAndWebHoverEffect(scaleTarget = 1.05f, shape = RoundedCornerShape(8.dp), onClick = {
                                        uploadErrorMessage = null
                                        filePicker.launch()
                                    })
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.details_sources_select_file),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isTv) 16.sp else 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // === ДИНАМИЧЕСКИЕ ТАБЫ (ЧИПСЫ) ОТ ПЛАГИНОВ ===
                val providerTabsScrollState = rememberLazyListState()
                val torrentsLabel = stringResource(Res.string.details_sources_torrents)
                LazyRow(
                    state = providerTabsScrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .horizontalScrollWithMouseAndTouch(providerTabsScrollState, wheelSpeedMultiplier = 0.35f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(visibleSlots, key = { _, slot -> slot.nodeId }) { index, slot ->
                        val providerTitle = slot.state.data?.providerTitle
                            ?: if (slot.nodeId.contains("torrserver", ignoreCase = true)) torrentsLabel
                            else slot.nodeId.replaceFirstChar { it.uppercase() }
                        val isSlotLoading = slot.state.isLoading || slot.state.isInitialLoading
                        val count = slot.state.data?.sources?.size ?: 0

                        SourceTab(
                            text = providerTitle,
                            count = count,
                            isLoading = isSlotLoading,
                            isSelected = safeTabIndex == index,
                            isTv = isTv,
                            onClick = {
                                selectedTabIndex = index
                                selectedSubFilterId = null
                            }
                        )
                    }
                }

                // === УНИВЕРСАЛЬНЫЕ ПОДЧИПСЫ ФИЛЬТРАЦИИ (СЕЗОНЫ, ТРЕКЕРЫ, СТУДИИ) ===
                val subFilters = currentData?.subFilters ?: emptyList()
                val allFilterLabel = stringResource(Res.string.details_sources_all)
                if (subFilters.isNotEmpty()) {
                    val subFiltersScrollState = rememberLazyListState()
                    LazyRow(
                        state = subFiltersScrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                            .horizontalScrollWithMouseAndTouch(subFiltersScrollState, wheelSpeedMultiplier = 0.35f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "subfilter_all") {
                            SubFilterChip(
                                label = allFilterLabel,
                                count = currentSources.size,
                                isSelected = selectedSubFilterId == null,
                                isTv = isTv,
                                onClick = { selectedSubFilterId = null }
                            )
                        }
                        items(subFilters, key = { it.id }) { filter ->
                            SubFilterChip(
                                label = filter.label,
                                count = filter.count,
                                isSelected = selectedSubFilterId == filter.id,
                                isTv = isTv,
                                onClick = { selectedSubFilterId = filter.id }
                            )
                        }
                    }
                }

                // === КОНТЕНТ ВЫБРАННОГО ПРОВАЙДЕРА ===
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp, max = 500.dp)) {
                    if (isCurrentLoading && currentSources.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (isTv) 72.dp else 60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .shimmerEffect()
                                )
                            }
                        }
                    } else if (currentSources.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.details_sources_not_found),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = if (isTv) 18.sp else 16.sp
                            )
                        }
                    } else {
                        val isTvShow = currentData?.mediaKey?.type == EntityType.TV
                        val uiItems = remember(currentSources, isTvShow) { currentSources.map { it.toSourceUiItem(isTvShow) } }
                        val isTorrents = uiItems.any { it is TorrentSourceUiItem }
                        val displayItems = remember(uiItems, isTorrents) {
                            if (isTorrents) {
                                uiItems.distinctBy { (it as? TorrentSourceUiItem)?.stream?.url?.substringBefore("&index=") ?: it.id }
                            } else {
                                uiItems.distinctBy { it.id }
                            }
                        }
                        val filteredDisplayItems = remember(displayItems, selectedSubFilterId) {
                            if (selectedSubFilterId == null) {
                                displayItems
                            } else {
                                displayItems.filter { item ->
                                    val stream = item.stream
                                    stream.subFilterId == selectedSubFilterId ||
                                    (stream.seasonNumber != null && "season_${stream.seasonNumber}" == selectedSubFilterId) ||
                                    (stream.sourceName.isNotBlank() && "tracker_${stream.sourceName.lowercase()}" == selectedSubFilterId)
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredDisplayItems, key = { it.id.ifBlank { "${it.stream.url}_${it.stream.title}_${it.hashCode()}" } }) { item ->
                                val isThisLoading = loadingSourceId == item.id || loadingTorrentUrl == item.stream.url
                                val isOtherLoading = (loadingSourceId != null && loadingSourceId != item.id) ||
                                        (loadingTorrentUrl != null && loadingTorrentUrl != item.stream.url)

                                val onItemClick: () -> Unit = {
                                    if (!isOtherLoading && !isThisLoading) {
                                        if (item is TorrentSourceUiItem) {
                                            loadingTorrentUrl = item.stream.url
                                        } else {
                                            loadingSourceId = item.id
                                        }
                                        val pId = currentData?.providerId ?: item.stream.sourceName
                                        val sId = item.id.ifBlank { item.stream.canonicalId ?: item.stream.url }

                                        val targetSeason = when (item) {
                                            is SeasonGroupSourceUiItem -> item.seasonNumber
                                            is SingleEpisodeSourceUiItem -> item.seasonNumber
                                            else -> item.stream.seasonNumber
                                        }
                                        val targetEpisode = when (item) {
                                            is SeasonGroupSourceUiItem -> 1
                                            is SingleEpisodeSourceUiItem -> item.episodeNumber
                                            else -> item.stream.episodeNumber
                                        }

                                        if (onSelectSource != null) {
                                            onSelectSource(pId, sId, targetSeason, targetEpisode) {
                                                loadingTorrentUrl = null
                                                loadingSourceId = null
                                            }
                                        } else {
                                            val act = item.clickAction
                                            if (act != null) {
                                                onAction(act)
                                            } else {
                                                onAction(
                                                    ActionPlayVideo(
                                                        url = item.stream.url,
                                                        title = item.stream.title,
                                                        durationSeconds = item.stream.durationSeconds,
                                                        playlist = currentSources.filter { it.isMapped }
                                                    )
                                                )
                                            }
                                            loadingTorrentUrl = null
                                            loadingSourceId = null
                                        }
                                    }
                                }


                                when (item) {
                                    is TorrentSourceUiItem -> TorrentSourceCard(
                                        item = item,
                                        isTv = isTv,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = onItemClick
                                    )
                                    is MovieSourceUiItem -> MovieSourceCard(
                                        item = item,
                                        isTv = isTv,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = onItemClick
                                    )
                                    is SeasonGroupSourceUiItem -> SeasonGroupSourceCard(
                                        item = item,
                                        isTv = isTv,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = onItemClick
                                    )
                                    is SingleEpisodeSourceUiItem -> SingleEpisodeSourceCard(
                                        item = item,
                                        isTv = isTv,
                                        isLoading = isThisLoading,
                                        isDisabled = isOtherLoading,
                                        onClick = onItemClick
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

// === ВНУТРЕННИЕ UI-ЭЛЕМЕНТЫ ===

@Composable
private fun SourceTab(
    text: String,
    count: Int,
    isSelected: Boolean,
    isTv: Boolean,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)
    )
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White

    Row(
        modifier = Modifier
            .tvAndWebHoverEffect(scaleTarget = 1.05f, shape = RoundedCornerShape(24.dp), onClick = { onClick() })
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .padding(horizontal = if (isTv) 20.dp else 16.dp, vertical = if (isTv) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (isTv) 18.sp else 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (isTv) 16.dp else 12.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else if (count > 0) {
            Box(
                modifier = Modifier.background(
                    if (isSelected) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(4.dp)
                ).padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = textColor,
                    fontSize = if (isTv) 14.sp else 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MovieSourceCard(
    item: MovieSourceUiItem,
    isTv: Boolean,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.4f else 1f
    val titleSize = if (isTv) 18.sp else 16.sp
    val subtitleSize = if (isTv) 14.sp else 13.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (!isDisabled && !isLoading) Modifier.tvAndWebHoverEffect(scaleTarget = 1.02f, shape = RoundedCornerShape(12.dp), onClick = { onClick() })
                else Modifier
            )
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .then(
                if (!isDisabled && !isLoading) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(if (isTv) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!item.durationFormatted.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Lucide.Clock,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(subtitleSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.durationFormatted,
                            color = Color(0xFF4CAF50),
                            fontSize = subtitleSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (!item.channel.isNullOrBlank()) {
                    Text(
                        text = "•  ${item.channel}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = subtitleSize,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            QualityPill(quality = item.quality, isTv = isTv)
        }
    }
}

@Composable
private fun SeasonGroupSourceCard(
    item: SeasonGroupSourceUiItem,
    isTv: Boolean,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.4f else 1f
    val titleSize = if (isTv) 18.sp else 16.sp
    val subtitleSize = if (isTv) 14.sp else 13.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (!isDisabled && !isLoading) Modifier.tvAndWebHoverEffect(scaleTarget = 1.02f, shape = RoundedCornerShape(12.dp), onClick = { onClick() })
                else Modifier
            )
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .then(
                if (!isDisabled && !isLoading) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(if (isTv) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Lucide.Tv,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(subtitleSize.value.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val episodesSummary = when {
                        item.episodesTotal != null && item.episodesTotal > 0 && item.episodesCount >= item.episodesTotal ->
                            stringResource(Res.string.details_sources_found_episodes_all, item.episodesCount)
                        item.episodesTotal != null && item.episodesTotal > 0 ->
                            stringResource(Res.string.details_sources_found_episodes_of, item.episodesCount, item.episodesTotal ?: 0)
                        else ->
                            stringResource(Res.string.details_sources_found_episodes, item.episodesCount)
                    }
                    Text(
                        text = episodesSummary,
                        color = Color(0xFF4CAF50),
                        fontSize = subtitleSize,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!item.channel.isNullOrBlank()) {
                    Text(
                        text = "•  ${item.channel}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = subtitleSize,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            QualityPill(quality = item.quality, isTv = isTv)
        }
    }
}

@Composable
private fun SingleEpisodeSourceCard(
    item: SingleEpisodeSourceUiItem,
    isTv: Boolean,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.4f else 1f
    val titleSize = if (isTv) 18.sp else 16.sp
    val subtitleSize = if (isTv) 14.sp else 13.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (!isDisabled && !isLoading) Modifier.tvAndWebHoverEffect(scaleTarget = 1.02f, shape = RoundedCornerShape(12.dp), onClick = { onClick() })
                else Modifier
            )
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .then(
                if (!isDisabled && !isLoading) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(if (isTv) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!item.channel.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Lucide.Tv,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(subtitleSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.channel,
                            color = Color(0xFF4CAF50),
                            fontSize = subtitleSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!item.durationFormatted.isNullOrBlank()) {
                    Text(
                        text = "•  ${item.durationFormatted}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = subtitleSize,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            QualityPill(quality = item.quality, isTv = isTv)
        }
    }
}

@Composable
private fun TorrentSourceCard(
    item: TorrentSourceUiItem,
    isTv: Boolean,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.4f else 1f
    val titleSize = if (isTv) 18.sp else 15.sp
    val statSize = if (isTv) 14.sp else 12.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (!isDisabled && !isLoading) Modifier.tvAndWebHoverEffect(
                    scaleTarget = 1.02f,
                    shape = RoundedCornerShape(12.dp)
                )
                else Modifier
            )
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .then(
                if (!isDisabled && !isLoading) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(if (isTv) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Размер
                if (item.sizeBytes != null && item.sizeBytes > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Lucide.HardDrive,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(statSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatBytes(item.sizeBytes),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = statSize
                        )
                    }
                }

                // Сиды (Раздающие - Зеленые)
                if (item.seeders != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Lucide.CircleArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(statSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.seeders}",
                            color = Color(0xFF4CAF50),
                            fontSize = statSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Личи (Качающие - Красные)
                if (item.leechers != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Lucide.CircleArrowDown,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(statSize.value.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.leechers}",
                            color = Color(0xFFF44336),
                            fontSize = statSize
                        )
                    }
                }
            }
        }

        if (!item.quality.isNullOrEmpty() || item.format != null || item.videoCodec != null || item.audioCodec != null || item.isHdr || item.sourceName.isNotBlank()) {
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!item.quality.isNullOrEmpty()) {
                    QualityPill(quality = item.quality, isTv = isTv)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.sourceName.isNotBlank()) MetadataBadge(item.sourceName, Color(0xFF2196F3), isTv = isTv)
                    if (item.isHdr) MetadataBadge("HDR", Color(0xFFE0A96D), isTv = isTv)
                    if (item.videoCodec != null) MetadataBadge(item.videoCodec, Color.White.copy(alpha = 0.6f), isTv = isTv)
                    if (item.audioCodec != null) MetadataBadge(item.audioCodec, Color.White.copy(alpha = 0.6f), isTv = isTv)
                    if (item.format != null) {
                        val isUnsupported = item.format == "AVI"
                        MetadataBadge(
                            item.format,
                            if (isUnsupported) Color(0xFFF44336) else Color.White.copy(alpha = 0.6f),
                            isTv = isTv
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataBadge(text: String, tint: Color, isTv: Boolean) {
    Box(
        modifier = Modifier
            .background(tint.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .border(1.dp, tint.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = if (isTv) 6.dp else 4.dp, vertical = if (isTv) 4.dp else 2.dp)
    ) {
        Text(text = text, color = tint, fontSize = if (isTv) 12.sp else 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QualityPill(quality: String, isTv: Boolean) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = if (isTv) 8.dp else 6.dp, vertical = if (isTv) 4.dp else 2.dp)
    ) {
        Text(text = quality, color = Color.White, fontSize = if (isTv) 14.sp else 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MappedEpisodeCard(source: MediaStream, isTv: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(scaleTarget = 1.02f, shape = RoundedCornerShape(12.dp), onClick = { onClick() })
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        Color(0xFF1A1A2E).copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val posterUrl = source.episodePosterUrl
        if (posterUrl != null && posterUrl.isNotBlank()) {
            ShimmerImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.Play,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            val epName = source.episodeName
            val primaryText = if (!epName.isNullOrBlank() && epName != source.title) {
                if (source.episodeNumber != null) "${source.episodeNumber}. $epName" else epName
            } else if (source.seasonNumber != null && source.episodeNumber != null) {
                stringResource(Res.string.player_episode_fmt, source.episodeNumber ?: 0)
            } else {
                source.title
            }

            Text(
                text = primaryText,
                color = Color.White,
                fontSize = if (isTv) 16.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (source.durationSeconds != null && source.durationSeconds!! > 0) {
                val mins = (source.durationSeconds!! / 60).toInt()
                Text(
                    text = stringResource(Res.string.player_duration_mins_single_fmt, mins),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = if (isTv) 14.sp else 12.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.details_sources_watch),
                color = MaterialTheme.colorScheme.primary,
                fontSize = if (isTv) 14.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SubFilterChip(
    label: String,
    count: Int?,
    isSelected: Boolean,
    isTv: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else Color.White.copy(alpha = 0.05f)
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else Color.White.copy(alpha = 0.12f)
    )
    val textColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else Color.White.copy(alpha = 0.75f)
    )

    Row(
        modifier = Modifier
            .tvAndWebHoverEffect(scaleTarget = 1.05f, shape = RoundedCornerShape(20.dp), onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (isTv) 13.sp else 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
        if (count != null && count > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$count",
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.9f),
                    fontSize = if (isTv) 11.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    val value = bytes / 1024.0.pow(exp.toDouble())

    return "${(value * 10).toInt() / 10.0} ${pre}B"
}