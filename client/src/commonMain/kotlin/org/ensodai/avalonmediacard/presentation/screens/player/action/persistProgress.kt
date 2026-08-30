package org.ensodai.avalonmediacard.presentation.screens.player.action

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.slot.SaveEpisodeProgressCommand
import org.ensodai.avalonmediacard.contract.slot.SaveMovieProgressCommand
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState

private val logger = AppLogging.logger("PlayerProgress")

fun PlayerViewModel.persistProgress(state: PlayerViewState, force: Boolean = false) {
    if (!force && state.status == PlaybackStatus.IDLE) return
    val mediaKey = state.mediaKey ?: return
    val current = state.currentTime
    val duration = state.duration
    if (current <= 0.0 || duration <= 0.0) return

    val currentSeconds = current.toLong()
    if (!force && currentSeconds == lastPersistedSeconds) return
    lastPersistedSeconds = currentSeconds

    viewModelScope.launch {
        withContext(if (force) NonCancellable else kotlin.coroutines.EmptyCoroutineContext) {
            try {
                logger.d { "persistProgress: type=${mediaKey.type}, id=${mediaKey.id}, pos=${currentSeconds}s/${duration.toLong()}s" }
                if (mediaKey.type == EntityType.TV) {
                    val playingEp = state.playlist.find { it.url == state.currentStreamUrl }
                        ?: state.currentEpisode

                    if (playingEp?.seasonNumber != null && playingEp.episodeNumber != null) {
                        executeServerAction(
                            SaveEpisodeProgressCommand(
                                mediaId = mediaKey.id,
                                season = playingEp.seasonNumber!!,
                                episode = playingEp.episodeNumber!!,
                                progressSeconds = currentSeconds,
                                durationSeconds = duration.toLong(),
                                isWatched = (playingEp.isWatched == true) || (current >= duration * 0.9)
                            )
                        )
                    }
                } else if (mediaKey.type == EntityType.MOVIE) {
                    executeServerAction(
                        SaveMovieProgressCommand(
                            mediaId = mediaKey.id,
                            progressSeconds = currentSeconds,
                            durationSeconds = duration.toLong(),
                            isWatched = current >= duration * 0.9
                        )
                    )
                }
            } catch (e: Throwable) {
                logger.e(e) { "Failed to persist progress: ${e.message}" }
            }
        }
    }
}
