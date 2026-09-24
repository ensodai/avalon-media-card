package org.ensodai.avalonmediacard.presentation.screens.watchParty.action

import androidx.lifecycle.viewModelScope
import avalonmediacard.client.generated.resources.*
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.presentation.screens.watchParty.WatchPartyViewModel
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.jetbrains.compose.resources.getString
import kotlin.uuid.Uuid

fun WatchPartyViewModel.initialize(
    mediaKey: MediaKey?,
    title: String?,
    initialStep: WatchPartyStep? = null
) {
    val currentUid = tokenStorage.cachedUserId?.let { runCatching { Uuid.parse(it) }.getOrNull() }
    val step = initialStep ?: if (mediaKey != null) WatchPartyStep.SETUP else WatchPartyStep.ENTRY
    updateViewState {
        it.copy(
            step = step,
            mediaKey = mediaKey,
            mediaTitle = title ?: it.mediaTitle,
            myUserId = currentUid
        )
    }
    if (mediaKey != null) {
        loadSavedRooms(mediaKey.id)
        searchSources(mediaKey)
    }
}

fun WatchPartyViewModel.loadSavedRooms(mediaId: String) {
    viewModelScope.launch {
        updateViewState { it.copy(isLoadingSavedRooms = true) }
        try {
            val rooms = getSavedWatchRoomsUseCase(mediaId)
            updateViewState { it.copy(savedRooms = rooms, isLoadingSavedRooms = false) }
        } catch (_: Exception) {
            updateViewState { it.copy(isLoadingSavedRooms = false) }
        }
    }
}

fun WatchPartyViewModel.searchSources(key: MediaKey) {
    viewModelScope.launch {
        try {
            searchMediaSourcesUseCase(key)
        } catch (_: Exception) {
        }
    }
}

fun WatchPartyViewModel.onSetStep(step: WatchPartyStep) {
    updateViewState { it.copy(step = step, joinError = null, createError = null) }
}

fun WatchPartyViewModel.onPinChanged(pin: String) {
    updateViewState { it.copy(pinInput = pin.take(6).uppercase(), joinError = null) }
}

fun WatchPartyViewModel.onJoinByPin() {
    val pin = viewState.value.pinInput.trim()
    viewModelScope.launch {
        if (pin.length != 6) {
            val errorMsg = getString(Res.string.watch_party_error_invalid_pin)
            updateViewState { it.copy(joinError = errorMsg) }
            return@launch
        }

        updateViewState { it.copy(isJoining = true, joinError = null) }
        when (val result = joinWatchRoomByPinUseCase(pin)) {
            is JoinRoomResult.Success -> {
                val room = result.room
                val myId = viewState.value.myUserId
                val isHost = room.hostUserId == myId
                val mediaType = if (room.mediaType == MediaType.TV) EntityType.TV else EntityType.MOVIE
                val resolvedKey = viewState.value.mediaKey ?: MediaKey(provider = MediaProvider.Tmdb, type = mediaType, id = room.mediaId)
                updateViewState {
                    it.copy(
                        activeRoom = room,
                        mediaKey = resolvedKey,
                        mediaTitle = if (it.mediaTitle.isNotBlank()) it.mediaTitle else room.title,
                        selectedSeason = room.currentSeason ?: it.selectedSeason,
                        selectedEpisode = room.currentEpisode ?: it.selectedEpisode,
                        selectedSourceType = room.sourceType ?: it.selectedSourceType,
                        selectedSourceId = room.sourceId ?: it.selectedSourceId,
                        participantsMap = room.participants.associateBy { p -> p.userId },
                        isHost = isHost,
                        step = WatchPartyStep.LOBBY,
                        isJoining = false
                    )
                }
                subscribeToLobbyState(room.id)
            }
            is JoinRoomResult.Error -> {
                updateViewState { it.copy(joinError = result.message, isJoining = false) }
            }
        }
    }
}

