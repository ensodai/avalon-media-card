package org.ensodai.avalonmediacard.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Level 1: Global Primitive Tokens (strictly internal).
 * Raw immutable hex palette for Obsidian Cinema.
 */
internal object AvalonPalette {
    val PureBlack = Color(0xFF000000)
    val Obsidian980 = Color(0xFF08080A)
    val Obsidian950 = Color(0xFF0B0B0D)
    val Obsidian900 = Color(0xFF131316)
    val Obsidian850 = Color(0xFF1B1B1F)
    val Obsidian800 = Color(0xFF242429)
    val Obsidian780 = Color(0xFF26262C)
    val Obsidian740 = Color(0xFF323235)
    val Graphite750 = Color(0xFF232328)
    val Graphite650 = Color(0xFF34343C)
    val Graphite500 = Color(0xFF6E6E78)
    val Porcelain100 = Color(0xFFF2F2F4)
    val Porcelain150 = Color(0xFFDCDCE0)
    val Porcelain200 = Color(0xFFD0D0D8)
    val Zinc400 = Color(0xFF9C9CA6)
    val Zinc450 = Color(0xFF8C8C98)
    val Zinc500 = Color(0xFF82828E)
    val Gold400 = Color(0xFFE5B85B)
    val Emerald400 = Color(0xFF6BCB8B)
    val Amber400 = Color(0xFFE09F5A)
    val Coral400 = Color(0xFFE06C6C)
    val White = Color(0xFFFFFFFF)
}

/**
 * Level 2: Semantic Tokens (Public Contract).
 */
@Immutable
data class AvalonColors(
    val bgPureCinema: Color,          // #000000 — strictly fullscreen video player
    val bgSidebar: Color,             // #08080A — sidebar navigation (below canvas)
    val bgCanvas: Color,              // #0B0B0D — root screen canvas
    val bgSurface: Color,             // #131316 — level 1: Bento cards, panels
    val bgElevated: Color,            // #1B1B1F — level 2: secondary buttons, tabs, inputs
    val bgElevatedHover: Color,       // #26262C — tonal elevation for hover on PC
    val bgElevatedPressed: Color,     // #323235 — tactile press
    val bgOverlay: Color,             // #242429 — level 3: modals, dropdowns, tooltips
    val borderSubtle: Color,          // #232328 — 1dp hairline border on canvas/surface
    val borderStrong: Color,          // #34343C — hover border for cards/active input
    val borderOverlay: Color,         // white@9% — visible border on bgOverlay
    val borderOverlayHighlight: Color,// white@8% — top 1dp sheen (Linear style)
    val borderFocus: Color,           // #F2F2F4 — 2dp TV / D-pad focus ring
    val mediaHairline: Color,         // white@10% — optical inner contour for posters
    val mediaRestingVeil: Color,      // black@6% — resting darkening veil on media
    val stateLayerHover: Color,       // white@6% — hover elevation for ghost controls
    val stateLayerPressed: Color,     // white@10% — press feedback
    val textPrimary: Color,           // #F2F2F4 — titles, headlines, primary button text
    val textBody: Color,              // #D0D0D8 — multi-line synopses (APCA Lc -78)
    val textSecondary: Color,         // #9C9CA6 — metadata, subtitles, secondary labels
    val textMuted: Color,             // #82828E — separators (·), timecodes, footnotes
    val textMutedOnOverlay: Color,    // #8C8C98 — safe muted text on bgOverlay (4.65:1)
    val accentPrimary: Color,         // #F2F2F4 — Primary CTA fill
    val accentPrimaryHover: Color,    // #FFFFFF — Primary CTA hover
    val accentPrimaryPressed: Color,  // #DCDCE0 — Primary CTA press
    val onAccentPrimary: Color,       // #0B0B0D — Text inside Primary CTA
    val accentGold: Color,            // #E5B85B — strictly ratings and stars (★)
    val statusLive: Color,            // #6BCB8B — strictly 6dp ongoing dot / live watch party
    val statusWarn: Color,            // #E09F5A — pause / warning
    val statusDanger: Color,          // #E06C6C — canceled / dropped / destructive
    val chartTrack: Color,            // #232328 — progress track & monogram background
    val chartBarDefault: Color,       // #6E6E78 — default chart bars
    val chartBarPeak: Color           // #F2F2F4 — #1 leader bar
)

