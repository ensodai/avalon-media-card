package org.ensodai.avalonmediacard.core.player.watchparty

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.ClockSyncPing
import org.ensodai.avalonmediacard.contract.model.ClockSyncPong
import org.ensodai.avalonmediacard.contract.rpc.WatchPartyRpcService
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Сервис SNTP-синхронизации часов между клиентом и сервером.
 * Реализует 4-точечный замер RTT, медианную фильтрацию по минимальному RTT
 * и экспоненциальное скользящее среднее (EMA, alpha = 0.2) для компенсации джиттера.
 */
@Single
class ClockSyncService(
    private val rpcService: WatchPartyRpcService
) {
    private val logger = AppLogging.logger("ClockSyncService")

    internal var clockProvider: () -> Instant = { Clock.System.now() }
    internal var pingServerOverride: (suspend (ClockSyncPing) -> ClockSyncPong)? = null

    var offset: Duration? = null
        private set

    var isSynchronized: Boolean = false
        private set

    private var syncJob: Job? = null

    data class ClockSample(
        val rtt: Duration,
        val theta: Duration
    )

    /**
     * Выполняет единичный 4-точечный замер RTT и смещения.
     */
    suspend fun measureSample(): ClockSample {
        val t1 = clockProvider()
        val ping = ClockSyncPing(clientSendTime = t1)
        val pong = pingServerOverride?.invoke(ping) ?: rpcService.syncClock(ping)
        val t4 = clockProvider()

        val t2 = pong.serverReceiveTime
        val t3 = pong.serverTransmitTime

        val rtt = (t4 - t1) - (t3 - t2)
        val theta = ((t2 - t1) + (t3 - t4)) / 2

        return ClockSample(rtt = rtt, theta = theta)
    }

    /**
     * Выполняет серию замеров, отбирает лучший по минимальному RTT и обновляет смещение через EMA.
     */
    suspend fun sync(samplesCount: Int = 5) {
        val samples = mutableListOf<ClockSample>()
        for (i in 0 until samplesCount) {
            try {
                samples.add(measureSample())
                if (i < samplesCount - 1) {
                    delay(50.milliseconds)
                }
            } catch (e: Exception) {
                logger.w(e) { "Failed clock sync sample $i" }
            }
        }

        if (samples.isEmpty()) return

        // Фильтр: берем замер с минимальным RTT (самый чистый маршрут без очередей в роутерах)
        val bestSample = samples.minByOrNull { it.rtt } ?: return

        val currentOffset = offset
        if (currentOffset == null) {
            offset = bestSample.theta
        } else {
            // Экспоненциальное сглаживание (EMA, alpha = 0.2)
            val prevMs = currentOffset.inWholeMilliseconds
            val rawMs = bestSample.theta.inWholeMilliseconds
            val filteredMs = (0.8 * prevMs + 0.2 * rawMs).toLong()
            offset = filteredMs.milliseconds
        }

        isSynchronized = true
        logger.i { "[TrueSync:SNTP] Clock synchronized: offset = ${offset?.inWholeMilliseconds} ms (RTT = ${bestSample.rtt.inWholeMilliseconds} ms)" }
    }

    /**
     * Возвращает текущее расчетное время сервера.
     */
    fun estimatedServerTime(): Instant {
        val currentOffset = offset ?: Duration.ZERO
        return clockProvider() + currentOffset
    }

    /**
     * Запускает периодическую синхронизацию в фоне.
     */
    fun startPeriodicSync(scope: CoroutineScope, interval: Duration = 30.seconds): Job {
        stopPeriodicSync()
        val job = scope.launch {
            while (isActive) {
                sync(samplesCount = 3)
                delay(interval)
            }
        }
        syncJob = job
        return job
    }

    /**
     * Останавливает фоновую синхронизацию.
     */
    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
    }

    /**
     * Сброс состояния синхронизации.
     */
    fun reset() {
        stopPeriodicSync()
        offset = null
        isSynchronized = false
    }
}
