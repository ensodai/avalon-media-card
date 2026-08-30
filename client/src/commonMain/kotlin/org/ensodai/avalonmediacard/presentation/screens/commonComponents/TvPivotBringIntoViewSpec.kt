package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(ExperimentalFoundationApi::class)
class TvPivotBringIntoViewSpec(
    private val pivotFraction: Float = 0.35f
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float
    ): Float {
        val targetPivot = containerSize * pivotFraction
        val elementCenter = offset + (size / 2f)
        return elementCenter - targetPivot
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvFocusManagerProvider(
    pivotFraction: Float = 0.35f,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalBringIntoViewSpec provides TvPivotBringIntoViewSpec(pivotFraction = pivotFraction),
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvHorizontalFocusProvider(
    pivotFraction: Float = 0.5f,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalBringIntoViewSpec provides TvPivotBringIntoViewSpec(pivotFraction = pivotFraction),
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
class TvEdgeGatedBringIntoViewSpec(
    private val pivotFraction: Float = 0.35f,
    private val safeViewportFraction: Float = 0.75f
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float
    ): Float {
        val safeThreshold = containerSize * safeViewportFraction
        if (offset >= 0f && (offset + size) <= safeThreshold) {
            return 0f
        }
        val targetPivot = containerSize * pivotFraction
        val elementCenter = offset + (size / 2f)
        return elementCenter - targetPivot
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvEdgeGatedFocusProvider(
    pivotFraction: Float = 0.35f,
    safeViewportFraction: Float = 0.75f,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalBringIntoViewSpec provides TvEdgeGatedBringIntoViewSpec(
            pivotFraction = pivotFraction,
            safeViewportFraction = safeViewportFraction
        ),
        content = content
    )
}
