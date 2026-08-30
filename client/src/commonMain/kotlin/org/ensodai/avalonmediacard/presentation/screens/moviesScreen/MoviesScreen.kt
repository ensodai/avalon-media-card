package org.ensodai.avalonmediacard.presentation.screens.moviesScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MoviesScreen(
    screen: Screen,
    title: String,
    viewModel: MoviesViewModel = koinViewModel(key = screen.toString()) { parametersOf(screen) }
) {
    val state by viewModel.viewState.collectAsState()

    SduiCoordinator(viewModel) { dispatch ->
        MoviesContent(
            state = state,
            onAction = dispatch
        )
    }
}
