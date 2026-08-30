package org.ensodai.avalonmediacard.presentation.screens.player.action

import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus

fun PlayerViewModel.onEpisodeSelected(stream: MediaStream) {
    // 1. Сохраняем прогресс предыдущей серии
    persistProgress(viewState.value)

    val startPos = (stream.watchedProgressSeconds ?: 0L).toDouble()

    // 2. Мгновенное обновление UI для новой выбранной серии
    updateViewState { state ->
        state.copy(
            title = stream.episodeName ?: stream.title,
            currentStreamId = stream.canonicalId,
            currentStreamUrl = null,
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

    // 3. Загружаем поток для выбранной серии
    loadStreamOnly(stream.seasonNumber, stream.episodeNumber)
}

