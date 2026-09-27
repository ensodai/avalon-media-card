package org.ensodai.avalonmediacard.presentation.screens.player.action

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerMode
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus

fun PlayerViewModel.onEpisodeSelected(stream: MediaStream, isRemoteSync: Boolean = false) {
    // 1. Сохраняем прогресс предыдущей серии (если не тестовый режим)
    if (viewState.value.mode != PlayerMode.TEST_PREVIEW) {
        persistProgress(viewState.value)
    }

    val isWatchParty = viewState.value.mode == PlayerMode.WATCH_PARTY
    val startPos = if (isWatchParty) 0.0 else (stream.watchedProgressSeconds ?: 0L).toDouble()

    // 2. В режиме совместного просмотра локальное переключение хостом рассылает команду в комнату
    if (isWatchParty && !isRemoteSync && syncController != null) {
        viewModelScope.launch {
            syncController?.sendChangeEpisode(stream.seasonNumber, stream.episodeNumber)
        }
        return
    }

    // 3. Мгновенное обновление UI для новой выбранной серии
    activeController?.pause()
    activeController?.state?.isBuffering = true

    updateViewState { state ->
        state.copy(
            title = stream.episodeName ?: stream.title,
            currentStreamId = stream.canonicalId,
            currentTime = startPos,
            bufferedTime = 0.0,

            status = PlaybackStatus.BUFFERING,
            audioTracks = stream.audioTracks,
            subtitleTracks = stream.subtitleTracks,
            selectedAudioTrackIndex = null,
            selectedSubtitleTrack = null,
            errorMessage = null
        )
    }

    // 4. Загружаем поток для выбранной серии
    loadStreamOnly(stream.seasonNumber, stream.episodeNumber)
}

