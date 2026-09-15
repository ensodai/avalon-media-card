package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.overlay.TvModalSurface
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard

@Composable
fun MediaSourcesSlot(
    isExpanded: Boolean,
    mediaSourcesList: List<SduiSlot<SlotData.MediaSources>> = emptyList(),
    torrentInspectorState: SlotUiState<SlotData.TorrentInspector>?,
    callerFocusRequester: FocusRequester? = null,
    onClose: () -> Unit,
    onSelectSource: ((providerId: String, sourceId: String, seasonNumber: Int?, episodeNumber: Int?, onComplete: () -> Unit) -> Unit)? = null,
    onRefreshSources: (() -> Unit)? = null,
    onAction: (Action) -> Unit
) {
    TvModalSurface(
        isOpen = isExpanded,
        onDismissRequest = onClose,
        callerFocusRequester = callerFocusRequester,
        scrimColor = Color.Black.copy(alpha = 0.9f)
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
                    onCloseSources = onClose
                )
            }

            else -> {
                MediaSourcesSection(
                    mediaSourcesList = mediaSourcesList,
                    onAction = onAction,
                    isExpanded = true,
                    onCloseSources = onClose,
                    onSelectSource = onSelectSource,
                    onRefreshSources = onRefreshSources
                )
            }
        }
    }
}
