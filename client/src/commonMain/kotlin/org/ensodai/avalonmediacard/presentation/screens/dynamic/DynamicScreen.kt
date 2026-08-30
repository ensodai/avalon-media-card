package org.ensodai.avalonmediacard.presentation.screens.dynamic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DynamicScreen(
    screen: Screen.Dynamic,
    viewModel: DynamicViewModel = koinViewModel(key = screen.screenId) { parametersOf(screen) }
) {
    val state by viewModel.viewState.collectAsState()

    SduiCoordinator(viewModel) { dispatch ->
        DynamicContent(
            title = screen.title,
            state = state,
            onAction = dispatch
        )
    }
}

