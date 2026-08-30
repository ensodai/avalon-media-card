package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard

@Composable
fun MediaSourcesSlot(
    isExpanded: Boolean,
    mediaSourcesList: List<SduiSlot<SlotData.MediaSources>> = emptyList(),
    torrentInspectorState: SlotUiState<SlotData.TorrentInspector>?,
    onClose: () -> Unit,
    onSelectSource: ((providerId: String, sourceId: String, seasonNumber: Int?, episodeNumber: Int?, onComplete: () -> Unit) -> Unit)? = null,
    onRefreshSources: (() -> Unit)? = null,
    onAction: (Action) -> Unit
) {

    if (!isExpanded) return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
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
}
