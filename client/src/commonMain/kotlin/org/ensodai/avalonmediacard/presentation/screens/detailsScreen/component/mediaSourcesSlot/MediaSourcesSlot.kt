package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.MediaSourcesScreen

@Composable
fun MediaSourcesSlot(
    isExpanded: Boolean,
    mediaSourcesList: List<SduiSlot<SlotData.MediaSources>> = emptyList(),
    torrentInspectorState: SlotUiState<SlotData.TorrentInspector>? = null,
    callerFocusRequester: FocusRequester? = null,
    onClose: () -> Unit,
    onSelectSource: ((providerId: String, sourceId: String, seasonNumber: Int?, episodeNumber: Int?, onComplete: () -> Unit) -> Unit)? = null,
    onRefreshSources: (() -> Unit)? = null,
    onAction: (Action) -> Unit = {},
    modifier: Modifier = Modifier
) {
    MediaSourcesScreen(
        isExpanded = isExpanded,
        mediaSourcesList = mediaSourcesList,
        torrentInspectorState = torrentInspectorState,
        callerFocusRequester = callerFocusRequester,
        onClose = onClose,
        onSelectSource = onSelectSource,
        onRefreshSources = onRefreshSources,
        onAction = onAction,
        modifier = modifier
    )
}