fun WatchPartyViewModel.onJoinById(roomId: Uuid) {
    viewModelScope.launch {
        updateViewState { it.copy(isJoining = true, joinError = null) }
        when (val result = joinWatchRoomByIdUseCase(roomId)) {
            is JoinRoomResult.Success -> {
                val room = result.room
                val myId = viewState.value.myUserId
                val isHost = room.hostUserId == myId
                val mediaType = if (room.mediaType == MediaType.TV) EntityType.TV else EntityType.MOVIE
                val resolvedKey = viewState.value.mediaKey ?: MediaKey(provider = MediaProvider.Tmdb, type = mediaType, id = room.mediaId)
                updateViewState {
                    it.copy(
                        activeRoom = room,
                        mediaKey = resolvedKey,
                        mediaTitle = if (it.mediaTitle.isNotBlank()) it.mediaTitle else room.title,
                        selectedSeason = room.currentSeason ?: it.selectedSeason,
                        selectedEpisode = room.currentEpisode ?: it.selectedEpisode,
                        selectedSourceType = room.sourceType ?: it.selectedSourceType,
                        selectedSourceId = room.sourceId ?: it.selectedSourceId,
                        participantsMap = room.participants.associateBy { p -> p.userId },
                        isHost = isHost,
                        step = WatchPartyStep.LOBBY,
                        isJoining = false
                    )
                }
                subscribeToLobbyState(room.id)
            }
            is JoinRoomResult.Error -> {
                updateViewState { it.copy(joinError = result.message, isJoining = false) }
            }
        }
    }
}

fun WatchPartyViewModel.onRoomTitleChanged(title: String) {
    updateViewState { it.copy(roomTitleInput = title) }
}

fun WatchPartyViewModel.onControlModeChanged(mode: WatchRoomControlMode) {
    updateViewState { it.copy(controlMode = mode) }
}

fun WatchPartyViewModel.onSeasonEpisodeChanged(season: Int?, episode: Int?) {
    updateViewState { it.copy(selectedSeason = season, selectedEpisode = episode) }
}

fun WatchPartyViewModel.onSelectSource(
    sourceType: String,
    sourceId: String,
    sourceName: String,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null
) {
    updateViewState {
        it.copy(
            selectedSourceType = sourceType,
            selectedSourceId = sourceId,
            selectedSourceName = sourceName,
            selectedSeason = seasonNumber ?: it.selectedSeason,
            selectedEpisode = episodeNumber ?: it.selectedEpisode,
            isSelectingSource = false,
            isSourceVerified = false
        )
    }
}

fun WatchPartyViewModel.onToggleSelectSource(isSelecting: Boolean) {
    updateViewState { it.copy(isSelectingSource = isSelecting) }
}

fun WatchPartyViewModel.onTestSource() {
    updateViewState { it.copy(isTestingSource = true) }
}

fun WatchPartyViewModel.onTestSourceVerified(verified: Boolean) {
    updateViewState { it.copy(isSourceVerified = verified, isTestingSource = false) }
}

fun WatchPartyViewModel.onTestSourceCancel() {
    updateViewState { it.copy(isTestingSource = false) }
}

fun WatchPartyViewModel.onCreateRoom() {
    val state = viewState.value
    val key = state.mediaKey ?: return

    viewModelScope.launch {
        if (state.selectedSourceType == null || state.selectedSourceId == null) {
            val errorMsg = getString(Res.string.watch_party_error_no_source)
            updateViewState { it.copy(createError = errorMsg) }
            return@launch
        }

        updateViewState { it.copy(isCreatingRoom = true, createError = null) }
        try {
            val mediaType = if (key.type == EntityType.TV) MediaType.TV else MediaType.MOVIE
            val defaultTitle = if (state.mediaTitle.isNotBlank()) {
                getString(Res.string.watch_party_title_format, state.mediaTitle)
            } else {
                getString(Res.string.watch_party_default_room_title)
            }
            val request = CreateRoomRequest(
                mediaId = key.id,
                mediaType = mediaType,
                title = state.roomTitleInput.ifBlank { defaultTitle },
                season = state.selectedSeason,
                episode = state.selectedEpisode,
                startPositionSeconds = 0L,
                sourceType = state.selectedSourceType,
                sourceId = state.selectedSourceId,
                controlMode = state.controlMode,
                isPrivate = state.isPrivate
            )
            val room = createWatchRoomUseCase(request)
            updateViewState {
                it.copy(
                    activeRoom = room,
                    participantsMap = room.participants.associateBy { p -> p.userId },
                    isHost = true,
                    step = WatchPartyStep.LOBBY,
                    isCreatingRoom = false
                )
            }
            subscribeToLobbyState(room.id)
        } catch (e: Exception) {
            val errorFallback = getString(Res.string.watch_party_error_create_failed)
            updateViewState { it.copy(createError = e.message ?: errorFallback, isCreatingRoom = false) }
        }
    }
}

