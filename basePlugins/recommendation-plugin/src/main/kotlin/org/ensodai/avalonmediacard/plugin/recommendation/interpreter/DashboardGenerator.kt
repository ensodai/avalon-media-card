package org.ensodai.avalonmediacard.plugin.recommendation.interpreter

import org.ensodai.avalonmediacard.contract.i18n.currentPluginLocale
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.DynamicSection
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.plugin.recommendation.RecommendationI18nHelper
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints.*
import kotlin.time.Clock

class DashboardGenerator(private val pluginContext: PluginContext) {
    // Движок инициализируется списком доступных шаблонов (Blueprints)
    private val engine = VectorInterpreterEngine(
        listOf(
            SerendipityTropeIsolationBlueprint,
            TempAfterWorkDecompressBlueprint,
            TempMorningRushBlueprint,
            TempLunchBreakBlueprint,
            TempMidnightMelancholyBlueprint,
            TempWeekendEpicBingeBlueprint,
            TempSundayCozyBlueprint,
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
            SerendipityActorPivotBlueprint,
            SerendipityGenreBendBlueprint,
            SerendipityDecadeSwapBlueprint,
            SerendipityAnimationTrapBlueprint,
            SerendipityBlindSpotBlueprint,
            SocialHiddenGemsBlueprint,
            SocialTrendingNowBlueprint,
            SocialUpcomingHypeBlueprint,
            SocialCriticsChoiceBlueprint,
            SocialGuiltyPleasureBlueprint,
            SocialFranchiseBingeBlueprint,
            ExploitActorBingeBlueprint,
            ExploitDirectorDeepDiveBlueprint,
            ExploitBecauseYouWatchedBlueprint,
            ExploitCurrentObsessionBlueprint,
            ExploitGenreMastersBlueprint,
            ActorMoodSynergyBlueprint,
            DirectorStudioSynergyBlueprint,
            NostalgiaTriggerBlueprint,
            BingeWatchingWeekendBlueprint,
            EchoChamberBreakerBlueprint,
            MicroNicheExplorationBlueprint,
            AnimeSpecialistBlueprint
            // Сюда будем добавлять новые шаблоны по мере их написания
        )
    )

    suspend fun generate(vector: AffinityVector, language: String = "ru"): List<DynamicSection> {
        // 1. Формируем контекст сессии для движка (Соблюдая правило №7)
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val secondsSinceEpoch = nowMillis / 1000
        val daysSinceEpoch = secondsSinceEpoch / 86400
        val dayOfWeekIndex = ((daysSinceEpoch + 3) % 7).toInt()
        val isWeekend = dayOfWeekIndex == 5 || dayOfWeekIndex == 6
        val hour = (((secondsSinceEpoch % 86400) / 3600).toInt() + 3) % 24

        // Получаем справочник жанров из кэша ядра для переданной локали
        val genreMap = pluginContext.genreDictionary.getLocalizedGenres(language)
        val effectiveI18n = if (pluginContext.i18n is org.ensodai.avalonmediacard.contract.i18n.EmptyPluginI18n) {
            RecommendationI18nHelper.load()
        } else {
            pluginContext.i18n
        }

        val context = InterpreterContext(
            affinityVector = vector,
            topGenres = vector.genreWeights.entries.sortedByDescending { it.value }.map { it.key },
            topKeywords = vector.keywordWeights.entries.sortedByDescending { it.value }.map { it.key },
            localHour = hour,
            localizedGenres = genreMap,
            isWeekend = isWeekend,
            i18n = effectiveI18n,
            locale = language
        )

        // 2. Делегируем всю сложную логику новому Движку-Интерпретатору
        val semanticSegments = engine.generateDashboard(context, maxShelves = 15)

        // 3. Адаптируем Семантические Сегменты обратно в формат DynamicSection
        // Это позволяет не ломать остальную архитектуру проекта (UI и API)
        return semanticSegments.mapIndexed { index, segment ->
            DynamicSection(
                id = "${segment.blueprintId.lowercase()}_$index",
                title = segment.displayTitle,
                description = segment.displaySubtitle,
                type = segment.visualLayout, // <-- Теперь тип передается напрямую из контракта
                targetType = segment.targetType,
                weight = segment.relevanceScore,
                queryParams = segment.queryParams
            )
        }
    }
}
