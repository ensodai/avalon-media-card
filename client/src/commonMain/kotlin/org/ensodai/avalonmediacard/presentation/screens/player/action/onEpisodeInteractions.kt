package org.ensodai.avalonmediacard.presentation.screens.player.action

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.slot.RateEpisodeCommand
import org.ensodai.avalonmediacard.contract.slot.ToggleEpisodeWatchedCommand
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerEngine

fun PlayerViewModel.onToggleEpisodeWatched(episode: MediaStream) {
    val mediaKey = viewState.value.mediaKey ?: return
    val season = episode.seasonNumber ?: return
    val epNum = episode.episodeNumber ?: return
    val newWatched = !episode.isWatched

    updateViewState { state ->
        val updatedList = state.playlist.map {
            if (it.url == episode.url) it.copy(isWatched = newWatched) else it
        }
        state.copy(playlist = updatedList)
    }

    viewModelScope.launch {
        executeServerAction(
            ToggleEpisodeWatchedCommand(
                key = mediaKey,
                seasonNumber = season,
                episodeNumber = epNum,
                isWatched = newWatched
            )
        )
    }
}

fun PlayerViewModel.onRateEpisode(episode: MediaStream, rating: Int) {
    val mediaKey = viewState.value.mediaKey ?: return
    val season = episode.seasonNumber ?: return
    val epNum = episode.episodeNumber ?: return

    updateViewState { state ->
        val updatedList = state.playlist.map {
            if (it.url == episode.url) it.copy(userRating = rating) else it
        }
        state.copy(playlist = updatedList)
    }

    viewModelScope.launch {
        executeServerAction(
            RateEpisodeCommand(
                key = mediaKey,
                seasonNumber = season,
                episodeNumber = epNum,
                rating = rating
            )
        )
    }
}

fun PlayerViewModel.onChangeDefaultPlayer(engine: PlayerEngine) {
    updateViewState { it.copy(defaultPlayerEngine = engine) }
    viewModelScope.launch {
        appSettings.saveDefaultPlayer(engine)
    }
}
