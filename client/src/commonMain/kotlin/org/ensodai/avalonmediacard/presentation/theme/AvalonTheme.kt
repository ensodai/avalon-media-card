package org.ensodai.avalonmediacard.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import org.ensodai.avalonmediacard.designsystem.theme.AvalonColors as DsColors
import org.ensodai.avalonmediacard.designsystem.theme.AvalonElevation as DsElevation
import org.ensodai.avalonmediacard.designsystem.theme.AvalonShapes as DsShapes
import org.ensodai.avalonmediacard.designsystem.theme.AvalonSpacing as DsSpacing
import org.ensodai.avalonmediacard.designsystem.theme.ObsidianCinemaDarkColors
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget

typealias AvalonColors = DsColors
typealias AvalonShapes = DsShapes
typealias AvalonSpacing = DsSpacing
typealias AvalonElevation = DsElevation

object AvalonTheme {
    val colors: AvalonColors = ObsidianCinemaDarkColors
    val shapes: AvalonShapes = DsShapes()
    val spacing: AvalonSpacing = DsSpacing()
    val elevation: AvalonElevation = DsElevation()

    val isTv: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalDeviceTarget.current.isTv

    val typography: AvalonTypography
        @Composable
        @ReadOnlyComposable
        get() = if (LocalDeviceTarget.current.isTv) AvalonTypographyTv else AvalonTypographyDesktop
}
