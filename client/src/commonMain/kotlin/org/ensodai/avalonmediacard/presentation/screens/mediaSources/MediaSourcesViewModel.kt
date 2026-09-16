package org.ensodai.avalonmediacard.presentation.screens.mediaSources

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionPlayVideo
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.playback.SearchMediaSourcesUseCase
import org.ensodai.avalonmediacard.domain.useCases.playback.SelectMediaSourceUseCase
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewModel
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.MediaSourcesActions
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.onItemClick
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.onRefresh
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.onSelectSubFilter
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.onSelectTab
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.onToggleAddTorrent
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.action.onUploadTorrentFile
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.viewState.MediaSourcesViewState
import org.koin.core.annotation.Factory

@Factory
class MediaSourcesViewModel(
    internal val searchMediaSourcesUseCase: SearchMediaSourcesUseCase,
    internal val selectMediaSourceUseCase: SelectMediaSourceUseCase,
    internal val executeServerActionUseCase: ExecuteServerActionUseCase
) : BaseViewModel<MediaSourcesViewState, MediaSourcesActions>(
    initialState = MediaSourcesViewState()
) {
    var onPlayVideo: ((ActionPlayVideo) -> Unit)? = null
    var onSelectSource: ((providerId: String, sourceId: String, seasonNumber: Int?, episodeNumber: Int?, onComplete: () -> Unit) -> Unit)? = null
    var onAction: ((Action) -> Unit)? = null
    var onRefreshSources: (() -> Unit)? = null
    var onCloseRequested: (() -> Unit)? = null
    var onPickFileRequested: (() -> Unit)? = null

    override val actions = MediaSourcesActions(
        onSelectTab = ::onSelectTab,
        onSelectSubFilter = ::onSelectSubFilter,
        onItemClick = ::onItemClick,
        onToggleAddTorrent = ::onToggleAddTorrent,
        onPickTorrentFile = { onPickFileRequested?.invoke() },
        onTorrentFileSelected = ::onUploadTorrentFile,
        onRefresh = ::onRefresh,
        onClose = { onCloseRequested?.invoke() }
    )
}
