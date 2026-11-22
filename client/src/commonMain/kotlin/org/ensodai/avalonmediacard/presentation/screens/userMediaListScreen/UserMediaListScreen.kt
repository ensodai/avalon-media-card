package org.ensodai.avalonmediacard.presentation.screens.userMediaListScreen

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.components.mediaGridSlot.MediaGridSlot
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun UserMediaListScreen(
    screen: Screen.UserMediaList,
    viewModel: UserMediaListViewModel = koinViewModel(key = screen.toString()) { parametersOf(screen) }
) {
    val state by viewModel.viewState.collectAsState()

    SduiCoordinator(viewModel) { dispatch ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AvalonTheme.colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = screen.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    state.grids.forEach { slot ->
                        MediaGridSlot(
                            state = slot.state,
                            onAction = dispatch,
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
