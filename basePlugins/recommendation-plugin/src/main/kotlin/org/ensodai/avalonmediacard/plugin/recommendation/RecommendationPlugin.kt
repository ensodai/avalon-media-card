package org.ensodai.avalonmediacard.plugin.recommendation

import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.AffinityRecalculationWorker
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.AffinityVectorCalculator
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.AffinityVectorReader

class RecommendationPlugin : AvalonPlugin {
    override val id: String = "org.ensodai.recommendation"
    override val name: String = "Premium Recommendation Engine"
    override val version: String = "1.0.0"
    override val author: String = "Antigravity"

    private var worker: AffinityRecalculationWorker? = null

    override fun onInitialize(context: PluginContext) {
        // 1. Регистрируем Reader как движок (рантайм)
        val reader = AffinityVectorReader(context)
        context.recommendations.registerEngine(reader)

        // 2. Запускаем фоновый Calculator (воркер)
        val calculator = AffinityVectorCalculator(context)
        worker = AffinityRecalculationWorker(context, calculator)
        worker?.start()

        context.logger.info("RecommendationPlugin initialized: AffinityVectorReader is active, Worker started.")
    }

    override fun onDestroy() {
        worker?.stop()
        super.onDestroy()
    }
}
