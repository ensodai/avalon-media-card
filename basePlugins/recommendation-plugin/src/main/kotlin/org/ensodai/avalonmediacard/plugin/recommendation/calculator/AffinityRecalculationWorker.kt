package org.ensodai.avalonmediacard.plugin.recommendation.calculator

import kotlinx.coroutines.*
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class AffinityRecalculationWorker(
    private val context: PluginContext,
    private val calculator: AffinityVectorCalculator
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        context.logger.info("Starting AffinityRecalculationWorker background loop...")

        job = context.scope.launch {
            while (isActive) {
                try {
                    val processedUsers = mutableSetOf<kotlin.uuid.Uuid>()
                    while (isActive) {
                        val pendingUsers = context.affinityStore.getPendingUsers(limit = 20)
                            .filter { it !in processedUsers }

                        if (pendingUsers.isEmpty()) break

                        context.logger.info("Worker found ${pendingUsers.size} pending users to check")
                        for (userId in pendingUsers) {
                            if (!isActive) break
                            processedUsers.add(userId)
                            calculator.recalculateVector(userId)
                            delay(100.milliseconds)
                        }
                    }
                    context.logger.info("All pending affinity vectors processed (${processedUsers.size} users). Sleeping for 24h...")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    context.logger.error("Error in affinity recalculation loop", e)
                }

                // Задержка 24 часа между циклами перерасчета вектора
                delay(24.hours)
            }
        }
    }

    fun stop() {
        context.logger.info("Stopping AffinityRecalculationWorker...")
        job?.cancel()
        job = null
    }
}
