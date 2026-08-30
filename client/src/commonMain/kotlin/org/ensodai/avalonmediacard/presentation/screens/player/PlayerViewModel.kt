package org.ensodai.avalonmediacard.presentation.screens.player

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.contract.rpc.PlaybackMetadataResult
import org.ensodai.avalonmediacard.contract.rpc.StreamPlaybackResult
import org.ensodai.avalonmediacard.core.player.StreamUrlResolver
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
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

@KoinViewModel
class PlayerViewModel(
    @InjectedParam private val params: PlayerInitParams,
    val executeServerAction: ExecuteServerActionUseCase,
    val getPlaybackMetadata: GetPlaybackMetadataUseCase,
    val getPlaybackStream: GetPlaybackStreamUseCase,
    val tokenStorage: TokenStorage,
    val appSettings: AppSettingsStorage
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
        status = PlaybackStatus.BUFFERING
    )
) {
    var onCloseCallback: (() -> Unit)? = null
    var onRequestOtherSourceCallback: (() -> Unit)? = null
    var lastPersistedSeconds: Long = -1L
    private var syncJob: Job? = null
    private var metadataJob: Job? = null
    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            appSettings.defaultPlayer.collect { engine ->
                updateViewState { it.copy(defaultPlayerEngine = engine) }
            }
        }
        loadPlaybackSession(params.targetSeason, params.targetEpisode)
    }

    fun updateStream(newParams: PlayerInitParams) {
        loadPlaybackSession(newParams.targetSeason, newParams.targetEpisode)
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
                episodeNumber = targetEpisode
            )
            when (metaResult) {
                is PlaybackMetadataResult.Ready -> {
                    val fullPlaylist = metaResult.playlist.map { stream ->
                        stream.copy(url = resolveAbsoluteUrl(stream.url))
                    }
                    val currentEp = fullPlaylist.find {
                        it.seasonNumber == (metaResult.currentSeason ?: targetSeason) &&
                                it.episodeNumber == (metaResult.currentEpisode ?: targetEpisode)
                    }
                    val resolvedTitle = currentEp?.episodeName ?: currentEp?.title ?: metaResult.episodeTitle
                    val initialPosition = metaResult.startPositionSeconds ?: currentEp?.watchedProgressSeconds ?: 0L
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
                episodeNumber = episode ?: params.targetEpisode
            )
            when (result) {
                is StreamPlaybackResult.Ready -> {
                    val fullUrl = resolveAbsoluteUrl(result.streamUrl)
                    val fullPlaylist = (result.playlist.ifEmpty { viewState.value.playlist }).map { stream ->
                        stream.copy(url = resolveAbsoluteUrl(stream.url))
                    }
                    val targetEpisode = fullPlaylist.find { it.canonicalId == result.streamId }
                        ?: fullPlaylist.find { it.url == fullUrl }
                    val resolvedTitle = targetEpisode?.episodeName ?: targetEpisode?.title ?: viewState.value.title
                    val startPosition = result.startPositionSeconds ?: targetEpisode?.watchedProgressSeconds ?: 0L

                    updateViewState {
                        it.copy(
                            title = resolvedTitle.ifBlank { it.title },
                            currentStreamId = result.streamId,
                            currentStreamUrl = fullUrl,
                            duration = result.durationSeconds ?: targetEpisode?.durationSeconds ?: it.duration,
                            currentTime = startPosition.toDouble(),
                            audioTracks = result.audioTracks.ifEmpty { it.audioTracks },
                            subtitleTracks = result.subtitleTracks.ifEmpty { it.subtitleTracks },
                            selectedAudioTrackIndex = result.audioTrackIndex ?: it.selectedAudioTrackIndex,
                            playlist = if (it.playlist.isEmpty()) fullPlaylist else it.playlist,
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
        persistProgress(viewState.value, force = true)
        updateViewState { it.copy(status = PlaybackStatus.IDLE, currentStreamUrl = null) }
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
        onEpisodeSelected = ::onEpisodeSelected,
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
        onChangeDefaultPlayer = ::onChangeDefaultPlayer
    )
}
