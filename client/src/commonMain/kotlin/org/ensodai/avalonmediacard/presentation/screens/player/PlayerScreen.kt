package org.ensodai.avalonmediacard.presentation.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.ensodai.avalonmediacard.core.VideoPlayer
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerInitParams
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlayerScreen(
    params: PlayerInitParams,
    onClose: () -> Unit,
    onRequestOtherSource: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(key = "${params.mediaKey}_${params.targetSeason}_${params.targetEpisode}_${params.streamId}_${params.streamUrl?.hashCode()}") { parametersOf(params) }
) {
    DisposableEffect(onClose, onRequestOtherSource) {
        viewModel.onCloseCallback = onClose
        viewModel.onRequestOtherSourceCallback = onRequestOtherSource
        onDispose {
            viewModel.onCloseCallback = null
            viewModel.onRequestOtherSourceCallback = null
        }
    }
    val viewState by viewModel.viewState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(params.streamUrl, params.streamId, params.targetSeason, params.targetEpisode, params.playlist) {
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
