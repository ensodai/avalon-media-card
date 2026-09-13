package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

enum class DeviceTarget {
    DESKTOP_WEB,
    TV_WEB,
    ANDROID_MOBILE,
    TABLET,
    ANDROID_TV;

    val isTv: Boolean get() = this == TV_WEB || this == ANDROID_TV
    val isDesktop: Boolean get() = this == DESKTOP_WEB
    val isPhone: Boolean get() = this == ANDROID_MOBILE
    val isTablet: Boolean get() = this == TABLET
    val isTouch: Boolean get() = this == ANDROID_MOBILE || this == TABLET
}

val LocalDeviceTarget = staticCompositionLocalOf<DeviceTarget> { DeviceTarget.DESKTOP_WEB }
val LocalRootOverlay = staticCompositionLocalOf<MutableState<(@Composable () -> Unit)?>> {
    mutableStateOf(null)
}

/**
 * Универсальный декларативный компонент выбора разметки под целевое устройство:
 * - [tv]: Android TV и веб в ТВ-режиме (TV_WEB) с управлением пультом/D-Pad
 * - [web]: ПК / Десктопный браузер с мышью и клавиатурой (DESKTOP_WEB)
 * - [mobile]: Мобильные телефоны (тач-интерфейс)
 * - [tablet]: Планшеты (если не указан, фоллбечится на [web], затем [default])
 * - [default]: Базовый контент, если специализированный таргет еще не реализован
 */
@Composable
fun AdaptiveLayout(
    tv: (@Composable () -> Unit)? = null,
    web: (@Composable () -> Unit)? = null,
    mobile: (@Composable () -> Unit)? = null,
    tablet: (@Composable () -> Unit)? = web,
    default: @Composable () -> Unit
) {
    val device = LocalDeviceTarget.current
    when {
        device.isTv && tv != null -> tv()
        device.isPhone && mobile != null -> mobile()
        device.isTablet && tablet != null -> tablet()
        device.isDesktop && web != null -> web()
        else -> default()
    }
}

/**
 * Адаптивный провайдер плотности экрана под ТВ-сетку.
 * При [isTv] = true масштабирует плотность интерфейса относительно эталонной высоты [baselineHeightDp]
 * (по умолчанию 540dp, соответствующей эталону Android TV 4K / 1080p при 16:9),
 * обеспечивая идентичный размер карточек, текста и отступов на любых разрешениях экрана.
 */
@Composable
fun ProvideAdaptiveTvDensity(
    isTv: Boolean,
    baselineHeightDp: Float = 540f,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val currentDensity = LocalDensity.current
        val tvDensity = remember(currentDensity, isTv, maxHeight) {
            if (isTv && maxHeight.value > 0f) {
                val heightPx = with(currentDensity) { maxHeight.toPx() }
                val targetDensity = (heightPx / baselineHeightDp).coerceAtLeast(1.0f)
                Density(density = targetDensity, fontScale = currentDensity.fontScale)
            } else {
                currentDensity
            }
        }
        CompositionLocalProvider(
            LocalDensity provides tvDensity,
            content = content
        )
    }
}
