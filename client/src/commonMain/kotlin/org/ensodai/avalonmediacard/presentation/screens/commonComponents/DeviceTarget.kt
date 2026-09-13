package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

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
