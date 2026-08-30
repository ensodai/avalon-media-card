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
