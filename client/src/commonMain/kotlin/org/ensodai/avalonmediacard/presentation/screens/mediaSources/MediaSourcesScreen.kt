package org.ensodai.avalonmediacard.presentation.screens.mediaSources

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionPlayVideo
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.overlay.TvModalSurface
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AdaptiveLayout
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.updateRawSlots
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.components.TorrentInspectorSection
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.targets.tv.MediaSourcesLayoutTv
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.targets.web.MediaSourcesLayoutWeb
import org.koin.compose.koinInject

@Composable
fun MediaSourcesScreen(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    mediaSourcesList: List<SduiSlot<SlotData.MediaSources>> = emptyList(),
    torrentInspectorState: SlotUiState<SlotData.TorrentInspector>? = null,
    mediaKey: MediaKey? = null,
    callerFocusRequester: FocusRequester? = null,
    onClose: () -> Unit,
    onSelectSource: ((providerId: String, sourceId: String, seasonNumber: Int?, episodeNumber: Int?, onComplete: () -> Unit) -> Unit)? = null,
    onRefreshSources: (() -> Unit)? = null,
    onPlayVideo: ((ActionPlayVideo) -> Unit)? = null,
    onAction: (Action) -> Unit = {},
    viewModel: MediaSourcesViewModel = koinInject()
) {
    val deviceTarget = LocalDeviceTarget.current
    val isTv = deviceTarget.isTv

    val coroutineScope = rememberCoroutineScope()
    val filePicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("torrent"))
    ) { file ->
        file?.let {
            coroutineScope.launch {
                try {
                    val bytes = it.readBytes()
                    viewModel.actions.onTorrentFileSelected(it.name, bytes)
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(viewModel, onClose, onSelectSource, onPlayVideo, onAction, onRefreshSources) {
        viewModel.onCloseRequested = onClose
        viewModel.onSelectSource = onSelectSource
        viewModel.onPlayVideo = onPlayVideo
        viewModel.onAction = onAction
        viewModel.onRefreshSources = onRefreshSources
        viewModel.onPickFileRequested = { filePicker.launch() }
    }

    LaunchedEffect(mediaSourcesList, mediaKey) {
        viewModel.updateRawSlots(mediaSourcesList, mediaKey)
    }

    val state by viewModel.viewState.collectAsState()
    val actions = viewModel.actions

    TvModalSurface(
        isOpen = isExpanded,
        onDismissRequest = actions.onClose,
        callerFocusRequester = callerFocusRequester,
        scrimColor = if (isTv) MaterialTheme.colorScheme.background else Color.Black.copy(alpha = 0.9f),
        modifier = if (isTv) Modifier.fillMaxSize() else Modifier
    ) {
        val inspectorData = torrentInspectorState?.data
        when {
            torrentInspectorState?.hasError == true && torrentInspectorState.error != null -> {
                SlotErrorCard(
                    message = torrentInspectorState.error,
                    retryAction = torrentInspectorState.retryAction,
                    onAction = onAction,
                    modifier = Modifier
                )
            }
            inspectorData != null -> {
                TorrentInspectorSection(
                    component = inspectorData,
                    onAction = onAction,
                    isExpanded = true,
                    onCloseSources = actions.onClose
                )
            }
            else -> {
                AdaptiveLayout(
                    tv = {
                        MediaSourcesLayoutTv(
                            state = state,
                            actions = actions,
                            modifier = modifier
                        )
                    },
                    default = {
                        MediaSourcesLayoutWeb(
                            state = state,
                            actions = actions,
                            modifier = modifier
                        )
                    }
                )
            }
        }
    }
}
