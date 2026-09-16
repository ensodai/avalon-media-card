package org.ensodai.avalonmediacard.presentation.screens.mediaSources.viewState

import androidx.compose.runtime.Immutable
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewState
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.MediaSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SourceSubFilter

@Immutable
data class MediaSourcesViewState(
    val mediaKey: MediaKey? = null,
    val visibleSlots: List<SduiSlot<SlotData.MediaSources>> = emptyList(),
    val selectedTabIndex: Int = 0,
    val currentSlotData: SlotData.MediaSources? = null,
    val currentSources: List<MediaStream> = emptyList(),
    val subFilters: List<SourceSubFilter> = emptyList(),
    val selectedSubFilter: SourceSubFilter? = null,
    val displayItems: List<MediaSourceUiItem> = emptyList(),
    val isCurrentLoading: Boolean = false,
    val loadingSourceId: String? = null,
    val loadingTorrentUrl: String? = null,
    val isAddTorrentMode: Boolean = false,
    val isUploadingFile: Boolean = false,
    val uploadingFileName: String? = null,
    val uploadErrorMessage: String? = null
) : BaseViewState()
