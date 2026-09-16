package org.ensodai.avalonmediacard.presentation.screens.mediaSources.action

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.slot.ActionPlayVideo
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.UploadCustomTorrentCommand
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.MediaSourcesViewModel
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.MediaSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SeasonGroupSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SingleEpisodeSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SourceSubFilter
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.TorrentSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.toSourceUiItem

fun MediaSourcesViewModel.updateRawSlots(
    slots: List<SduiSlot<SlotData.MediaSources>>,
    key: MediaKey? = null
) {
    val nonEmptiesOrLoading = slots.filter {
        val isSlotLoading = it.state.isLoading || it.state.isInitialLoading
        val hasSources = (it.state.data?.sources?.isNotEmpty() == true)
        isSlotLoading || hasSources
    }
    val visibleSlots = if (nonEmptiesOrLoading.isNotEmpty()) nonEmptiesOrLoading else slots
    val safeTabIndex = viewState.value.selectedTabIndex.coerceIn(0, (visibleSlots.size - 1).coerceAtLeast(0))
    val currentSlot = visibleSlots.getOrNull(safeTabIndex)
    val currentData = currentSlot?.state?.data
    val currentSources = currentData?.sources.orEmpty()
    val isCurrentLoading = currentSlot?.state?.isLoading == true || currentSlot?.state?.isInitialLoading == true

    val effectiveKey = key ?: viewState.value.mediaKey ?: currentData?.mediaKey
    val isTvShow = effectiveKey?.type == EntityType.TV
    val uiItems = currentSources.map { it.toSourceUiItem(isTvShow) }
    val isTorrents = uiItems.any { it is TorrentSourceUiItem }

    val baseItems = if (isTorrents) {
        uiItems.distinctBy { (it as? TorrentSourceUiItem)?.stream?.url?.substringBefore("&index=") ?: it.id }
    } else {
        uiItems.distinctBy { it.id }
    }

    val serverSubFilters = currentData?.subFilters?.map { SourceSubFilter(it.id, it.label, it.count) }.orEmpty()
    val subFilters = if (serverSubFilters.isNotEmpty()) {
        serverSubFilters
    } else {
        buildList {
            val seasonNumbers = currentSources.mapNotNull { it.seasonNumber }.distinct().sorted()
            if (seasonNumbers.size > 1) {
                seasonNumbers.forEach { sNum ->
                    val count = currentSources.count { it.seasonNumber == sNum }
                    add(SourceSubFilter("season_$sNum", "Сезон $sNum", count))
                }
            }
            if (isTorrents) {
                val trackers = currentSources.map { it.sourceName }.filter { it.isNotBlank() }.distinct().sorted()
                if (trackers.size > 1) {
                    trackers.forEach { tracker ->
                        val count = currentSources.count { it.sourceName.equals(tracker, ignoreCase = true) }
                        add(SourceSubFilter("tracker_${tracker.lowercase()}", tracker, count))
                    }
                }
            }
        }
    }

    val currentFilter = viewState.value.selectedSubFilter
    val filteredItems = applyFilter(baseItems, currentFilter)

    updateViewState {
        it.copy(
            mediaKey = effectiveKey,
            visibleSlots = visibleSlots,
            selectedTabIndex = safeTabIndex,
            currentSlotData = currentData,
            currentSources = currentSources,
            subFilters = subFilters,
            displayItems = filteredItems,
            isCurrentLoading = isCurrentLoading
        )
    }
}

fun MediaSourcesViewModel.onSelectTab(index: Int) {
    updateViewState { it.copy(selectedTabIndex = index, selectedSubFilter = null) }
    updateRawSlots(viewState.value.visibleSlots)
}

fun MediaSourcesViewModel.onSelectSubFilter(filter: SourceSubFilter?) {
    updateViewState { it.copy(selectedSubFilter = filter) }
    updateRawSlots(viewState.value.visibleSlots)
}

