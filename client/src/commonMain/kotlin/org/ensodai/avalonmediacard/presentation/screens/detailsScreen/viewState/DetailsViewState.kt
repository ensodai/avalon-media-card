package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.viewState

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.SduiViewState

data class DetailsViewState(
    val mediaKey: MediaKey,
    override val loadingActions: Set<ServerAction> = emptySet(),
    val header: SduiSlot<SlotData.Header>? = null,
    val playButtons: SduiSlot<SlotData.ButtonGroup>? = null,
    val collectionButtons: SduiSlot<SlotData.ButtonGroup>? = null,
    val continueWatching: SduiSlot<SlotData.ContinueWatching>? = null,
    val userActions: SduiSlot<SlotData.UserActions>? = null,
    val syncStatus: SduiSlot<SlotData.SyncStatus>? = null,
    val description: SduiSlot<SlotData.Text>? = null,
    val tvSeasons: SduiSlot<SlotData.TvSeasons>? = null,
    val mediaSourcesList: List<SduiSlot<SlotData.MediaSources>> = emptyList(),
    val torrentInspector: SduiSlot<SlotData.TorrentInspector>? = null,
    val cast: SduiSlot<SlotData.Cast>? = null,
    val carousels: List<SduiSlot<SlotData.Carousel>> = emptyList(),
    val comments: SduiSlot<SlotData.Comments>? = null,
    val isSourcesExpanded: Boolean = false,
    val playerState: PlayerState = PlayerState.Idle
) : SduiViewState {

    sealed interface PlayerState {
        object Idle : PlayerState
        data class Preparing(
            val title: String, 
            val hasEpisodes: Boolean,
            val playlist: List<MediaStream> = emptyList(),
            val targetSeason: Int? = null,
            val targetEpisode: Int? = null
        ) : PlayerState
        data class Playing(
            val streamUrl: String,
            val title: String,
            val streamId: String? = null,
            val duration: Double? = null,
            val startPositionSeconds: Long? = null,
            val playlist: List<MediaStream> = emptyList(),
            val audioTracks: List<org.ensodai.avalonmediacard.contract.plugins.AudioTrack> = emptyList(),
            val subtitleTracks: List<org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack> = emptyList(),
            val audioTrackIndex: Int? = null
        ) : PlayerState

        data class Error(val message: String) : PlayerState
    }


}
