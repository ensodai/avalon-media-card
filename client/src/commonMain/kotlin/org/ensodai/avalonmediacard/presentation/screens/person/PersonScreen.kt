package org.ensodai.avalonmediacard.presentation.screens.person

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PersonScreen(
    screen: Screen.Person,
    modifier: Modifier = Modifier,
    viewModel: PersonViewModel = koinViewModel(key = screen.key.id) { parametersOf(screen) }
) {
    val state by viewModel.viewState.collectAsState()

    SduiCoordinator(viewModel) { dispatch ->
        PersonContent(
            header = state.header,
            bio = state.bio,
            credits = state.credits,
            onAction = dispatch,
            modifier = modifier
        )
    }
}

