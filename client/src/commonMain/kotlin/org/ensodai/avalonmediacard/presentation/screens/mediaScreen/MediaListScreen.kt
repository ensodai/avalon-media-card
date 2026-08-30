package org.ensodai.avalonmediacard.presentation.screens.mediaScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MediaListScreen(
    screen: Screen.MediaList,
    viewModel: MediaListViewModel = koinViewModel(key = screen.key.id) { parametersOf(screen) }
) {
    val state by viewModel.viewState.collectAsState()

    SduiCoordinator(viewModel) { dispatch ->
        MediaListContent(
            title = screen.title,
            state = state,
            onAction = dispatch
        )
    }
}

