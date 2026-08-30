package org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.SectionType
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.InterpreterBlueprint

/**
 * Пул блюпринтов для вкладки "Сериалы" (TvShowTabBlueprints)
 * Содержит 15 спек-блоков по спецификации исследования №10
 */

// 1. Продолжить просмотр (Сериалы)
val TvContinueWatchingBlueprint = blueprint("TV_CONTINUE_WATCHING") {
    condition { context.continueWatchingKeys.isNotEmpty() }
    val keys = context.continueWatchingKeys
    score { 1.0 }
    segment {
        title = t("blueprint.tv_continue_watching.title")
        subtitle = t("blueprint.tv_continue_watching.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        mediaIds = keys
    }
}

// 2. Сериалы на выходные (Hero Баннер)
val TvWeekendBingeHeroBlueprint = blueprint("TV_WEEKEND_BINGE_HERO") {
    condition { true }
    score { 1.0 }
    segment {
        title = t("blueprint.tv_weekend_binge_hero.title")
        subtitle = t("blueprint.tv_weekend_binge_hero.subtitle")
        type = EntityType.TV
        layout = SectionType.HERO
        params = mapOf(
            "with_status" to "3",
            "sort_by" to "popularity.desc"
        )
    }
}

// 3. Знак качества: HBO
val TvHboQualityBlueprint = blueprint("TV_HBO_QUALITY") {
    condition { true }
    score { 0.95 }
    segment {
        title = t("blueprint.tv_hbo_quality.title")
        subtitle = t("blueprint.tv_hbo_quality.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_networks" to "49",
            "sort_by" to "popularity.desc"
        )
    }
}

// 4. Вселенные Netflix
val TvNetflixUniversesBlueprint = blueprint("TV_NETFLIX_UNIVERSES") {
    condition { true }
    score { 0.90 }
    segment {
        title = t("blueprint.tv_netflix_universes.title")
        subtitle = t("blueprint.tv_netflix_universes.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_networks" to "213",
            "sort_by" to "popularity.desc"
        )
    }
}

// 5. Ситкомы и комедии
val TvSitcomsBlueprint = blueprint("TV_SITCOMS") {
    condition { true }
    score { 0.85 }
    segment {
        title = t("blueprint.tv_sitcoms.title")
        subtitle = t("blueprint.tv_sitcoms.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "35",
            "with_runtime.lte" to "30",
            "sort_by" to "popularity.desc"
        )
    }
}

// 6. Научная фантастика и фэнтези (TV Genre 10765)
val TvSciFiFantasyBlueprint = blueprint("TV_SCIFI_FANTASY") {
    condition { true }
    score { 0.82 }
    segment {
        title = t("blueprint.tv_scifi_fantasy.title")
        subtitle = t("blueprint.tv_scifi_fantasy.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_genres" to "10765",
            "sort_by" to "popularity.desc"
        )
    }
}

// 7. Реальные преступления (True Crime)
val TvTrueCrimeBlueprint = blueprint("TV_TRUE_CRIME") {
    condition { true }
    score { 0.78 }
    segment {
        title = t("blueprint.tv_true_crime.title")
        subtitle = t("blueprint.tv_true_crime.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "99",
            "with_keywords" to "9875",
            "sort_by" to "popularity.desc"
        )
    }
}

// 8. Элитная драма
val TvEliteDramaBlueprint = blueprint("TV_ELITE_DRAMA") {
    condition { true }
    score { 0.75 }
    segment {
        title = t("blueprint.tv_elite_drama.title")
        subtitle = t("blueprint.tv_elite_drama.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_type" to "4",
            "with_genres" to "18",
            "vote_average.gte" to "8.0",
            "sort_by" to "popularity.desc"
        )
    }
}

// 9. Экшен и приключения (TV Genre 10759)
val TvActionAdventureBlueprint = blueprint("TV_ACTION_ADVENTURE") {
    condition { true }
    score { 0.72 }
    segment {
        title = t("blueprint.tv_action_adventure.title")
        subtitle = t("blueprint.tv_action_adventure.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "10759",
            "sort_by" to "popularity.desc"
        )
    }
}

// 10. Взрослая анимация (TV Genre 16)
val TvAdultAnimationBlueprint = blueprint("TV_ADULT_ANIMATION") {
    condition { true }
    score { 0.70 }
    segment {
        title = t("blueprint.tv_adult_animation.title")
        subtitle = t("blueprint.tv_adult_animation.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "16",
            "with_keywords" to "11881",
            "sort_by" to "popularity.desc"
        )
    }
}

// 11. Британский стиль
val TvBritishStyleBlueprint = blueprint("TV_BRITISH_STYLE") {
    condition { true }
    score { 0.68 }
    segment {
        title = t("blueprint.tv_british_style.title")
        subtitle = t("blueprint.tv_british_style.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_origin_country" to "GB",
            "sort_by" to "popularity.desc"
        )
    }
}

// 12. Дорамы и K-Drama
val TvKdramaBlueprint = blueprint("TV_KDRAMA") {
    condition { true }
    score { 0.65 }
    segment {
        title = t("blueprint.tv_kdrama.title")
        subtitle = t("blueprint.tv_kdrama.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_origin_country" to "KR",
            "sort_by" to "popularity.desc"
        )
    }
}

// 13. Скрытые сокровища ТВ
val TvHiddenTreasuresBlueprint = blueprint("TV_HIDDEN_TREASURES") {
    condition { true }
    score { 0.60 }
    segment {
        title = t("blueprint.tv_hidden_treasures.title")
        subtitle = t("blueprint.tv_hidden_treasures.subtitle")
        type = EntityType.TV
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_genres" to "18",
            "vote_average.gte" to "8.0",
            "vote_count.lte" to "500",
            "sort_by" to "vote_average.desc"
        )
    }
}

// 14. Ретро-телевидение
val TvRetroClassicBlueprint = blueprint("TV_RETRO_CLASSIC") {
    condition { true }
    score { 0.55 }
    segment {
        title = t("blueprint.tv_retro_classic.title")
        subtitle = t("blueprint.tv_retro_classic.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "first_air_date.lte" to "2010-01-01",
            "sort_by" to "popularity.desc"
        )
    }
}

// 15. Новый вызов
val TvComfortZoneBreakBlueprint = blueprint("TV_COMFORT_ZONE_BREAK") {
    condition { true }
    score { 0.50 }
    segment {
        title = t("blueprint.tv_comfort_zone_break.title")
        subtitle = t("blueprint.tv_comfort_zone_break.subtitle")
        type = EntityType.TV
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_genres" to "99",
            "vote_average.gte" to "8.0",
            "sort_by" to "vote_average.desc"
        )
    }
}

val TvShowTabBlueprints: List<InterpreterBlueprint> = listOf(
    TvContinueWatchingBlueprint,
    TvWeekendBingeHeroBlueprint,
    TvHboQualityBlueprint,
    TvNetflixUniversesBlueprint,
    TvSitcomsBlueprint,
    TvSciFiFantasyBlueprint,
    TvTrueCrimeBlueprint,
    TvEliteDramaBlueprint,
    TvActionAdventureBlueprint,
    TvAdultAnimationBlueprint,
    TvBritishStyleBlueprint,
    TvKdramaBlueprint,
    TvHiddenTreasuresBlueprint,
    TvRetroClassicBlueprint,
    TvComfortZoneBreakBlueprint
)
