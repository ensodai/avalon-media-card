package org.ensodai.avalonmediacard.presentation.screens.dashboardScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 1. Умная обертка (Роутер и коллектор стейта)
 * Здесь мы достаем ViewModel, подписываемся на StateFlow.
 * Вся работа с сетью изолирована во ViewModel. Навигацию прокидываем прямо отсюда.
 */
@Composable
fun DashboardScreen(
    screen: Screen,
    title: String,
    viewModel: DashboardViewModel = koinViewModel(key = screen.toString()) { parametersOf(screen) }
) {
    val state by viewModel.viewState.collectAsState()

    SduiCoordinator(viewModel) { dispatch ->
        DashboardContent(
            title = title,
            state = state,
            onAction = dispatch
        )
    }
}
