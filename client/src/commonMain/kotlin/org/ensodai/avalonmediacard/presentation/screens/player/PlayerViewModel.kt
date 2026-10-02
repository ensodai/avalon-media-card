package org.ensodai.avalonmediacard.presentation.screens.player

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.contract.rpc.PlaybackMetadataResult
import org.ensodai.avalonmediacard.contract.rpc.StreamPlaybackResult
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.core.player.StreamUrlResolver
import org.ensodai.avalonmediacard.core.player.watchparty.ClientSyncController
import org.ensodai.avalonmediacard.core.player.watchparty.ClockSyncService
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.data.TokenStorage
import org.ensodai.avalonmediacard.data.platformServerUrl
import org.ensodai.avalonmediacard.domain.useCases.core.ExecuteServerActionUseCase
import org.ensodai.avalonmediacard.domain.useCases.playback.GetPlaybackMetadataUseCase
import org.ensodai.avalonmediacard.domain.useCases.playback.GetPlaybackStreamUseCase
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.action.*
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerInitParams
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerMode
import org.ensodai.avalonmediacard.presentation.screens.player.model.WatchPartyReaction
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerChatUiMessage
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@KoinViewModel
class PlayerViewModel(
    @InjectedParam private val params: PlayerInitParams,
    val executeServerAction: ExecuteServerActionUseCase,
    val getPlaybackMetadata: GetPlaybackMetadataUseCase,
    val getPlaybackStream: GetPlaybackStreamUseCase,
    val tokenStorage: TokenStorage,
    val appSettings: AppSettingsStorage,
    val clockSync: ClockSyncService,
    val rpcService: WatchPartyRpcService
) : BaseViewModel<PlayerViewState, PlayerActions>(
    initialState = PlayerViewState(
        title = params.title,
        seriesTitle = params.seriesTitle,
        mediaKey = params.mediaKey,
        currentStreamId = params.streamId ?: "",
        currentStreamUrl = params.streamUrl?.takeIf { it.isNotBlank() },
        playlist = params.playlist,

        duration = params.durationSeconds ?: 0.0,
        currentTime = (params.startPositionSeconds ?: 0L).toDouble(),
        audioTracks = params.audioTracks,
        subtitleTracks = params.subtitleTracks,
        selectedAudioTrackIndex = params.audioTrackIndex,
        defaultPlayerEngine = appSettings.cachedDefaultPlayer,
        mode = params.mode,
        watchRoomId = params.watchRoomId,
        currentUserId = tokenStorage.cachedUserId,
        isHost = params.isHost,
        status = PlaybackStatus.BUFFERING
    )
) {
    var onCloseCallback: (() -> Unit)? = null
    var onRequestOtherSourceCallback: (() -> Unit)? = null
    var onConfirmSourceCallback: (() -> Unit)? = null
    var onReturnToLobbyCallback: ((roomId: Uuid) -> Unit)? = null
    var lastPersistedSeconds: Long = -1L
    var seekDebounceJob: Job? = null
    private var syncJob: Job? = null
    private var syncEventsJob: Job? = null
    private var metadataJob: Job? = null
    private var playbackJob: Job? = null
    private var controllerBufferingJob: Job? = null
    private var reactionCounter: Long = 0L

    var activeController: PlaybackController? = null
        private set
    var syncController: ClientSyncController? = null
        private set

    fun attachController(controller: PlaybackController) {
        activeController = controller
        controllerBufferingJob?.cancel()
        controllerBufferingJob = viewModelScope.launch {
            controller.isBufferingFlow.collect { isBuffering ->
                updateViewState { s ->
                    val newStatus = when {
                        isBuffering -> PlaybackStatus.BUFFERING
                        controller.state.isPlaying -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    }
                    s.copy(status = newStatus)
                }
            }
        }
        val roomId = viewState.value.watchRoomId
        if (viewState.value.mode == PlayerMode.WATCH_PARTY && roomId != null) {
            val existing = syncController
            if (existing != null && existing.roomId == roomId) {
                existing.updateUnderlyingController(controller)
            } else {
                syncEventsJob?.cancel()
                syncController?.stop()
                val sc = ClientSyncController(
                    roomId = roomId,
                    underlyingController = controller,
                    clockSync = clockSync,
                    rpcService = rpcService,
                    coroutineScope = viewModelScope
                ).also { scInstance ->
                    scInstance.onEpisodeChangeRequested = { season, episode ->
                        handleRemoteEpisodeChange(season, episode)
                    }
                    scInstance.start()
                }
                syncController = sc
                syncEventsJob = viewModelScope.launch {
                    sc.roomEvents.collect { event ->
                        when (event) {
                            is WatchRoomEvent.ParticipantsUpdated -> {
                                val currentUid = viewState.value.currentUserId
                                val myRole = event.participants.find { it.userId.toString() == currentUid }?.role
                                updateViewState {
                                    it.copy(
                                        watchRoomParticipants = event.participants,
                                        isHost = it.isHost || myRole == WatchRoomParticipantRole.HOST
                                    )
                                }
                            }
                            is WatchRoomEvent.ReturnedToLobby -> {
                                val roomId = viewState.value.watchRoomId
                                if (roomId != null) {
                                    onReturnToLobbyCallback?.invoke(roomId)
                                }
                                actions.onCloseClicked()
                            }
                            is WatchRoomEvent.ChatHistorySnapshot -> {
                                val currentUid = viewState.value.currentUserId
                                val uiMessages = event.messages.map { msg ->
                                    val isMe = msg.senderUserId.toString() == currentUid
                                    val isHost = viewState.value.watchRoomParticipants.find { it.userId == msg.senderUserId }?.role == WatchRoomParticipantRole.HOST
                                    PlayerChatUiMessage(
                                        id = msg.id,
                                        senderUserId = msg.senderUserId,
                                        senderUsername = msg.senderUsername,
                                        senderAvatarUrl = msg.senderAvatarUrl,
                                        text = msg.text,
                                        formattedPosition = formatChatTimecode(msg.playbackPositionMs),
                                        rawPositionMs = msg.playbackPositionMs,
                                        isFromMe = isMe,
                                        isHost = isHost,
                                        isSending = false,
                                        createdAt = msg.createdAt
                                    )
                                }
                                updateViewState {
                                    it.copy(
                                        chatState = it.chatState.copy(
                                            messages = uiMessages
                                        )
                                    )
                                }
                            }
                            is WatchRoomEvent.ChatMessageReceived -> {
                                val currentUid = viewState.value.currentUserId
                                val msg = event.message
                                val isMe = msg.senderUserId.toString() == currentUid
                                val isHost = viewState.value.watchRoomParticipants.find { it.userId == msg.senderUserId }?.role == WatchRoomParticipantRole.HOST
                                val uiMessage = PlayerChatUiMessage(
                                    id = msg.id,
                                    senderUserId = msg.senderUserId,
                                    senderUsername = msg.senderUsername,
                                    senderAvatarUrl = msg.senderAvatarUrl,
                                    text = msg.text,
                                    formattedPosition = formatChatTimecode(msg.playbackPositionMs),
                                    rawPositionMs = msg.playbackPositionMs,
                                    isFromMe = isMe,
                                    isHost = isHost,
                                    isSending = false,
                                    createdAt = msg.createdAt
                                )
                                updateViewState { state ->
                                    val existingMessages = state.chatState.messages
                                    val filtered = if (isMe) {
                                        val idx = existingMessages.indexOfFirst { it.isSending && it.text == msg.text }
                                        if (idx != -1) {
                                            existingMessages.toMutableList().apply { removeAt(idx) }
                                        } else {
                                            existingMessages
                                        }
                                    } else {
                                        existingMessages
                                    }
                                    val newUnread = if (!state.chatState.isVisible && !state.chatState.isInputFocused) {
                                        state.chatState.unreadCount + 1
                                    } else {
                                        0
                                    }
                                    state.copy(
                                        chatState = state.chatState.copy(
                                            messages = filtered + uiMessage,
                                            unreadCount = newUnread
                                        )
                                    )
                                }
                            }
                            is WatchRoomEvent.ReactionTriggered -> {
                                val currentUid = viewState.value.currentUserId
                                if (event.userId.toString() != currentUid) {
                                    val nextId = ++reactionCounter
                                    updateViewState { state ->
                                        state.copy(
                                            lastReaction = WatchPartyReaction(
                                                id = nextId,
                                                emoji = event.emoji,
                                                senderUsername = event.username,
                                                isFromMe = false
                                            )
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    fun handleRemoteEpisodeChange(season: Int?, episode: Int?) {
        val targetStream = viewState.value.playlist.find {
            it.seasonNumber == season && it.episodeNumber == episode
        }
        if (targetStream != null) {
            onEpisodeSelected(targetStream, isRemoteSync = true)
        } else {
            loadPlaybackSession(season, episode)
        }
    }

    fun detachController() {
        controllerBufferingJob?.cancel()
        controllerBufferingJob = null
        syncEventsJob?.cancel()
        syncEventsJob = null
        syncController?.stop()
        syncController = null
        activeController = null
    }

    init {
        viewModelScope.launch {
            tokenStorage.userId.collect { uid ->
                updateViewState { it.copy(currentUserId = uid) }
            }
        }
        viewModelScope.launch {
            appSettings.defaultPlayer.collect { engine ->
                updateViewState { it.copy(defaultPlayerEngine = engine) }
            }
        }
        if (params.mode == PlayerMode.TEST_PREVIEW && !params.streamUrl.isNullOrBlank()) {
            val fullUrl = resolveAbsoluteUrl(params.streamUrl)
            updateViewState {
                it.copy(
                    currentStreamUrl = fullUrl,
                    currentStreamId = params.streamId ?: it.currentStreamId,
                    playlist = if (params.playlist.isNotEmpty()) params.playlist else listOf(
                        MediaStream(
                            id = params.streamId ?: "",
                            title = params.title,
                            url = fullUrl,
                            type = StreamType.DirectUrl,
                            sourceName = params.title
                        )
                    ),
                    audioTracks = params.audioTracks,
                    subtitleTracks = params.subtitleTracks,
                    selectedAudioTrackIndex = params.audioTrackIndex,
                    status = PlaybackStatus.BUFFERING
                )
            }
        } else {
            loadPlaybackSession(params.targetSeason, params.targetEpisode)
        }
    }

    fun updateStream(newParams: PlayerInitParams) {
        if (newParams.mode == PlayerMode.TEST_PREVIEW && !newParams.streamUrl.isNullOrBlank()) {
            val fullUrl = resolveAbsoluteUrl(newParams.streamUrl)
            updateViewState {
                it.copy(
                    currentStreamUrl = fullUrl,
                    currentStreamId = newParams.streamId ?: it.currentStreamId,
                    mode = newParams.mode,
                    watchRoomId = newParams.watchRoomId,
                    status = PlaybackStatus.BUFFERING
                )
            }
        } else {
            loadPlaybackSession(newParams.targetSeason, newParams.targetEpisode)
        }
    }

    fun resolveAbsoluteUrl(url: String): String {
        val serverUrl = tokenStorage.cachedServerUrl?.takeIf { it.isNotBlank() }
            ?: platformServerUrl
        return StreamUrlResolver.resolveAbsoluteUrl(url, serverUrl)
    }

    fun loadPlaybackSession(season: Int? = null, episode: Int? = null) {
        metadataJob?.cancel()
        playbackJob?.cancel()

        val targetSeason = season ?: params.targetSeason
        val targetEpisode = episode ?: params.targetEpisode

        updateViewState {
            it.copy(
                status = PlaybackStatus.BUFFERING,
                currentStreamUrl = null,
                errorMessage = null,
                audioTracks = emptyList(),
                subtitleTracks = emptyList(),
                selectedAudioTrackIndex = null,
                selectedSubtitleTrack = null
            )
        }

        // Фаза 1: Мгновенные метаданные UI из ядра (БД + TMDB)
        metadataJob = viewModelScope.launch {
            val metaResult = getPlaybackMetadata(
                key = params.mediaKey,
                seasonNumber = targetSeason,
                episodeNumber = targetEpisode,
                sourceType = params.sourceType,
                sourceId = params.sourceId
            )
            when (metaResult) {
                is PlaybackMetadataResult.Ready -> {
                    val fullPlaylist = metaResult.playlist.map { stream ->
                        stream.copy(
                            url = resolveAbsoluteUrl(stream.url),
                            qualityVariants = stream.qualityVariants.map { it.copy(url = resolveAbsoluteUrl(it.url)) },
                            subtitleTracks = stream.subtitleTracks.map { it.copy(url = it.url?.let(::resolveAbsoluteUrl)) }
                        )
                    }
                    val currentEp = fullPlaylist.find {
                        it.seasonNumber == (metaResult.currentSeason ?: targetSeason) &&
                                it.episodeNumber == (metaResult.currentEpisode ?: targetEpisode)
                    }
                    val resolvedTitle = currentEp?.episodeName ?: currentEp?.title ?: metaResult.episodeTitle
                    val initialPosition = if (viewState.value.mode == PlayerMode.WATCH_PARTY) {
                        params.startPositionSeconds ?: 0L
                    } else {
                        metaResult.startPositionSeconds ?: currentEp?.watchedProgressSeconds ?: 0L
                    }
                    val streamId = currentEp?.canonicalId ?: ""

                    updateViewState {
                        it.copy(
                            title = resolvedTitle.ifBlank { it.title },
                            seriesTitle = metaResult.seriesTitle ?: it.seriesTitle,
                            currentStreamId = streamId.ifBlank { it.currentStreamId },
                            playlist = fullPlaylist,
                            duration = metaResult.durationSeconds ?: it.duration,
                            currentTime = initialPosition.toDouble()
                        )
                    }

                }
                is PlaybackMetadataResult.NoSourceBound -> {
                    updateViewState { it.copy(status = PlaybackStatus.IDLE, currentStreamUrl = null) }
                    onRequestOtherSourceCallback?.invoke()
                    return@launch
                }
                is PlaybackMetadataResult.Error -> {
                    // Ошибку потока обработает Фаза 2
                }
            }
        }

        // Фаза 2: Асинхронная подготовка видеопотока из плагина
        loadStreamOnly(targetSeason, targetEpisode)
    }

    fun loadStreamOnly(season: Int? = null, episode: Int? = null) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val result = getPlaybackStream(
                key = params.mediaKey,
                seasonNumber = season ?: params.targetSeason,
                episodeNumber = episode ?: params.targetEpisode,
                sourceType = params.sourceType,
                sourceId = params.sourceId
            )
            when (result) {
                is StreamPlaybackResult.Ready -> {
                    val fullUrl = resolveAbsoluteUrl(result.streamUrl)
                    val fullPlaylist = (result.playlist.ifEmpty { viewState.value.playlist }).map { stream ->
                        stream.copy(
                            url = resolveAbsoluteUrl(stream.url),
                            qualityVariants = stream.qualityVariants.map { it.copy(url = resolveAbsoluteUrl(it.url)) },
                            subtitleTracks = stream.subtitleTracks.map { it.copy(url = it.url?.let(::resolveAbsoluteUrl)) }
                        )
                    }
                    val targetEpisode = fullPlaylist.find { it.canonicalId == result.streamId }
                        ?: fullPlaylist.find { it.url == fullUrl }
                    val resolvedTitle = targetEpisode?.episodeName ?: targetEpisode?.title ?: viewState.value.title
                    val startPosition = if (viewState.value.mode == PlayerMode.WATCH_PARTY) {
                        params.startPositionSeconds ?: 0L
                    } else {
                        result.startPositionSeconds ?: targetEpisode?.watchedProgressSeconds ?: 0L
                    }

                    val resolvedAudio = result.audioTracks.map { it.copy(url = it.url?.let(::resolveAbsoluteUrl)) }
                    val resolvedSubs = result.subtitleTracks.map { it.copy(url = it.url?.let(::resolveAbsoluteUrl)) }

                    updateViewState {
                        it.copy(
                            title = resolvedTitle.ifBlank { it.title },
                            currentStreamId = result.streamId,
                            currentStreamUrl = fullUrl,
                            duration = result.durationSeconds ?: targetEpisode?.durationSeconds ?: it.duration,
                            currentTime = startPosition.toDouble(),
                            audioTracks = resolvedAudio.ifEmpty { it.audioTracks },
                            subtitleTracks = resolvedSubs.ifEmpty { it.subtitleTracks },
                            selectedAudioTrackIndex = result.audioTrackIndex ?: it.selectedAudioTrackIndex,
                            playlist = if (result.playlist.isNotEmpty()) fullPlaylist else (if (it.playlist.isEmpty()) fullPlaylist else it.playlist),
                            status = PlaybackStatus.BUFFERING,
                            errorMessage = null
                        )
                    }
                }

                is StreamPlaybackResult.NoSourceBound -> {
                    updateViewState { it.copy(status = PlaybackStatus.IDLE, currentStreamUrl = null) }
                    onRequestOtherSourceCallback?.invoke()
                }
                is StreamPlaybackResult.Error -> {
                    updateViewState {
                        it.copy(
                            status = PlaybackStatus.ERROR,
                            currentStreamUrl = null,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }


    fun checkAndStartSyncLoop() {
        val state = viewState.value
        if (state.mode == PlayerMode.TEST_PREVIEW) return
        if (state.status == PlaybackStatus.PLAYING || state.isPlaying) {
            if (syncJob?.isActive == true) return
            syncJob = viewModelScope.launch {
                while (isActive) {
                    delay(5000.milliseconds)
                    val s = viewState.value
                    if ((s.isPlaying || s.status == PlaybackStatus.PLAYING) && s.currentTime > 0.0 && s.duration > 0.0) {
                        persistProgress(s)
                    }
                }
            }
        } else {
            syncJob?.cancel()
            syncJob = null
        }
    }

    fun stopPlaybackAndDispose() {
        syncJob?.cancel()
        syncJob = null
        syncEventsJob?.cancel()
        syncEventsJob = null
        syncController?.stop()
        syncController = null
        detachController()
        if (viewState.value.mode != PlayerMode.TEST_PREVIEW) {
            persistProgress(viewState.value, force = true)
        }
        updateViewState { it.copy(status = PlaybackStatus.IDLE, currentStreamUrl = null) }
    }

    fun onToggleParticipantsPanel() {
        updateViewState { it.copy(isParticipantsPanelVisible = !it.isParticipantsPanelVisible) }
    }


    fun onQualitySelected(variant: VideoQuality) {
        val currentSec = viewState.value.currentTime
        val fullUrl = resolveAbsoluteUrl(variant.url)
        updateViewState {
            it.copy(
                currentStreamUrl = fullUrl,
                currentTime = currentSec,
                status = PlaybackStatus.BUFFERING
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlaybackAndDispose()
    }

    override val actions = PlayerActions(
        onPlayPauseClicked = ::onPlayPauseClicked,
        onSeek = ::onSeek,
        onEpisodeSelected = { stream -> onEpisodeSelected(stream) },
        onNextEpisodeClicked = { viewState.value.nextEpisode?.let { onEpisodeSelected(it) } },
        onPrevEpisodeClicked = { viewState.value.prevEpisode?.let { onEpisodeSelected(it) } },
        onAudioTrackSelected = ::onAudioTrackSelected,
        onSubtitleTrackSelected = ::onSubtitleTrackSelected,
        onQualitySelected = ::onQualitySelected,
        onToggleFullscreen = ::onToggleFullscreen,
        onFullscreenChanged = ::onFullscreenChanged,
        onControlsVisibilityChanged = ::onControlsVisibilityChanged,
        onProgressUpdate = ::onProgressUpdate,
        onPlaybackStateChanged = ::onPlaybackStateChanged,
        onError = ::onError,
        onStreamRecovery = ::onStreamRecovery,
        onCloseClicked = {
            stopPlaybackAndDispose()
            onCloseCallback?.invoke()
        },
        onRequestOtherSource = {
            stopPlaybackAndDispose()
            onRequestOtherSourceCallback?.invoke()
        },
        onToggleEpisodeWatched = ::onToggleEpisodeWatched,
        onRateEpisode = ::onRateEpisode,
        onChangeDefaultPlayer = ::onChangeDefaultPlayer,
        onConfirmSource = {
            stopPlaybackAndDispose()
            onConfirmSourceCallback?.invoke()
        },
        onAttachController = ::attachController,
        onDetachController = ::detachController,
        onToggleParticipantsPanel = ::onToggleParticipantsPanel,
        onReturnToLobby = ::onReturnToLobby,
        onSendReaction = ::onSendReaction,
        chat = createPlayerChatActions()
    )

    fun onSendReaction(emoji: String) {
        val roomId = viewState.value.watchRoomId ?: return
        val currentUid = viewState.value.currentUserId
        val username = viewState.value.watchRoomParticipants
            .find { it.userId.toString() == currentUid }?.username ?: "Me"

        val nextId = ++reactionCounter
        updateViewState { state ->
            state.copy(
                lastReaction = WatchPartyReaction(
                    id = nextId,
                    emoji = emoji,
                    senderUsername = username,
                    isFromMe = true
                )
            )
        }

        viewModelScope.launch {
            try {
                val sc = syncController
                if (sc != null) {
                    sc.sendReaction(emoji)
                } else {
                    rpcService.sendReaction(roomId, emoji)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun onReturnToLobby() {
        val roomId = viewState.value.watchRoomId ?: return
        viewModelScope.launch {
            try {
                rpcService.sendPlaybackCommand(roomId, RoomPlaybackCommand.ReturnToLobby)
            } catch (_: Exception) {
            }
        }
    }
}
