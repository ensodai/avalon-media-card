package org.ensodai.avalonmediacard.presentation.screens.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.ensodai.avalonmediacard.presentation.telemetry.LocalTelemetryTracker
import org.ensodai.avalonmediacard.presentation.telemetry.TrackPageView
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SearchScreen(
    screen: Screen.Search,
    viewModel: SearchViewModel = koinViewModel(key = screen.initialQuery) { parametersOf(screen) }
) {
    val state by viewModel.viewState.collectAsState()

    TrackPageView(
        screenId = "SearchScreen",
        context = ClickstreamContext.SEARCH_PAGE
    )

    val telemetryTracker = LocalTelemetryTracker.current

    SduiCoordinator(viewModel) { dispatch ->
        SearchContent(
            initialQuery = screen.initialQuery,
            state = state,
            onAction = dispatch,
            onSearchQueryChanged = { query ->
                telemetryTracker.logSearch(query)
                viewModel.onSearchQueryChanged(query)
            }
        )
    }
}

