package org.ensodai.avalonmediacard.presentation.screens.person

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.DetailsContent
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.viewState.DetailsViewState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PersonScreen(
    screen: Screen.Person,
    viewModel: PersonViewModel = koinViewModel(key = screen.key.id) { parametersOf(screen) }
) {
    val state by viewModel.viewState.collectAsState()

    SduiCoordinator(viewModel) { dispatch ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val detailsState = DetailsViewState(
                mediaKey = screen.key,
                header = state.header,
                description = state.bio,
                carousels = state.credits
            )

            DetailsContent(
                state = detailsState,
                onAction = dispatch
            )
        }
    }
}
