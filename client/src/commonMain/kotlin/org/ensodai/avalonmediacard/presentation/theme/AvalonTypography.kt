package org.ensodai.avalonmediacard.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Avalon MediaCard Typography Matrix («Obsidian Cinema» v2.1).
 * Mathematical Minor Third (1.200) scale calibrated for dark cinematic UI.
 * Fully compliant with DESIGN.md Section 4.
 */
@Immutable
data class AvalonTypography(
    /** Title of movie/show on Hero banner ("Alien", "Succession") */
    val displayHero: TextStyle,
    /** Primary KPI numbers in stats dashboard ("33 дн.", "11.3 ч") */
    val kpiHero: TextStyle,
    /** Page titles ("Статистика синефила", "Каталог") */
    val pageTitle: TextStyle,
    /** Secondary KPI tiles ("24", "351", "51"), archetype name */
    val kpiTile: TextStyle,
    /** Section & Bento card titles in Sentence case ("Библиотека и прогресс", "Сезоны и серии") */
    val sectionTitle: TextStyle,
    /** Titles under posters, episode titles ("1. Юбилей"), person names */
    val cardTitle: TextStyle,
    /** Movie synopses, episode descriptions (max width 560dp) */
    val bodySynopsis: TextStyle,
    /** Buttons ("Смотреть", "Отметить сезон"), tabs, sidebar items, status rows */
    val uiLabel: TextStyle,
    /** Metadata strings ("★ 8.3 · 2023 · 4 сезона"), human dates ("3 июня 2018"), subtitles */
    val metadata: TextStyle,
    /** Graph axis labels (1..10), calendar heatmap days (Пн/Ср/Пт), legends */
    val microCaption: TextStyle,
    /** Technical tags (4K HDR10), duration on frame ("58 мин"), player timecodes */
    val monoTech: TextStyle
) {
    fun toMaterialTypography(): Typography = Typography(
        displayMedium = displayHero,
        displaySmall = kpiHero,
        headlineMedium = pageTitle,
        headlineSmall = kpiTile,
        titleMedium = sectionTitle,
        titleSmall = cardTitle,
        bodyLarge = bodySynopsis,
        bodyMedium = metadata,
        bodySmall = microCaption,
        labelLarge = uiLabel,
        labelMedium = microCaption.copy(fontWeight = FontWeight.Medium),
        labelSmall = monoTech
    )
}

/**
 * Desktop & Web typography matrix (viewing distance 50–70 cm).
 */
val AvalonTypographyDesktop = AvalonTypography(
    displayHero = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.90).sp
    ),
    kpiHero = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.64).sp,
        fontFeatureSettings = "tnum"
    ),
    pageTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.48).sp
    ),
    kpiTile = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.33).sp,
        fontFeatureSettings = "tnum"
    ),
    sectionTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.18).sp
    ),
    cardTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySynopsis = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.07.sp
    ),
    uiLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.07.sp,
        fontFeatureSettings = "tnum"
    ),
    metadata = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.13.sp,
        fontFeatureSettings = "tnum"
    ),
    microCaption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.18.sp,
        fontFeatureSettings = "tnum"
    ),
    monoTech = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.36.sp,
        fontFeatureSettings = "tnum"
    )
)

/**
 * Smart TV 10-foot UI typography matrix (viewing distance 2.5–3.5 m, scaled ~+30%).
 * Strictly obeys minimum 16sp rule for readability from couch.
 */
val AvalonTypographyTv = AvalonTypography(
    displayHero = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.88).sp
    ),
    kpiHero = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.60).sp,
        fontFeatureSettings = "tnum"
    ),
    pageTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.48).sp
    ),
    kpiTile = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.28).sp,
        fontFeatureSettings = "tnum"
    ),
    sectionTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.12).sp
    ),
    cardTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodySynopsis = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.10.sp
    ),
    uiLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.09.sp,
        fontFeatureSettings = "tnum"
    ),
    metadata = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.17.sp,
        fontFeatureSettings = "tnum"
    ),
    microCaption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.16.sp,
        fontFeatureSettings = "tnum"
    ),
    monoTech = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.40.sp,
        fontFeatureSettings = "tnum"
    )
)
