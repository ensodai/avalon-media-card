package org.ensodai.avalonmediacard.presentation.screens.watchParty

import kotlinx.coroutines.Job
import org.ensodai.avalonmediacard.data.TokenStorage
import org.ensodai.avalonmediacard.domain.useCases.playback.SearchMediaSourcesUseCase
import org.ensodai.avalonmediacard.domain.useCases.playback.SelectMediaSourceUseCase
import org.ensodai.avalonmediacard.domain.useCases.watchparty.CloseWatchRoomUseCase
import org.ensodai.avalonmediacard.domain.useCases.watchparty.CreateWatchRoomUseCase
import org.ensodai.avalonmediacard.domain.useCases.watchparty.GetSavedWatchRoomsUseCase
import org.ensodai.avalonmediacard.domain.useCases.watchparty.JoinWatchRoomByIdUseCase
import org.ensodai.avalonmediacard.domain.useCases.watchparty.JoinWatchRoomByPinUseCase
import org.ensodai.avalonmediacard.domain.useCases.watchparty.LeaveWatchRoomUseCase
import org.ensodai.avalonmediacard.domain.useCases.watchparty.SetLobbyStatusUseCase
import org.ensodai.avalonmediacard.domain.useCases.watchparty.StreamLobbyStateUseCase
import org.ensodai.avalonmediacard.domain.useCases.watchparty.TriggerStartPlaybackUseCase
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewModel
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onControlModeChanged
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onCreateRoom
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onJoinById
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onJoinByPin
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onLeaveRoom
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onPinChanged
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onRoomTitleChanged
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onSeasonEpisodeChanged
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onSelectSource
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onSetMyIntent
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onSetStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onStartPlayback
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onTestSource
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onTestSourceCancel
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onTestSourceVerified
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onToggleReady
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.onToggleSelectSource
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory
class WatchPartyViewModel(
    internal val createWatchRoomUseCase: CreateWatchRoomUseCase,
    internal val joinWatchRoomByPinUseCase: JoinWatchRoomByPinUseCase,
    internal val joinWatchRoomByIdUseCase: JoinWatchRoomByIdUseCase,
    internal val getSavedWatchRoomsUseCase: GetSavedWatchRoomsUseCase,
    internal val leaveWatchRoomUseCase: LeaveWatchRoomUseCase,
    internal val closeWatchRoomUseCase: CloseWatchRoomUseCase,
    internal val streamLobbyStateUseCase: StreamLobbyStateUseCase,
    internal val setLobbyStatusUseCase: SetLobbyStatusUseCase,
    internal val triggerStartPlaybackUseCase: TriggerStartPlaybackUseCase,
    internal val searchMediaSourcesUseCase: SearchMediaSourcesUseCase,
    internal val selectMediaSourceUseCase: SelectMediaSourceUseCase,
    internal val tokenStorage: TokenStorage
) : BaseViewModel<WatchPartyViewState, WatchPartyActions>(
    initialState = WatchPartyViewState()
) {
    var onCloseRequested: (() -> Unit)? = null
    var onLaunchPlayerRequested: ((roomId: Uuid, season: Int?, episode: Int?, startPositionSeconds: Long) -> Unit)? = null

    internal var eventStreamJob: Job? = null
    internal var savedRoomsStreamJob: Job? = null

    override val actions = WatchPartyActions(
        onSetStep = ::onSetStep,
        onPinChanged = ::onPinChanged,
        onJoinByPin = ::onJoinByPin,
        onJoinById = ::onJoinById,
        onRoomTitleChanged = ::onRoomTitleChanged,
        onControlModeChanged = ::onControlModeChanged,
        onSeasonEpisodeChanged = ::onSeasonEpisodeChanged,
        onSelectSource = ::onSelectSource,
        onToggleSelectSource = ::onToggleSelectSource,
        onTestSource = ::onTestSource,
        onTestSourceVerified = ::onTestSourceVerified,
        onTestSourceCancel = ::onTestSourceCancel,
        onCreateRoom = ::onCreateRoom,
        onSetMyIntent = ::onSetMyIntent,
        onToggleReady = ::onToggleReady,
        onStartPlayback = ::onStartPlayback,
        onLeaveRoom = ::onLeaveRoom,
        onClose = {
            eventStreamJob?.cancel()
            eventStreamJob = null
            savedRoomsStreamJob?.cancel()
            savedRoomsStreamJob = null
            onCloseRequested?.invoke()
        }
    )

    override fun onCleared() {
        super.onCleared()
        eventStreamJob?.cancel()
        savedRoomsStreamJob?.cancel()
    }
}
