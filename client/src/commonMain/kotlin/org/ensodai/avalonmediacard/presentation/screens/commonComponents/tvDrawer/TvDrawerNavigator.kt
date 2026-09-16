package org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Навигатор правой ТВ-шторки.
 * Управляет стеком экранов [backStack], направлением анимации [isMovingForward]
 * и точкой возврата фокуса [callerFocusRequester].
 */
@Stable
class TvDrawerNavigator {
    var backStack by mutableStateOf<List<TvDrawerScreen>>(emptyList())
        private set

    /** Направление последней навигации: true = вперед (push/open), false = назад (pop) */
    var isMovingForward by mutableStateOf(true)
        private set

    /** Точка возврата фокуса при закрытии всей шторки */
    var callerFocusRequester by mutableStateOf<FocusRequester?>(null)
        private set

    /** Последняя известная точка возврата фокуса (не сбрасывается при clear для надежности при быстрых анимациях) */
    var lastCallerFocusRequester by mutableStateOf<FocusRequester?>(null)
        private set

    val isOpen: Boolean
        get() = backStack.isNotEmpty()

    val currentScreen: TvDrawerScreen?
        get() = backStack.lastOrNull()

    /**
     * Открывает шторку с заданным начальным экраном, очищая предыдущий стек.
     */
    fun open(screen: TvDrawerScreen, caller: FocusRequester? = null) {
        isMovingForward = true
        val targetCaller = caller ?: screen.callerFocusRequester
        callerFocusRequester = targetCaller
        if (targetCaller != null) {
            lastCallerFocusRequester = targetCaller
        }
        backStack = listOf(screen)
    }

    /**
     * Переходит вперед на новый экран внутри текущей открытой шторки.
     */
    fun push(screen: TvDrawerScreen) {
        isMovingForward = true
        backStack = backStack + screen
    }

    /**
     * Возвращается на предыдущий экран в стеке или закрывает шторку, если экран был последним.
     */
    fun pop() {
        if (backStack.size > 1) {
            isMovingForward = false
            val popped = backStack.last()
            backStack = backStack.dropLast(1)
            popped.onDismiss?.invoke()
        } else {
            clear()
        }
    }

    /**
     * Закрывает текущий экран: если стек глубже 1 — делает pop, иначе закрывает шторку.
     */
    fun dismissCurrent() {
        pop()
    }

    /**
     * Полностью закрывает шторку и очищает стек.
     */
    fun clear() {
        isMovingForward = false
        val previousCaller = callerFocusRequester ?: lastCallerFocusRequester
        backStack.asReversed().forEach { it.onDismiss?.invoke() }
        backStack = emptyList()
        callerFocusRequester = null
        previousCaller?.requestFocus()
    }

    internal fun addOrUpdateLegacy(
        id: String,
        title: String,
        subtitle: String?,
        icon: ImageVector?,
        callerFocusRequester: FocusRequester?,
        onDismiss: () -> Unit,
        content: @Composable () -> Unit
    ) {
        val existing = backStack.find { it.key == id }
        if (existing is LegacyTvDrawerScreen) {
            existing.title = title
            existing.subtitle = subtitle
            existing.icon = icon
            existing.callerFocusRequester = callerFocusRequester
            existing.onDismissCallback = onDismiss
            existing.rawContent = content
        } else {
            val newScreen = LegacyTvDrawerScreen(
                key = id,
                initialTitle = title,
                initialSubtitle = subtitle,
                initialIcon = icon,
                initialCallerFocusRequester = callerFocusRequester,
                initialOnDismiss = onDismiss,
                initialContent = content
            )
            if (callerFocusRequester != null) {
                if (this.callerFocusRequester == null) {
                    this.callerFocusRequester = callerFocusRequester
                }
                this.lastCallerFocusRequester = callerFocusRequester
            }
            isMovingForward = true
            backStack = backStack + newScreen
        }
    }

    internal fun removeLegacy(id: String) {
        if (backStack.any { it.key == id }) {
            isMovingForward = false
            backStack = backStack.filter { it.key != id }
            if (backStack.isEmpty()) {
                val caller = callerFocusRequester ?: lastCallerFocusRequester
                caller?.requestFocus()
                callerFocusRequester = null
            }
        }
    }
}

val LocalTvDrawerNavigator = staticCompositionLocalOf { TvDrawerNavigator() }
