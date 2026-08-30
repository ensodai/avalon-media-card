package org.ensodai.avalonmediacard.presentation.screens.integrations


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntegrationsScreen(
    viewModel: IntegrationsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.viewState.collectAsState()

    SduiCoordinator(viewModel) { dispatch ->
        IntegrationsContent(
            state = state,
            onAction = dispatch,
            modifier = modifier
        )
    }
}