fun MediaSourcesViewModel.onItemClick(item: MediaSourceUiItem) {
    val state = viewState.value
    val isThisLoading = state.loadingSourceId == item.id || state.loadingTorrentUrl == item.stream.url
    val isOtherLoading = (state.loadingSourceId != null && state.loadingSourceId != item.id) ||
            (state.loadingTorrentUrl != null && state.loadingTorrentUrl != item.stream.url)

    if (isThisLoading || isOtherLoading) return

    if (item is TorrentSourceUiItem) {
        updateViewState { it.copy(loadingTorrentUrl = item.stream.url) }
    } else {
        updateViewState { it.copy(loadingSourceId = item.id) }
    }

    val pId = state.currentSlotData?.providerId ?: item.stream.sourceName
    val sId = item.id.ifBlank { item.stream.canonicalId.ifBlank { item.stream.url } }

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

    val selectSourceCallback = onSelectSource
    if (selectSourceCallback != null) {
        selectSourceCallback(pId, sId, targetSeason, targetEpisode) {
            updateViewState { it.copy(loadingSourceId = null, loadingTorrentUrl = null) }
        }
    } else {
        val clickAction = item.clickAction
        if (clickAction != null) {
            onAction?.invoke(clickAction)
            updateViewState { it.copy(loadingSourceId = null, loadingTorrentUrl = null) }
        } else {
            val key = state.mediaKey ?: state.currentSlotData?.mediaKey
            if (key != null) {
                viewModelScope.launch {
                    try {
                        val result = selectMediaSourceUseCase(key, pId, sId, targetSeason, targetEpisode)
                        when (result) {
                            is SourceSelectionResult.Ready -> {
                                onCloseRequested?.invoke()
                                onPlayVideo?.invoke(
                                    ActionPlayVideo(
                                        url = item.stream.url,
                                        title = item.stream.title,
                                        durationSeconds = item.stream.durationSeconds,
                                        playlist = state.currentSources.filter { it.isMapped }
                                    )
                                )
                            }
                            else -> Unit
                        }
                    } finally {
                        updateViewState { it.copy(loadingSourceId = null, loadingTorrentUrl = null) }
                    }
                }
            } else {
                onPlayVideo?.invoke(
                    ActionPlayVideo(
                        url = item.stream.url,
                        title = item.stream.title,
                        durationSeconds = item.stream.durationSeconds,
                        playlist = state.currentSources.filter { it.isMapped }
                    )
                )
                updateViewState { it.copy(loadingSourceId = null, loadingTorrentUrl = null) }
            }
        }
    }
}

fun MediaSourcesViewModel.onToggleAddTorrent() {
    updateViewState {
        it.copy(
            isAddTorrentMode = !it.isAddTorrentMode,
            isUploadingFile = false,
            uploadErrorMessage = null,
            uploadingFileName = null
        )
    }
}

fun MediaSourcesViewModel.onUploadTorrentFile(fileName: String, bytes: ByteArray) {
    val key = viewState.value.mediaKey ?: viewState.value.currentSlotData?.mediaKey
    if (key == null) {
        updateViewState { it.copy(uploadErrorMessage = "Media key not found") }
        return
    }

    updateViewState {
        it.copy(
            isUploadingFile = true,
            uploadingFileName = fileName,
            uploadErrorMessage = null
        )
    }

    viewModelScope.launch {
        try {
            executeServerActionUseCase(UploadCustomTorrentCommand(key, fileName, bytes))
            updateViewState { it.copy(isUploadingFile = false) }
        } catch (e: Exception) {
            updateViewState {
                it.copy(
                    isUploadingFile = false,
                    uploadErrorMessage = e.message ?: "File upload error"
                )
            }
        }
    }
}

fun MediaSourcesViewModel.onRefresh() {
    val refreshCallback = onRefreshSources
    if (refreshCallback != null) {
        refreshCallback()
    } else {
        val key = viewState.value.mediaKey ?: viewState.value.currentSlotData?.mediaKey ?: return
        viewModelScope.launch {
            searchMediaSourcesUseCase(key, forceRefresh = true)
        }
    }
}

private fun applyFilter(
    items: List<MediaSourceUiItem>,
    filter: SourceSubFilter?
): List<MediaSourceUiItem> {
    if (filter == null) return items
    return items.filter { item ->
        val stream = item.stream
        stream.subFilterId == filter.id ||
                (stream.seasonNumber != null && "season_${stream.seasonNumber}" == filter.id) ||
                (stream.sourceName.isNotBlank() && "tracker_${stream.sourceName.lowercase()}" == filter.id)
    }
}
