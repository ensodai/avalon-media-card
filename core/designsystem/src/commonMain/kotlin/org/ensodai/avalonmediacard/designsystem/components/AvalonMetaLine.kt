package org.ensodai.avalonmediacard.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

object AvalonMetaGlyphs {
    const val INTERPUNCT = "\u00B7"
    const val NNBSP = "\u202F" // Narrow No-Break Space (★ 8.6, 1 ч 27 мин)
    const val STAR = "\u2605"
}

@Immutable
sealed interface MetaSegment {
    @Immutable
    data class Rating(val value: String) : MetaSegment

    @Immutable
    data class Fact(val text: String) : MetaSegment

    @Immutable
    data class StatusDot(
        val label: String,
        val tone: StatusTone = StatusTone.Live
    ) : MetaSegment
}

enum class StatusTone(internal val inlineId: String) {
    Live("avalon_dot_live"),
    Warn("avalon_dot_warn"),
    Danger("avalon_dot_danger")
}

/**
 * Canonical metadata line without badge soup:
 * ★ 8.6 · 2022–present · 2 seasons · Series · ● Ongoing
 */
@Composable
fun AvalonMetaLine(
    segments: List<MetaSegment>,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    onOverlaySurface: Boolean = false
) {
    val colors = AvalonTheme.colors
    val style = AvalonTheme.typography.metadata
    val separatorColor = if (onOverlaySurface) colors.textMutedOnOverlay else colors.textMuted

    val annotated = remember(segments, colors, separatorColor) {
        buildAnnotatedString {
            segments.forEachIndexed { index, segment ->
                if (index > 0) {
                    withStyle(SpanStyle(color = separatorColor)) {
                        append(" ${AvalonMetaGlyphs.INTERPUNCT} ")
                    }
                }
                when (segment) {
                    is MetaSegment.Rating -> {
                        withStyle(SpanStyle(color = colors.accentGold, fontWeight = FontWeight.Medium)) {
                            append(AvalonMetaGlyphs.STAR)
                        }
                        append(AvalonMetaGlyphs.NNBSP)
                        withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Medium)) {
                            append(segment.value)
                        }
                    }
                    is MetaSegment.Fact -> {
                        withStyle(SpanStyle(color = colors.textSecondary)) {
                            append(segment.text)
                        }
                    }
                    is MetaSegment.StatusDot -> {
                        appendInlineContent(id = segment.tone.inlineId, alternateText = "●")
                        append(AvalonMetaGlyphs.NNBSP)
                        withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Medium)) {
                            append(segment.label)
                        }
                    }
                }
            }
        }
    }

    val isTv = AvalonTheme.isTv
    val circleShape = AvalonTheme.shapes.circle

    val inlineContent = remember(colors, isTv, circleShape) {
        val boxSp = if (isTv) 14.sp else 11.sp
        val dotSize = if (isTv) 8.dp else 6.dp
        val haloSize = if (isTv) 12.dp else 9.dp

        fun dotEntry(dotColor: Color) = InlineTextContent(
            placeholder = Placeholder(
                width = boxSp,
                height = boxSp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(haloSize)
                        .clip(circleShape)
                        .background(dotColor.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(circleShape)
                            .background(dotColor)
                    )
                }
            }
        }

        mapOf(
            StatusTone.Live.inlineId to dotEntry(colors.statusLive),
            StatusTone.Warn.inlineId to dotEntry(colors.statusWarn),
            StatusTone.Danger.inlineId to dotEntry(colors.statusDanger)
        )
    }

    BasicText(
        text = annotated,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        inlineContent = inlineContent,
        modifier = modifier
    )
}
