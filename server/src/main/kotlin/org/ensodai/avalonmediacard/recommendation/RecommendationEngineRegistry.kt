package org.ensodai.avalonmediacard.recommendation

import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.DynamicSection
import org.ensodai.avalonmediacard.contract.plugins.RecommendationEngine
import org.ensodai.avalonmediacard.contract.plugins.RecommendationEngineRegistrar
import org.koin.core.annotation.Single
import java.util.concurrent.atomic.AtomicReference
import kotlin.uuid.Uuid

@Single(binds = [RecommendationEngine::class, RecommendationEngineRegistrar::class])
class RecommendationEngineRegistry(
    private val defaultEngine: DefaultRecommendationEngine
) : RecommendationEngine, RecommendationEngineRegistrar {

    private val activeEngine = AtomicReference<RecommendationEngine?>(null)

    override fun registerEngine(engine: RecommendationEngine) {
        activeEngine.set(engine)
    }

    override fun unregisterEngine() {
        activeEngine.set(null)
    }

    override fun unregisterEngine(engine: RecommendationEngine) {
        activeEngine.compareAndSet(engine, null)
    }

    override suspend fun getAffinityVector(userId: Uuid): AffinityVector {
        val engine = activeEngine.get() ?: defaultEngine
        return engine.getAffinityVector(userId)
    }

    override suspend fun generateDashboard(userId: Uuid, language: String): List<DynamicSection> {
        val engine = activeEngine.get() ?: defaultEngine
        return engine.generateDashboard(userId, language)
    }

    override suspend fun generateTab(userId: Uuid, scope: String, language: String): List<DynamicSection> {
        val engine = activeEngine.get() ?: defaultEngine
        return engine.generateTab(userId, scope, language)
    }
}
