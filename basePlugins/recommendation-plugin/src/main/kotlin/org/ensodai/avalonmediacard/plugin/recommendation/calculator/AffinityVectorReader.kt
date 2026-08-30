package org.ensodai.avalonmediacard.plugin.recommendation.calculator

import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.DynamicSection
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.RecommendationEngine
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.DashboardGenerator
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.SideTabGenerator
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.SidebarTabScope
import kotlin.uuid.Uuid

/**
 * Читает заранее вычисленный вектор из БД (1 SELECT запрос).
 * Используется в рантайме при каждом запросе пользователя.
 */
class AffinityVectorReader(
    private val context: PluginContext
) : RecommendationEngine {

    override suspend fun getAffinityVector(userId: Uuid): AffinityVector {
        val rawVector = try {
            context.affinityStore.getVector(userId) ?: AffinityVector()
        } catch (e: Exception) {
            context.logger.error("Failed to read affinity vector for user $userId", e)
            AffinityVector()
        }

        val tempString = context.settings.getString("recommendation_temperature") ?: "1.5"
        val temperature = tempString.toDoubleOrNull() ?: 1.5

        if (temperature == 1.0) return rawVector

        return AffinityVector(
            genreWeights = rawVector.genreWeights.mapValues { applyTemperatureScaling(it.value, temperature) },
            keywordWeights = rawVector.keywordWeights.mapValues { applyTemperatureScaling(it.value, temperature) },
            directorWeights = rawVector.directorWeights.mapValues { applyTemperatureScaling(it.value, temperature) },
            actorWeights = rawVector.actorWeights.mapValues { applyTemperatureScaling(it.value, temperature) },
            companyWeights = rawVector.companyWeights.mapValues { applyTemperatureScaling(it.value, temperature) },
            pacingWeights = rawVector.pacingWeights.mapValues { applyTemperatureScaling(it.value, temperature) },
            eraWeights = rawVector.eraWeights.mapValues { applyTemperatureScaling(it.value, temperature) },
            moodWeights = rawVector.moodWeights.mapValues { applyTemperatureScaling(it.value, temperature) }
        )
    }

    override suspend fun generateDashboard(userId: Uuid, language: String): List<DynamicSection> {
        val vector = getAffinityVector(userId)
        val generator = DashboardGenerator(context)
        return generator.generate(vector, language)
    }

    override suspend fun generateTab(userId: Uuid, scope: String, language: String): List<DynamicSection> {
        val tabScope = when (scope.lowercase()) {
            "movies", "movie" -> SidebarTabScope.MOVIES
            "tv_shows", "tvshows", "tv" -> SidebarTabScope.TV_SHOWS
            "trends", "trend" -> SidebarTabScope.TRENDS
            else -> return emptyList()
        }
        val generator = SideTabGenerator(context)
        return generator.generateTab(userId, tabScope, language)
    }
}
