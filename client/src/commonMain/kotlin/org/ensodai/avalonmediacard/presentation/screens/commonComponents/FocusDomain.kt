package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import org.ensodai.avalonmediacard.contract.logging.AppLogging

private val logger = AppLogging.logger("FocusTracker")

val LocalContentFocusRequester = compositionLocalOf { FocusRequester() }

/**
 * Отслеживание и логирование состояния фокуса элемента в реальном времени.
 */
fun Modifier.logFocus(tag: String): Modifier = this.onFocusChanged { state ->
    if (state.isFocused) {
        logger.d { "🎯 [FOCUS] '$tag' -> ПОЛУЧИЛ ФОКУС (isFocused=true)" }
    } else if (state.hasFocus) {
        logger.d { "📦 [FOCUS] '$tag' -> СОДЕРЖИТ ФОКУС ВНУТРИ (hasFocus=true)" }
    } else {
        logger.d { "⚪ [FOCUS] '$tag' -> ПОТЕРЯЛ ФОКУС" }
    }
}

/**
 * Автоматический первичный захват фокуса при монтировании экрана на TV.
 * Выполняется один раз при первом кадре раскладки ([Modifier.onPlaced]).
 */
fun Modifier.initialFocus(
    focusRequester: FocusRequester?,
    enabled: Boolean = true
): Modifier = composed {
    if (focusRequester == null || !enabled) return@composed this

    var hasRequested by rememberSaveable { mutableStateOf(false) }
    val deviceTarget = LocalDeviceTarget.current
    val shouldFocus = deviceTarget.isTv || !deviceTarget.isTouch

    if (shouldFocus) {
        this
            .focusRequester(focusRequester)
            .onPlaced {
                if (!hasRequested) {
                    hasRequested = true
                    logger.d { "🚀 [INITIAL_FOCUS] Запрашиваем первичный фокус" }
                    runCatching { focusRequester.requestFocus() }
                }
            }
    } else {
        this
    }
}

/**
 * Топологический Фокусный Домен (Focus Domain).
 * Изолирует зону экрана (сайдбар, экран контента, оверлей) и управляет входом/выходом фокуса:
 * - При выходе (`onExit`): сохраняет активного ребенка через [FocusRequester.saveFocusedChild].
 * - При входе (`onEnter`): восстанавливает активного ребенка через [FocusRequester.restoreFocusedChild],
 *   а если его не было — направляет фокус на [fallbackRequester].
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.focusDomain(
    domainRequester: FocusRequester,
    fallbackRequester: FocusRequester? = null
): Modifier = this
    .focusRequester(domainRequester)
    .focusProperties {
        onEnter = {
            logger.d { "🌀 [FOCUS_DOMAIN] onEnter -> попытка restoreFocusedChild()..." }
            val isRestored = domainRequester.restoreFocusedChild()
            logger.d { "🌀 [FOCUS_DOMAIN] restoreFocusedChild() = $isRestored" }
            if (!isRestored && fallbackRequester != null) {
                logger.d { "🌀 [FOCUS_DOMAIN] фоллбэк на fallbackRequester.requestFocus()" }
                fallbackRequester.requestFocus()
            }
        }
        onExit = {
            logger.d { "🌀 [FOCUS_DOMAIN] onExit -> сохранение активного ребенка..." }
            val isSaved = domainRequester.saveFocusedChild()
            logger.d { "🌀 [FOCUS_DOMAIN] saveFocusedChild() = $isSaved" }
        }
    }
    .focusGroup()

/**
 * Фокусный домен Сайдбара (Navigation Rail).
 * При попытке фокуса выйти вправо гарантированно направляет фокус в [contentRequester],
 * минуя слепой 2D-поиск.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.navigationDomain(
    sidebarRequester: FocusRequester,
    contentRequester: FocusRequester
): Modifier = this
    .focusRequester(sidebarRequester)
    .focusProperties {
        onExit = {
            logger.d { "🧭 [NAV_DOMAIN] onExit requestedFocusDirection=$requestedFocusDirection" }
            if (requestedFocusDirection == FocusDirection.Right) {
                logger.d { "🧭 [NAV_DOMAIN] выход вправо -> направляем в contentRequester" }
                contentRequester.requestFocus()
                cancelFocusChange()
            }
        }
    }
    .focusGroup()