fun WatchPartyViewModel.onSetMyIntent(intent: WatchParticipantIntent) {
    val room = viewState.value.activeRoom ?: return
    if (viewState.value.isActionPending) return
    val currentReady = viewState.value.myIsReady
    updateViewState { it.copy(myIntent = intent, isActionPending = true) }
    viewModelScope.launch {
        try {
            setLobbyStatusUseCase(
                roomId = room.id,
                request = SetLobbyStatusRequest(
                    isReady = currentReady,
                    intent = intent
                )
            )
        } catch (_: Exception) {
            updateViewState { it.copy(isActionPending = false) }
        }
    }
}

fun WatchPartyViewModel.onToggleReady() {
    val room = viewState.value.activeRoom ?: return
    if (viewState.value.isActionPending) return
    val nextReady = !viewState.value.myIsReady
    val currentIntent = viewState.value.myIntent
    updateViewState { it.copy(myIsReady = nextReady, isActionPending = true) }
    viewModelScope.launch {
        try {
            setLobbyStatusUseCase(
                roomId = room.id,
                request = SetLobbyStatusRequest(
                    isReady = nextReady,
                    intent = currentIntent
                )
            )
        } catch (_: Exception) {
            updateViewState { it.copy(myIsReady = !nextReady, isActionPending = false) }
        }
    }
}

fun WatchPartyViewModel.onStartPlayback() {
    val room = viewState.value.activeRoom ?: return
    viewModelScope.launch {
        updateViewState { it.copy(isStartingPlayback = true) }
        try {
            triggerStartPlaybackUseCase(room.id)
        } catch (_: Exception) {
            updateViewState { it.copy(isStartingPlayback = false) }
        }
    }
}

fun WatchPartyViewModel.onLeaveRoom() {
    val room = viewState.value.activeRoom ?: return
    viewModelScope.launch {
        try {
            leaveWatchRoomUseCase(room.id)
        } catch (_: Exception) {
        }
        eventStreamJob?.cancel()
        eventStreamJob = null
        updateViewState {
            it.copy(
                activeRoom = null,
                participantsMap = emptyMap(),
                isHost = false,
                step = WatchPartyStep.ENTRY,
                myIsReady = false,
                isActionPending = false,
                isStartingPlayback = false
            )
        }
        viewState.value.mediaKey?.let { loadSavedRooms(it.id) }
    }
}

fun WatchPartyViewModel.subscribeToLobbyState(roomId: Uuid) {
    eventStreamJob?.cancel()
    eventStreamJob = viewModelScope.launch {
        streamLobbyStateUseCase(roomId).collect { event ->
            when (event) {
                is LobbyEvent.InitialSnapshot -> {
                    val map = event.participants.associateBy { it.userId }
                    val myId = viewState.value.myUserId
                    val me = myId?.let { map[it] }
                    updateViewState {
                        it.copy(
                            participantsMap = map,
                            myIsReady = me?.isReady ?: it.myIsReady,
                            myIntent = me?.intent ?: it.myIntent,
                            isActionPending = false
                        )
                    }
                }
                is LobbyEvent.ParticipantUpdated -> {
                    val myId = viewState.value.myUserId
                    val isMe = event.participant.userId == myId
                    updateViewState {
                        it.copy(
                            participantsMap = it.participantsMap + (event.participant.userId to event.participant),
                            myIsReady = if (isMe) event.participant.isReady else it.myIsReady,
                            myIntent = if (isMe) event.participant.intent else it.myIntent,
                            isActionPending = if (isMe) false else it.isActionPending
                        )
                    }
                }
                is LobbyEvent.ParticipantRemoved -> {
                    updateViewState {
                        it.copy(participantsMap = it.participantsMap - event.userId)
                    }
                }
                is LobbyEvent.TransitionToPlayer -> {
                    onLaunchPlayerRequested?.invoke(roomId, event.season, event.episode)
                }
                is LobbyEvent.SystemNotice -> {
                    updateViewState { it.copy(systemNotice = event.message) }
                }
            }
        }
    }
}
