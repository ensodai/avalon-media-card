package org.ensodai.avalonmediacard.presentation.screens.watchParty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.overlay.TvModalSurface
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.MediaSourcesViewModel
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.updateRawSlots
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.components.TorrentInspectorSection
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.MovieSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SeasonGroupSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SingleEpisodeSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.TorrentSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.targets.web.MediaSourcesLayoutWeb
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerScreen
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerInitParams
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerMode
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.initialize
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onToggleSelectSource
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.WatchPartyLayoutWeb
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

@Composable
fun WatchPartyScreen(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    mediaKey: MediaKey? = null,
    mediaTitle: String? = null,
    mediaSourcesList: List<SduiSlot<SlotData.MediaSources>> = emptyList(),
    torrentInspectorState: SlotUiState<SlotData.TorrentInspector>? = null,
    onRefreshSources: (() -> Unit)? = null,
    onAction: (Action) -> Unit = {},
    initialStep: WatchPartyStep? = null,
    onClose: () -> Unit,
    onStartPlayback: ((roomId: Uuid, season: Int?, episode: Int?, startPositionSeconds: Long) -> Unit)? = null,
    viewModel: WatchPartyViewModel = koinInject(),
    mediaSourcesViewModel: MediaSourcesViewModel = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val filePicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("torrent"))
    ) { file ->
        file?.let {
            coroutineScope.launch {
                try {
                    val bytes = it.readBytes()
                    mediaSourcesViewModel.actions.onTorrentFileSelected(it.name, bytes)
                } catch (_: Exception) {
                }
            }
        }
    }

    val state by viewModel.viewState.collectAsState()
    val actions = viewModel.actions

    val effectiveMediaKey = state.mediaKey ?: mediaKey
    val effectiveMediaSourcesList = if (mediaSourcesList.isNotEmpty()) mediaSourcesList else state.mediaSourcesList
    val effectiveInspectorState = torrentInspectorState ?: state.torrentInspectorSlot?.state

    DisposableEffect(viewModel, onClose, onStartPlayback) {
        viewModel.onCloseRequested = onClose
        viewModel.onLaunchPlayerRequested = onStartPlayback
        onDispose {
            viewModel.onCloseRequested = null
            viewModel.onLaunchPlayerRequested = null
            viewModel.eventStreamJob?.cancel()
            viewModel.eventStreamJob = null
            viewModel.sourcesStreamJob?.cancel()
            viewModel.sourcesStreamJob = null
        }
    }

    LaunchedEffect(isVisible, mediaKey, mediaTitle, initialStep) {
        if (isVisible) {
            viewModel.initialize(mediaKey, mediaTitle, initialStep)
        }
    }

    LaunchedEffect(mediaSourcesViewModel, effectiveMediaSourcesList, effectiveMediaKey) {
        mediaSourcesViewModel.updateRawSlots(effectiveMediaSourcesList, effectiveMediaKey)
    }

    LaunchedEffect(mediaSourcesViewModel, onRefreshSources, onAction) {
        mediaSourcesViewModel.onAction = { action ->
            onAction(action)
            if (action is ServerAction) {
                actions.onExecuteServerAction(action)
            }
        }
        mediaSourcesViewModel.onRefreshSources = {
            if (onRefreshSources != null) {
                onRefreshSources.invoke()
            } else {
                actions.onRefreshSources()
            }
        }
        mediaSourcesViewModel.onPickFileRequested = { filePicker.launch() }
        mediaSourcesViewModel.onCloseRequested = { actions.onToggleSelectSource(false) }
        mediaSourcesViewModel.onSelectSourceItem = { item, providerId, sourceId, season, episode, onComplete ->
            val sourceDetails = when (item) {
                is TorrentSourceUiItem -> {
                    val parts = mutableListOf<String>()
                    item.quality?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    if (item.isHdr) parts.add("HDR")
                    item.sizeBytes?.let { bytes ->
                        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                        if (gb >= 1.0) {
                            val rounded = (gb * 10).toLong() / 10.0
                            parts.add("${rounded} GB")
                        } else {
                            val mb = bytes / (1024 * 1024)
                            parts.add("${mb} MB")
                        }
                    }
                    if (parts.isEmpty()) item.sourceName.ifBlank { item.title }
                    else parts.joinToString(" • ")
                }
                is MovieSourceUiItem -> {
                    val parts = mutableListOf<String>()
                    item.quality.takeIf { it.isNotBlank() && !it.equals("Auto", ignoreCase = true) }?.let { parts.add(it) }
                    val cleanChannel = item.channel?.takeIf { ch ->
                        ch.isNotBlank() && !ch.contains("VK Video", ignoreCase = true) && !ch.contains(item.quality, ignoreCase = true)
                    }
                    cleanChannel?.let { parts.add(it) }
                    if (parts.isEmpty()) item.quality.ifBlank { item.title }
                    else parts.joinToString(" • ")
                }
                is SeasonGroupSourceUiItem -> {
                    val parts = mutableListOf<String>()
                    parts.add("S${item.seasonNumber}")
                    item.quality.takeIf { it.isNotBlank() && !it.equals("Auto", ignoreCase = true) }?.let { parts.add(it) }
                    parts.joinToString(" • ")
                }
                is SingleEpisodeSourceUiItem -> {
                    val parts = mutableListOf<String>()
                    if (item.seasonNumber != null && item.episodeNumber != null) {
                        parts.add("S${item.seasonNumber}E${item.episodeNumber}")
                    }
                    item.quality.takeIf { it.isNotBlank() && !it.equals("Auto", ignoreCase = true) }?.let { parts.add(it) }
                    val cleanChannel = item.channel?.takeIf { ch ->
                        ch.isNotBlank() && !ch.contains("VK Video", ignoreCase = true) && !ch.contains(item.quality, ignoreCase = true)
                    }
                    cleanChannel?.let { parts.add(it) }
                    if (parts.isEmpty()) item.quality.ifBlank { item.title }
                    else parts.joinToString(" • ")
                }
            }
            actions.onSelectSource(providerId, sourceId, sourceDetails, season, episode)
            onComplete()
        }
    }

    LaunchedEffect(state.isSelectingSource, effectiveMediaSourcesList) {
        if (state.isSelectingSource) {
            val hasSources = effectiveMediaSourcesList.any { it.state.data?.sources?.isNotEmpty() == true }
            val isSearching = effectiveMediaSourcesList.any { it.state.isLoading || it.state.isInitialLoading }
            if (!hasSources && !isSearching) {
                onRefreshSources?.invoke() ?: actions.onRefreshSources()
            }
        }
    }

    val mediaSourcesState by mediaSourcesViewModel.viewState.collectAsState()
    val inspectorData = effectiveInspectorState?.data

    val sourcesContent: @Composable () -> Unit = {
        when {
            effectiveInspectorState?.hasError == true && effectiveInspectorState.error != null -> {
                SlotErrorCard(
                    message = effectiveInspectorState.error,
                    retryAction = effectiveInspectorState.retryAction,
                    onAction = { action ->
                        onAction(action)
                        if (action is ServerAction) {
                            actions.onExecuteServerAction(action)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            inspectorData != null -> {
                TorrentInspectorSection(
                    component = inspectorData,
                    onAction = { action ->
                        onAction(action)
                        if (action is ServerAction) {
                            actions.onExecuteServerAction(action)
                        }
                    },
                    isExpanded = true,
                    onCloseSources = { actions.onToggleSelectSource(false) }
                )
            }
            else -> {
                MediaSourcesLayoutWeb(
                    state = mediaSourcesState,
                    actions = mediaSourcesViewModel.actions,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    TvModalSurface(
        modifier = modifier,
        isOpen = isVisible,
        onDismissRequest = {
            if (state.isTestingSource) {
                actions.onTestSourceCancel()
            } else {
                actions.onClose()
            }
        },
        scrimColor = Color.Black.copy(alpha = 0.85f),
    ) {
        if (state.isTestingSource && effectiveMediaKey != null) {
            PlayerScreen(
                params = PlayerInitParams(
                    title = state.selectedSourceName ?: state.mediaTitle,
                    seriesTitle = state.mediaTitle,
                    mediaKey = effectiveMediaKey,
                    targetSeason = state.selectedSeason,
                    targetEpisode = state.selectedEpisode,
                    mode = PlayerMode.TEST_PREVIEW,
                    sourceType = state.selectedSourceType,
                    sourceId = state.selectedSourceId
                ),
                onClose = {
                    actions.onTestSourceCancel()
                },
                onConfirmSource = {
                    actions.onTestSourceVerified(true)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            WatchPartyLayoutWeb(
                state = state,
                actions = actions,
                sourcesContent = sourcesContent
            )
        }
    }
}
