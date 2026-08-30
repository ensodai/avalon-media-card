package org.ensodai.avalonmediacard.presentation.navigation


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavKey

@Composable
fun <T : NavKey> AvalonNavHost(
    controller: AvalonNavController<T>,
    manager: VisualStateManager<T> = remember { VisualStateManager<T>() },
    screenContent: @Composable (route: T) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val logicalStack = controller.backStack.toList()

    LaunchedEffect(logicalStack) {
        manager.sync(logicalStack)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val sortedEntries = manager.entries.sortedBy { it.zIndex }

        sortedEntries.forEach { entry ->
            val isActive = entry.state == VisualState.ACTIVE

            saveableStateHolder.SaveableStateProvider(key = entry.id) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .preventTouchBleed()
                    ) {
                        CompositionLocalProvider(
                            LocalLifecycleOwner provides entry,
                            LocalViewModelStoreOwner provides entry
                        ) {
                            screenContent(entry.route)
                        }
                    }
                }
            }
        }
    }
}
