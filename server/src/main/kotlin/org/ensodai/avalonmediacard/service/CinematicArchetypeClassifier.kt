package org.ensodai.avalonmediacard.service

import org.ensodai.avalonmediacard.contract.model.ArchetypeDto
import org.ensodai.avalonmediacard.contract.model.ArchetypeType
import org.ensodai.avalonmediacard.contract.model.DecadeStatDto
import org.ensodai.avalonmediacard.contract.model.GenreStatDto
import org.koin.core.annotation.Single

@Single
class CinematicArchetypeClassifier {

    /**
     * Вычисляет кинематографический архетип на основе распределения жанров и эпох.
     * Возвращает null при пустой библиотеке.
     */
    fun classify(
        genres: List<GenreStatDto>,
        decades: List<DecadeStatDto>
    ): ArchetypeDto? {
        if (genres.isEmpty()) return null

        val dominantGenreIds = genres.take(3).map { it.genreId }
        val genreScoreMap = genres.associate { it.genreId to it.percentage }

        // Весовые категории архетипов на основе процентов просмотренных жанров:
        // 1. SCIFI_VISIONARY: Sci-Fi (878), Fantasy (14)
        val scifiWeight = (genreScoreMap[878] ?: 0f) * 1.3f + (genreScoreMap[14] ?: 0f) * 0.9f

        // 2. SUSPENSE_HUNTER: Horror (27), Thriller (53), Mystery (9648), Crime (80)
        val suspenseWeight = (genreScoreMap[53] ?: 0f) * 1.1f +
            (genreScoreMap[27] ?: 0f) * 1.2f +
            (genreScoreMap[9648] ?: 0f) * 1.1f +
            (genreScoreMap[80] ?: 0f) * 0.8f

        // 3. DEEP_EMPATH: Drama (18), Romance (10749)
        val empathWeight = (genreScoreMap[18] ?: 0f) * 1.0f + (genreScoreMap[10749] ?: 0f) * 1.2f

        // 4. DOPAMINE_SEEKER: Comedy (35), Animation (16), Action (28), Family (10751)
        val dopamineWeight = (genreScoreMap[35] ?: 0f) * 1.1f +
            (genreScoreMap[16] ?: 0f) * 1.1f +
            (genreScoreMap[28] ?: 0f) * 0.7f +
            (genreScoreMap[10751] ?: 0f) * 0.9f

        // 5. CURIOUS_VOYAGER: Documentary (99), History (36), War (10752), Western (37) + ретро (<1990)
        val totalWatchedInDecades = decades.sumOf { it.watchedCount }.coerceAtLeast(1)
        val vintageCount = decades.filter { it.startYear in 0..1989 }.sumOf { it.watchedCount }
        val vintagePercentage = (vintageCount.toFloat() / totalWatchedInDecades) * 100f

        val voyagerWeight = (genreScoreMap[99] ?: 0f) * 1.5f +
            (genreScoreMap[36] ?: 0f) * 1.2f +
            (genreScoreMap[10752] ?: 0f) * 0.9f +
            (genreScoreMap[37] ?: 0f) * 1.1f +
            vintagePercentage * 0.5f

        val scores = listOf(
            ArchetypeType.SCIFI_VISIONARY to scifiWeight,
            ArchetypeType.SUSPENSE_HUNTER to suspenseWeight,
            ArchetypeType.DEEP_EMPATH to empathWeight,
            ArchetypeType.DOPAMINE_SEEKER to dopamineWeight,
            ArchetypeType.CURIOUS_VOYAGER to voyagerWeight
        )

        val bestArchetype = scores.maxByOrNull { it.second }?.takeIf { it.second > 0f }?.first
            ?: ArchetypeType.CURIOUS_VOYAGER

        return ArchetypeDto(
            type = bestArchetype,
            dominantGenreIds = dominantGenreIds
        )
    }
}
