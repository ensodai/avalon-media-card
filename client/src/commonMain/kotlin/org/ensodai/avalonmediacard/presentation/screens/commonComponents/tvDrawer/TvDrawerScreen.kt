package org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Контракт независимого экрана правой ТВ-шторки.
 */
@Stable
interface TvDrawerScreen {
    val key: String
    val title: String
    val subtitle: String? get() = null
    val icon: ImageVector? get() = null
    val callerFocusRequester: FocusRequester? get() = null
    val initialFocusRequester: FocusRequester? get() = null
    val onDismiss: (() -> Unit)? get() = null

    @Composable
    fun Content(navigator: TvDrawerNavigator)
}

/**
 * Простая реализация экрана шторки для передачи лямбды контента.
 */
class SimpleTvDrawerScreen(
    override val key: String,
    override val title: String,
    override val subtitle: String? = null,
    override val icon: ImageVector? = null,
    override val callerFocusRequester: FocusRequester? = null,
    override val initialFocusRequester: FocusRequester? = null,
    override val onDismiss: (() -> Unit)? = null,
    private val content: @Composable (TvDrawerNavigator) -> Unit
) : TvDrawerScreen {
    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        content(navigator)
    }
}
