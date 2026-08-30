package org.ensodai.avalonmediacard.presentation.telemetry

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.rpc.TelemetryRpcService
import org.ensodai.avalonmediacard.contract.model.*
import org.koin.core.annotation.Single
import kotlin.time.Clock

interface TelemetryTracker {
    fun logClick(
        targetType: ClickstreamTargetType,
        targetId: String,
        context: ClickstreamContext,
        payload: ClickstreamPayload = ClickstreamPayload.Empty
    )

    fun logPageView(
        context: ClickstreamContext,
        dwellTimeMs: Long,
        targetType: ClickstreamTargetType? = null,
        targetId: String? = null
    )

    fun logImpressions(items: List<ClickstreamPayload.ImpressionItem>, context: ClickstreamContext)
    fun logScrollDepth(context: ClickstreamContext, maxDepth: Double)
    fun logSearch(query: String)
    fun logPlaybackStop(targetType: ClickstreamTargetType, targetId: String, completionPercentage: Double)
    fun startScreenSession() // called when screen changes to reset impression caches
}

@Single
class TelemetryTrackerImpl(
    private val telemetryRpcService: TelemetryRpcService
) : TelemetryTracker {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val seenImpressions = mutableSetOf<String>()

    override fun startScreenSession() {
        seenImpressions.clear()
    }

    override fun logSearch(query: String) {
        if (query.isBlank()) return
        scope.launch {
            try {
                telemetryRpcService.logEvent(
                    TelemetryEvent(
                        eventType = ClickstreamEventType.SEARCH,
                        context = ClickstreamContext.SEARCH_PAGE,
                        payload = ClickstreamPayload.SearchEvent(query),
                        timestamp = Clock.System.now()
                    )
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    override fun logPlaybackStop(
        targetType: ClickstreamTargetType,
        targetId: String,
        completionPercentage: Double
    ) {
        scope.launch {
            try {
                telemetryRpcService.logEvent(
                    TelemetryEvent(
                        eventType = ClickstreamEventType.PLAYBACK_STOP,
                        targetType = targetType,
                        targetId = targetId,
                        context = ClickstreamContext.DETAILS_PAGE, // Player opens from Details
                        payload = ClickstreamPayload.PlaybackStop(completionPercentage),
                        timestamp = Clock.System.now()
                    )
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    override fun logClick(
        targetType: ClickstreamTargetType,
        targetId: String,
        context: ClickstreamContext,
        payload: ClickstreamPayload
    ) {
        val event = TelemetryEvent(
            eventType = ClickstreamEventType.CLICK,
            targetType = targetType,
            targetId = targetId,
            context = context,
            payload = payload,
            timestamp = Clock.System.now()
        )
        sendEvent(event)
    }

    override fun logPageView(
        context: ClickstreamContext,
        dwellTimeMs: Long,
        targetType: ClickstreamTargetType?,
        targetId: String?
    ) {
        val event = TelemetryEvent(
            eventType = ClickstreamEventType.PAGE_VIEW,
            targetType = targetType,
            targetId = targetId,
            context = context,
            dwellTimeMs = dwellTimeMs,
            timestamp = Clock.System.now()
        )
        sendEvent(event)
    }

    override fun logImpressions(items: List<ClickstreamPayload.ImpressionItem>, context: ClickstreamContext) {
        val unseen = items.filter { !seenImpressions.contains(it.id) }
        if (unseen.isEmpty()) return

        seenImpressions.addAll(unseen.map { it.id })

        val event = TelemetryEvent(
            eventType = ClickstreamEventType.IMPRESSION_BATCH,
            context = context,
            payload = ClickstreamPayload.ImpressionBatch(unseen),
            timestamp = Clock.System.now()
        )
        sendEvent(event)
    }

    override fun logScrollDepth(context: ClickstreamContext, maxDepth: Double) {
        val event = TelemetryEvent(
            eventType = ClickstreamEventType.SCROLL,
            context = context,
            payload = ClickstreamPayload.ScrollDepth(maxDepth),
            timestamp = Clock.System.now()
        )
        sendEvent(event)
    }

    private fun sendEvent(event: TelemetryEvent) {
        scope.launch {
            try {
                telemetryRpcService.logEvent(event)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Ignore telemetry errors
                e.printStackTrace()
            }
        }
    }
}

val LocalTelemetryTracker = compositionLocalOf<TelemetryTracker> {
    error("No TelemetryTracker provided")
}