val ObsidianCinemaDarkColors = AvalonColors(
    bgPureCinema = AvalonPalette.PureBlack,
    bgSidebar = AvalonPalette.Obsidian980,
    bgCanvas = AvalonPalette.Obsidian950,
    bgSurface = AvalonPalette.Obsidian900,
    bgElevated = AvalonPalette.Obsidian850,
    bgElevatedHover = AvalonPalette.Obsidian780,
    bgElevatedPressed = AvalonPalette.Obsidian740,
    bgOverlay = AvalonPalette.Obsidian800,
    borderSubtle = AvalonPalette.Graphite750,
    borderStrong = AvalonPalette.Graphite650,
    borderOverlay = AvalonPalette.White.copy(alpha = 0.09f),
    borderOverlayHighlight = AvalonPalette.White.copy(alpha = 0.08f),
    borderFocus = AvalonPalette.Porcelain100,
    mediaHairline = AvalonPalette.White.copy(alpha = 0.10f),
    mediaRestingVeil = AvalonPalette.PureBlack.copy(alpha = 0.06f),
    stateLayerHover = AvalonPalette.White.copy(alpha = 0.06f),
    stateLayerPressed = AvalonPalette.White.copy(alpha = 0.10f),
    textPrimary = AvalonPalette.Porcelain100,
    textBody = AvalonPalette.Porcelain200,
    textSecondary = AvalonPalette.Zinc400,
    textMuted = AvalonPalette.Zinc500,
    textMutedOnOverlay = AvalonPalette.Zinc450,
    accentPrimary = AvalonPalette.Porcelain100,
    accentPrimaryHover = AvalonPalette.White,
    accentPrimaryPressed = AvalonPalette.Porcelain150,
    onAccentPrimary = AvalonPalette.Obsidian950,
    accentGold = AvalonPalette.Gold400,
    statusLive = AvalonPalette.Emerald400,
    statusWarn = AvalonPalette.Amber400,
    statusDanger = AvalonPalette.Coral400,
    chartTrack = AvalonPalette.Graphite750,
    chartBarDefault = AvalonPalette.Graphite500,
    chartBarPeak = AvalonPalette.Porcelain100
)

@Immutable
data class AvalonShapes(
    val micro: RoundedCornerShape = RoundedCornerShape(4.dp),       // 4K/HDR badges, progress bars, timecodes
    val segmentThumb: RoundedCornerShape = RoundedCornerShape(8.dp),// concentric tab thumb (12 - 4 = 8dp)
    val control: RoundedCornerShape = RoundedCornerShape(10.dp),    // ALL buttons, inputs, menu items, chips
    val media: RoundedCornerShape = RoundedCornerShape(12.dp),      // Posters 2:3, episodes 16:9, tab track
    val container: RoundedCornerShape = RoundedCornerShape(16.dp),  // Bento cards, dialogs, drawers
    val circle: Shape = CircleShape                                 // 1:1 person avatars & status dots
)

@Immutable
data class AvalonSpacing(
    val space2: Dp = 2.dp,
    val space4: Dp = 4.dp,
    val space8: Dp = 8.dp,
    val space12: Dp = 12.dp,
    val space16: Dp = 16.dp,
    val space20: Dp = 20.dp,
    val space24: Dp = 24.dp,
    val space32: Dp = 32.dp,
    val space48: Dp = 48.dp,
    val screenHorizontal: Dp = 24.dp,
    val screenVertical: Dp = 24.dp
)

@Immutable
data class AvalonElevation(
    val none: Dp = 0.dp,
    val borderWidth: Dp = 1.dp,
    val focusBorderWidth: Dp = 2.dp,
    val focusRingOffset: Dp = 2.dp
)

val LocalAvalonColors = staticCompositionLocalOf { ObsidianCinemaDarkColors }
val LocalAvalonShapes = staticCompositionLocalOf { AvalonShapes() }
val LocalAvalonSpacing = staticCompositionLocalOf { AvalonSpacing() }
val LocalAvalonElevation = staticCompositionLocalOf { AvalonElevation() }
val LocalAvalonIsTv = staticCompositionLocalOf { false }
val LocalAvalonContentColor = compositionLocalOf { ObsidianCinemaDarkColors.textPrimary }
val LocalAvalonTextStyle = compositionLocalOf { TextStyle.Default }

object AvalonTheme {
    val colors: AvalonColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAvalonColors.current

    val shapes: AvalonShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalAvalonShapes.current

    val spacing: AvalonSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAvalonSpacing.current

    val elevation: AvalonElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalAvalonElevation.current

    val isTv: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalAvalonIsTv.current

    val typography: AvalonTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAvalonTypography.current
}

@Composable
fun AvalonDesignSystemTheme(
    isTv: Boolean = false,
    colors: AvalonColors = ObsidianCinemaDarkColors,
    content: @Composable () -> Unit
) {
    val typography = if (isTv) AvalonTypographyTv else AvalonTypographyDesktop
    val spacing = if (isTv) {
        AvalonSpacing(screenHorizontal = 48.dp, screenVertical = 27.dp)
    } else {
        AvalonSpacing()
    }

    CompositionLocalProvider(
        LocalAvalonColors provides colors,
        LocalAvalonShapes provides remember { AvalonShapes() },
        LocalAvalonSpacing provides spacing,
        LocalAvalonElevation provides remember { AvalonElevation() },
        LocalAvalonIsTv provides isTv,
        LocalAvalonTypography provides typography,
        LocalAvalonContentColor provides colors.textPrimary,
        content = content
    )
}
