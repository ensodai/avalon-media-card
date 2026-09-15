package org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector

typealias TvDrawerEntry = TvDrawerScreen
typealias TvDrawerState = TvDrawerNavigator

val LocalTvDrawerState = LocalTvDrawerNavigator

/**
 * Внутренний адаптер для поддержки легаси вызовов TvDrawerEffect.
 */
internal class LegacyTvDrawerScreen(
    override val key: String,
    initialTitle: String,
    initialSubtitle: String?,
    initialIcon: ImageVector?,
    initialCallerFocusRequester: FocusRequester?,
    initialOnDismiss: () -> Unit,
    initialContent: @Composable () -> Unit
) : TvDrawerScreen {
    override var title by mutableStateOf(initialTitle)
    override var subtitle by mutableStateOf(initialSubtitle)
    override var icon by mutableStateOf(initialIcon)
    override var callerFocusRequester by mutableStateOf(initialCallerFocusRequester)
    var onDismissCallback by mutableStateOf(initialOnDismiss)
    var rawContent by mutableStateOf(initialContent)

    override val onDismiss: () -> Unit
        get() = { onDismissCallback() }

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        rawContent()
    }
}

/**
 * Декларативный мост к глобальной ТВ-шторке со встроенной стековой навигацией.
 * Обеспечивает обратную совместимость с существующими вызовами.
 */
@Composable
fun TvDrawerEffect(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    callerFocusRequester: FocusRequester? = null,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = LocalTvDrawerNavigator.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentContent by rememberUpdatedState(content)

    val effectId = remember { kotlin.random.Random.nextInt().toString() }

    DisposableEffect(effectId) {
        drawerState.addOrUpdateLegacy(
            id = effectId,
            title = title,
            subtitle = subtitle,
            icon = icon,
            callerFocusRequester = callerFocusRequester,
            onDismiss = { currentOnDismiss() },
            content = { currentContent() }
        )
        onDispose { drawerState.removeLegacy(effectId) }
    }

    SideEffect {
        drawerState.addOrUpdateLegacy(
            id = effectId,
            title = title,
            subtitle = subtitle,
            icon = icon,
            callerFocusRequester = callerFocusRequester,
            onDismiss = { currentOnDismiss() },
            content = { currentContent() }
        )
    }
}
