package org.ensodai.avalonmediacard.presentation.navigation


import androidx.compose.runtime.staticCompositionLocalOf
import org.ensodai.avalonmediacard.contract.ui.navigation.Navigation

val LocalAvalonNavController = staticCompositionLocalOf<AvalonNavController<ScreenKey>> {
    error("No AvalonNavController provided. Make sure you wrapped your UI in CompositionLocalProvider(LocalAvalonNavController provides ...)")
}

val LocalNavigation = staticCompositionLocalOf<Navigation> {
    error("No Navigation provided. Make sure you wrapped your UI in CompositionLocalProvider(LocalNavigation provides ...)")
}
