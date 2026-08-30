package org.ensodai.avalonmediacard.plugin.recommendation

import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.InterpreterContext
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints.*
import kotlin.test.Test
import kotlin.test.assertTrue

class RelevanceMathTest {

    private val allBlueprints = listOf(
        SerendipityTropeIsolationBlueprint,
        TempAfterWorkDecompressBlueprint,
        TempMorningRushBlueprint,
        TempLunchBreakBlueprint,
        TempMidnightMelancholyBlueprint,
        TempSundayCozyBlueprint,
        TempWeekendEpicBingeBlueprint,
        BingeWatchingWeekendBlueprint,
        AestheticStudioVibeBlueprint,
        AestheticAuteurThemesBlueprint,
        AestheticDecadeNostalgiaBlueprint,
        AestheticIndieDarlingsBlueprint,
        AestheticBlackAndWhiteBlueprint,
        AestheticForeignGemsBlueprint,
        MoodDarkComedyMixBlueprint,
        MoodClaustrophobiaBlueprint,
        MoodMindBenderBlueprint,
        MoodUnapologeticGritBlueprint,
        MoodHeartwarmingBlueprint,
        MoodAdrenalineSpikeBlueprint,
        DirectorStudioSynergyBlueprint,
        NostalgiaTriggerBlueprint,
        ActorMoodSynergyBlueprint,
        SerendipityActorPivotBlueprint,
        SerendipityGenreBendBlueprint,
        SerendipityDecadeSwapBlueprint,
        SerendipityAnimationTrapBlueprint,
        SerendipityBlindSpotBlueprint,
        EchoChamberBreakerBlueprint,
        MicroNicheExplorationBlueprint,
        SocialHiddenGemsBlueprint,
        SocialTrendingNowBlueprint,
        SocialUpcomingHypeBlueprint,
        SocialCriticsChoiceBlueprint,
        SocialGuiltyPleasureBlueprint,
        SocialFranchiseBingeBlueprint,
        ExploitBecauseYouWatchedBlueprint,
        ExploitCurrentObsessionBlueprint,
        ExploitActorBingeBlueprint,
        ExploitDirectorDeepDiveBlueprint,
        ExploitGenreMastersBlueprint
    )

    // Вспомогательный метод для симуляции профиля
    private fun evaluatePersona(
        vector: AffinityVector,
        hour: Int,
        isWeekend: Boolean
    ): List<String> {
        val context = InterpreterContext(
            affinityVector = vector,
            topGenres = listOf("878", "28"), // Фантастика, Боевик
            topKeywords = emptyList(),
            localHour = hour,
            localizedGenres = emptyMap(),
            isWeekend = isWeekend
        )

        // Прогоняем карусельные блюпринты (исключая HERO баннер) и сортируем по скору
        return allBlueprints
            .filter { it != ExploitCurrentObsessionBlueprint }
            .mapNotNull { it.evaluate(context) }
            .sortedByDescending { it.relevanceScore }
            .map { it.blueprintId }
    }

    @Test
    fun `test Time Multiplier pushes temporal blueprints to the top`() {
        // Базовый любитель фантастики
        val vector = AffinityVector(
            genreWeights = mapOf("878" to 0.9, "18" to 0.5),
            pacingWeights = mapOf("slow" to 0.9), // Любит медленное кино
            recentWatchedIds = emptyList()
        )

        // Днем на выходных побеждают обычные или дневные блюпринты
        val dayTop = evaluatePersona(vector, hour = 14, isWeekend = true)

        // Ночью (23:00) ночной меланхоличный блюпринт должен взлететь в топ благодаря темпу и множителю
        val nightTop = evaluatePersona(vector, hour = 23, isWeekend = false)

        assertTrue(
            nightTop.take(3).contains("TEMP_MIDNIGHT_MELANCHOLY"),
            "Математика времени не сработала: ночные блюпринты не пробились в ТОП-3 ночью. Топ: ${nightTop.take(5)}"
        )
    }

    @Test
    fun `test Serendipity Multiplier breaks the comfort zone`() {
        // Эстет: любит тяжелые жанры, но ненавидит мультики
        val vector = AffinityVector(
            genreWeights = mapOf("53" to 0.9, "878" to 0.9, "18" to 0.8, "16" to 0.0),
            recentWatchedIds = emptyList()
        )

        val topBlueprints = evaluatePersona(vector, hour = 20, isWeekend = false)

        // Проверяем: блюпринт "Ловушка для взрослых" (взрослая анимация) должен сработать, 
        // получить свой serendipityMultiplier = 1.9 и выскочить в ТОП-3
        assertTrue(
            topBlueprints.take(3).contains("SERENDIPITY_ANIMATION_TRAP"),
            "Математика выхода из зоны комфорта не сработала: серендипность не вытянула блюпринт"
        )
    }
}
