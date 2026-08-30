package org.ensodai.avalonmediacard.plugin.recommendation.interpreter

import org.ensodai.avalonmediacard.contract.model.SectionType

class VectorInterpreterEngine(
    private val blueprints: List<InterpreterBlueprint>
) {
    /**
     * Основной метод движка. 
     * Принимает контекст юзера, прогоняет через все шаблоны, отбирает СТРОГО 1 лучший HERO-баннер 
     * и Топ-N лучших уникальных каруселей.
     */
    fun generateDashboard(context: InterpreterContext, maxShelves: Int = 15): List<SemanticSegment> {
        val allSegments = mutableListOf<SemanticSegment>()

        // 1. Прогоняем контекст через все шаблоны
        for (blueprint in blueprints) {
            val segment = blueprint.evaluate(context)
            if (segment != null) {
                allSegments.add(segment)
            }
        }

        // 2. Сортируем по relevanceScore
        allSegments.sortByDescending { it.relevanceScore }

        val orchestratedSegments = mutableListOf<SemanticSegment>()

        // 3. СТРОГО 1 HERO-баннер (победитель с максимальным релевантным скором)
        val bestHero = allSegments.firstOrNull { it.visualLayout == SectionType.HERO }
        if (bestHero != null) {
            orchestratedSegments.add(bestHero)
        }

        // 4. Оркестрация остальных (не-HERO) каруселей с дедупликацией фокуса
        val nonHeroSegments = allSegments.filter { it.visualLayout != SectionType.HERO }
        val usedGenres = mutableSetOf<String>()
        val usedCast = mutableSetOf<String>()
        val usedCrew = mutableSetOf<String>()

        for (segment in nonHeroSegments) {
            val genreParam = segment.queryParams["with_genres"]
            val castParam = segment.queryParams["with_cast"]
            val crewParam = segment.queryParams["with_crew"]

            // Проверяем, не выводили ли мы уже полку с таким же главным фокусом
            var isDuplicate = false
            if (genreParam != null && usedGenres.contains(genreParam)) isDuplicate = true
            if (castParam != null && usedCast.contains(castParam)) isDuplicate = true
            if (crewParam != null && usedCrew.contains(crewParam)) isDuplicate = true

            // Если релевантность абсолютная (>= 1.0), пропускаем без дубликат-фильтра
            if (segment.relevanceScore >= 1.0) isDuplicate = false

            if (!isDuplicate) {
                orchestratedSegments.add(segment)
                if (genreParam != null) usedGenres.add(genreParam)
                if (castParam != null) usedCast.add(castParam)
                if (crewParam != null) usedCrew.add(crewParam)
            }

            if (orchestratedSegments.size >= maxShelves) break
        }

        return orchestratedSegments
    }
}

