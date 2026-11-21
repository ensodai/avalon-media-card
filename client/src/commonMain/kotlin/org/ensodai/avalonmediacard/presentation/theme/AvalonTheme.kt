package org.ensodai.avalonmediacard.presentation.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Avalon MediaCard Design Tokens («Obsidian Cinema»).
 * Canonical specification declared in DESIGN.md in the project root.
 */
object AvalonColors {
    // Surfaces (Luminance Elevation Ladder)
    val bgPureCinema = Color(0xFF000000) // Strictly fullscreen video playback
    val bgSidebar = Color(0xFF08080A)    // Navigation rail / drawer
    val bgCanvas = Color(0xFF0B0B0D)     // Root screen canvas
    val bgSurface = Color(0xFF131316)    // Level 1: Bento cards & panels
    val bgElevated = Color(0xFF1B1B1F)   // Level 2: Interactive buttons, hover, segmented controls, inputs
    val bgOverlay = Color(0xFF242429)    // Level 3: Modals, dropdowns, tooltips

    // Borders
    val borderSubtle = Color(0xFF232328) // 1dp default card border & dividers
    val borderStrong = Color(0xFF34343C) // 1dp hover / active input border
    val borderFocus = Color(0xFFF2F2F4)  // 2dp TV / D-pad focus border

    // Typography
    val textPrimary = Color(0xFFF2F2F4)   // Headings, titles, KPI numbers, primary buttons
    val textSecondary = Color(0xFF9C9CA6) // Synopses, metadata line, subtitles
    val textMuted = Color(0xFF82828E)     // Axes labels, timecodes, footnotes, inactive nav items

    // Accents & Semantics (Strictly scoped, <= 2% budget)
    val accentPrimary = Color(0xFFF2F2F4) // Primary CTA fill (text is bgCanvas #0B0B0D)
    val accentGold = Color(0xFFE5B85B)    // Strictly ratings and stars (★)
    val statusLive = Color(0xFF6BCB8B)    // Strictly 6dp dot for Ongoing series / Live Watch Party
    val statusWarn = Color(0xFFE09F5A)    // Strictly Paused / buffer warning
    val statusDanger = Color(0xFFE06C6C)  // Strictly Canceled / Dropped / Destructive

    // Charts (Edward Tufte Monochrome)
    val chartTrack = Color(0xFF232328)      // Empty track / empty heatmap cell
    val chartBarDefault = Color(0xFF6E6E78) // Default bars (genres, eras, ratings)
    val chartBarPeak = Color(0xFFF2F2F4)    // #1 Leader bar / peak day
}

object AvalonShapes {
    val micro = RoundedCornerShape(4.dp)      // Tech tags (4K, HDR), progress bars, heatmap cells
    val control = RoundedCornerShape(10.dp)   // ALL buttons, inputs, segmented controls, filter chips
    val media = RoundedCornerShape(12.dp)     // Posters (2:3), 16:9 cards
    val container = RoundedCornerShape(16.dp) // Bento grid containers, dialogs
    val circle = CircleShape                  // Strictly 1:1 person avatars and 6dp status dots
}

object AvalonSpacing {
    val space2 = 2.dp
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space32 = 32.dp
    val space48 = 48.dp
}

object AvalonTheme {
    val colors: AvalonColors = AvalonColors
    val shapes: AvalonShapes = AvalonShapes
    val spacing: AvalonSpacing = AvalonSpacing
}
