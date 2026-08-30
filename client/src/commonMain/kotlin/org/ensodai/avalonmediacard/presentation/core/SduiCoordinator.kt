package org.ensodai.avalonmediacard.presentation.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.presentation.navigation.LocalNavigation

val LocalExecutingAction = compositionLocalOf<Action?> { null }

/**
 * Координатор для Server-Driven UI экранов.
 * Вьюмодель просто возвращает [ActionResult], а этот компонент решает, как его показать на экране.
 *
 * @param viewModel Вьюмодель экрана, унаследованная от [SduiViewModel].
 * @param content Компоуз-функция для отрисовки контента. В нее прокидывается коллбэк [dispatch], 
 * который нужно привязать к кликам по кнопкам и слотам на экране.
 */
@Composable
fun SduiCoordinator(
    viewModel: SduiViewModel<*>,
    content: @Composable (dispatch: (Action) -> Unit) -> Unit
) {
    val navigation = LocalNavigation.current
    val uriHandler = LocalUriHandler.current

    val scope = rememberCoroutineScope()
    var executingAction by remember { mutableStateOf<Action?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val dispatch = remember(navigation, uriHandler, viewModel, scope, snackbarHostState) {
        { action: Action ->
            when (action) {
                is ActionNavigate -> navigation.navigateTo(action.screen)
                is ActionOpenUrl -> uriHandler.openUri(action.url)
                is ServerAction -> {
                    scope.launch {
                        executingAction = action
                        try {
                            val result = viewModel.executeServerAction(action)
                            when (result) {
                                is ActionResult.Navigate -> navigation.navigateTo(result.screen)
                                is ActionResult.ExecuteAction -> {
                                    when (val act = result.action) {
                                        is ActionNavigate -> navigation.navigateTo(act.screen)
                                        is ActionOpenUrl -> uriHandler.openUri(act.url)
                                        else -> viewModel.handleLocalAction(act)
                                    }
                                }

                                is ActionResult.ShowNotification -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(result.message)
                                    }
                                }

                                is ActionResult.Error -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(result.message)
                                    }
                                }

                                else -> {}
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            snackbarHostState.showSnackbar(e.message ?: "Action failed")
                            println("ERROR: ${e.message}")
                        } finally {
                            if (executingAction == action) {
                                executingAction = null
                            }
                        }
                    }
                    Unit
                }

                else -> viewModel.handleLocalAction(action)
            }
        }
    }

    CompositionLocalProvider(LocalExecutingAction provides executingAction) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(dispatch)
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
