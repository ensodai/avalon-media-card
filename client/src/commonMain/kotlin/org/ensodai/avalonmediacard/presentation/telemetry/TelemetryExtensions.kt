package org.ensodai.avalonmediacard.presentation.telemetry

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.ClickstreamTargetType
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tracks the Dwell Time for a specific screen/page.
 * Automatically handles recompositions by binding to screenId.
 */
@Composable
fun TrackPageView(
    screenId: String,
    context: ClickstreamContext,
    targetType: ClickstreamTargetType? = null,
    targetId: String? = null
) {
    val telemetry = LocalTelemetryTracker.current

    DisposableEffect(screenId) {
        telemetry.startScreenSession()
        val enterTime = Clock.System.now().toEpochMilliseconds()

        onDispose {
            val leaveTime = Clock.System.now().toEpochMilliseconds()
            val dwellTimeMs = leaveTime - enterTime
            telemetry.logPageView(
                context = context,
                dwellTimeMs = dwellTimeMs,
                targetType = targetType,
                targetId = targetId
            )
        }
    }
}

/**
 * Tracks impressions for items in a LazyRow/LazyColumn.
 * Emits an event if an item remains on screen for at least 1 second.
 * Requires the keys of the items to start with the targetId, separated by '_', e.g. "movieId_index".
 */
@Composable
fun TrackCarouselImpressions(
    lazyListState: LazyListState,
    context: ClickstreamContext,
    debounceMs: Long = 1000L
) {
    val telemetry = LocalTelemetryTracker.current

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                // Extract IDs and types from keys (assuming format "type:id_index" or "id_index")
                val visibleItemsParsed = visibleItems.mapNotNull { itemInfo ->
                    val keyString = itemInfo.key.toString()
                    if (keyString.contains("_")) {
                        val prefix = keyString.substringBefore("_")
                        if (prefix.contains(":")) {
                            val typeStr = prefix.substringBefore(":")
                            val id = prefix.substringAfter(":")
                            val targetType = when (typeStr) {
                                "tv" -> ClickstreamTargetType.MEDIA_TV
                                "person" -> ClickstreamTargetType.PERSON
                                "genre" -> ClickstreamTargetType.GENRE
                                else -> ClickstreamTargetType.MEDIA_MOVIE
                            }
                            org.ensodai.avalonmediacard.contract.model.ClickstreamPayload.ImpressionItem(id, targetType)
                        } else {
                            org.ensodai.avalonmediacard.contract.model.ClickstreamPayload.ImpressionItem(
                                prefix,
                                ClickstreamTargetType.MEDIA_MOVIE
                            )
                        }
                    } else {
                        // skeleton cards have no string keys (they use index), so ignore them
                        null
                    }
                }

                if (visibleItemsParsed.isNotEmpty()) {
                    delay(debounceMs.milliseconds)
                    telemetry.logImpressions(items = visibleItemsParsed, context = context)
                }
            }
    }
}

/**
 * Tracks the scroll depth in a LazyList (like Dashboard or Home feed).
 * Reports when the user scrolls past 25%, 50%, 75%, and 100% of the list.
 */
@Composable
fun TrackScrollDepth(
    lazyListState: LazyListState,
    context: ClickstreamContext
) {
    val telemetry = LocalTelemetryTracker.current
    // Use an array or simple var inside LaunchedEffect instead of remember to avoid recomposition triggers
    LaunchedEffect(lazyListState) {
        var lastReportedDepth = 0.0
        snapshotFlow { lazyListState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems > 0) {
                    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val currentDepth = (lastVisibleItemIndex + 1).toDouble() / totalItems

                    val thresholds = listOf(0.25, 0.50, 0.75, 1.0)
                    val highestPassed = thresholds.lastOrNull { currentDepth >= it } ?: 0.0

                    if (highestPassed > lastReportedDepth) {
                        lastReportedDepth = highestPassed
                        telemetry.logScrollDepth(context = context, maxDepth = lastReportedDepth)
                    }
                }
            }
    }
}
