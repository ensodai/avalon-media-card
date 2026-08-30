package org.ensodai.avalonmediacard.presentation.screens.myCollectionScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyCollectionScreen(
    expectedItemsCount: Int? = null,
    viewModel: MyCollectionViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    SduiCoordinator(viewModel) { dispatch ->
        MyCollectionContent(
            state = state,
            onAction = dispatch,
            expectedItemsCount = expectedItemsCount
        )
    }
}
