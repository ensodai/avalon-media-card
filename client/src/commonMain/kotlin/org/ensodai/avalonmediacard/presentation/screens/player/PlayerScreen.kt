package org.ensodai.avalonmediacard.presentation.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.uuid.Uuid
import org.ensodai.avalonmediacard.core.VideoPlayer
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerInitParams
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    params: PlayerInitParams,
    onClose: () -> Unit,
    onRequestOtherSource: (() -> Unit)? = null,
    onConfirmSource: (() -> Unit)? = null,
    onReturnToLobby: ((roomId: Uuid) -> Unit)? = null,
    viewModel: PlayerViewModel = koinViewModel(key = "${params.mediaKey}_${params.targetSeason}_${params.targetEpisode}_${params.streamId}_${params.sourceType}_${params.sourceId}_${params.streamUrl?.hashCode()}_${params.mode}_${params.watchRoomId}") { parametersOf(params) }
) {
    DisposableEffect(onClose, onRequestOtherSource, onConfirmSource, onReturnToLobby) {
        viewModel.onCloseCallback = onClose
        viewModel.onRequestOtherSourceCallback = onRequestOtherSource
        viewModel.onConfirmSourceCallback = onConfirmSource
        viewModel.onReturnToLobbyCallback = onReturnToLobby
        onDispose {
            viewModel.onCloseCallback = null
            viewModel.onRequestOtherSourceCallback = null
            viewModel.onConfirmSourceCallback = null
            viewModel.onReturnToLobbyCallback = null
        }
    }
    val viewState by viewModel.viewState.collectAsState()

    LaunchedEffect(params.streamUrl, params.streamId, params.sourceType, params.sourceId, params.targetSeason, params.targetEpisode, params.playlist, params.watchRoomId) {
        viewModel.updateStream(params)
    }


    DisposableEffect(viewModel) {
        onDispose {
            viewModel.stopPlaybackAndDispose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoPlayer(
            state = viewState,
            actions = viewModel.actions,
            modifier = Modifier.fillMaxSize()
        )
    }
}
