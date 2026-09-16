package org.ensodai.avalonmediacard.presentation.screens.player.action

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.slot.RateEpisodeCommand
import org.ensodai.avalonmediacard.contract.slot.SetRatingCommand
import org.ensodai.avalonmediacard.contract.slot.SetStatusCommand
import org.ensodai.avalonmediacard.contract.slot.ToggleEpisodeWatchedCommand
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerEngine

private val logger = AppLogging.logger("PlayerEpisodeInteractions")

private fun isStreamMatching(a: MediaStream, b: MediaStream): Boolean {
    if (a.canonicalId.isNotBlank() && b.canonicalId.isNotBlank()) {
        if (a.canonicalId == b.canonicalId) return true
    }
    if (a.seasonNumber != null && b.seasonNumber != null && a.episodeNumber != null && b.episodeNumber != null) {
        if (a.seasonNumber == b.seasonNumber && a.episodeNumber == b.episodeNumber) return true
    }
    return a.url == b.url
}

fun PlayerViewModel.onToggleEpisodeWatched(episode: MediaStream) {
    val mediaKey = viewState.value.mediaKey ?: return
    val isMovie = mediaKey.type == EntityType.MOVIE
    val newWatched = !episode.isWatched

    logger.d { "onToggleEpisodeWatched: mediaKey=$mediaKey, isMovie=$isMovie, newWatched=$newWatched, epId=${episode.canonicalId}" }

    updateViewState { state ->
        val updatedList = if (state.playlist.isEmpty()) {
            listOf(episode.copy(isWatched = newWatched))
        } else {
            state.playlist.map {
                if (isStreamMatching(it, episode)) it.copy(isWatched = newWatched) else it
            }
        }
        state.copy(playlist = updatedList)
    }

    viewModelScope.launch {
        if (isMovie) {
            val status = if (newWatched) MediaStatus.COMPLETED else MediaStatus.NONE
            executeServerAction(SetStatusCommand(key = mediaKey, status = status))
        } else {
            val season = episode.seasonNumber ?: 1
            val epNum = episode.episodeNumber ?: 1
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
}

fun PlayerViewModel.onRateEpisode(episode: MediaStream, rating: Int) {
    val mediaKey = viewState.value.mediaKey ?: return
    val isMovie = mediaKey.type == EntityType.MOVIE

    logger.d { "onRateEpisode: mediaKey=$mediaKey, isMovie=$isMovie, rating=$rating, epId=${episode.canonicalId}" }

    updateViewState { state ->
        val updatedList = if (state.playlist.isEmpty()) {
            listOf(episode.copy(userRating = rating))
        } else {
            state.playlist.map {
                if (isStreamMatching(it, episode)) it.copy(userRating = rating) else it
            }
        }
        state.copy(playlist = updatedList)
    }

    viewModelScope.launch {
        if (isMovie) {
            executeServerAction(SetRatingCommand(key = mediaKey, rating = rating))
        } else {
            val season = episode.seasonNumber ?: 1
            val epNum = episode.episodeNumber ?: 1
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
}

fun PlayerViewModel.onChangeDefaultPlayer(engine: PlayerEngine) {
    updateViewState { it.copy(defaultPlayerEngine = engine) }
    viewModelScope.launch {
        appSettings.saveDefaultPlayer(engine)
    }
}

