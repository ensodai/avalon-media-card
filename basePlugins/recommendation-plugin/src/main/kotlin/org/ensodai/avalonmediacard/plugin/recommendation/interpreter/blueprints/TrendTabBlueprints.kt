package org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.SectionType
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.InterpreterBlueprint
import kotlin.time.Clock

/**
 * Пул блюпринтов для вкладки "Тренды" (TrendTabBlueprints)
 * Содержит 15 спек-блоков по спецификации исследования №10
 */

// 1. Общий тренд дня (Hero Баннер)
val TrendDailyHeroBlueprint = blueprint("TREND_DAILY_HERO") {
    condition { true }
    score { 1.0 }
    segment {
        title = t("blueprint.trend_daily_hero.title")
        subtitle = t("blueprint.trend_daily_hero.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.HERO
        params = mapOf("trending" to "all/day")
    }
}

// 2. Популярное кино этой недели
val TrendMoviesWeekBlueprint = blueprint("TREND_MOVIES_WEEK") {
    condition { true }
    score { 0.95 }
    segment {
        title = t("blueprint.trend_movies_week.title")
        subtitle = t("blueprint.trend_movies_week.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf("trending" to "movie/week")
    }
}

// 3. Популярные сериалы недели
val TrendTvWeekBlueprint = blueprint("TREND_TV_WEEK") {
    condition { true }
    score { 0.90 }
    segment {
        title = t("blueprint.trend_tv_week.title")
        subtitle = t("blueprint.trend_tv_week.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf("trending" to "tv/week")
    }
}

// 4. Самые ожидаемые премьеры
val TrendUpcomingHypeBlueprint = blueprint("TREND_UPCOMING_HYPE") {
    condition { true }
    score { 0.85 }
    val currentDate = Clock.System.now().toString().substringBefore("T")
    segment {
        title = t("blueprint.trend_upcoming_hype.title")
        subtitle = t("blueprint.trend_upcoming_hype.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "primary_release_date.gte" to currentDate,
            "sort_by" to "popularity.desc"
        )
    }
}

// 5. Выбор критиков (современное кино)
val TrendCriticsChoiceBlueprint = blueprint("TREND_CRITICS_CHOICE") {
    condition { true }
    score { 0.80 }
    segment {
        title = t("blueprint.trend_critics_choice.title")
        subtitle = t("blueprint.trend_critics_choice.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_genres" to "18|36",
            "primary_release_date.gte" to "2020-01-01",
            "vote_average.gte" to "7.5",
            "vote_count.gte" to "1000",
            "sort_by" to "vote_average.desc"
        )
    }
}

// 6. Кассовые гиганты
val TrendBoxOfficeGiantsBlueprint = blueprint("TREND_BOX_OFFICE_GIANTS") {
    condition { true }
    score { 0.78 }
    segment {
        title = t("blueprint.trend_box_office_giants.title")
        subtitle = t("blueprint.trend_box_office_giants.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "primary_release_date.gte" to "2018-01-01",
            "sort_by" to "revenue.desc"
        )
    }
}

// 7. Триумфаторы фестивалей
val TrendFestivalWinnersBlueprint = blueprint("TREND_FESTIVAL_WINNERS") {
    condition { true }
    score { 0.75 }
    segment {
        title = t("blueprint.trend_festival_winners.title")
        subtitle = t("blueprint.trend_festival_winners.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_keywords" to "23432|31122",
            "sort_by" to "popularity.desc"
        )
    }
}

// 8. Набирающее популярность: Ретро
val TrendRetroSurgeBlueprint = blueprint("TREND_RETRO_SURGE") {
    condition { true }
    score { 0.72 }
    segment {
        title = t("blueprint.trend_retro_surge.title")
        subtitle = t("blueprint.trend_retro_surge.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "primary_release_date.lte" to "2005-01-01",
            "sort_by" to "popularity.desc",
            "vote_count.gte" to "3000"
        )
    }
}

// 9. Новое на Netflix
val TrendNewNetflixBlueprint = blueprint("TREND_NEW_NETFLIX") {
    condition { true }
    score { 0.70 }
    segment {
        title = t("blueprint.trend_new_netflix.title")
        subtitle = t("blueprint.trend_new_netflix.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_networks" to "213",
            "sort_by" to "popularity.desc"
        )
    }
}

// 10. Культовые феномены
val TrendCultPhenomenaBlueprint = blueprint("TREND_CULT_PHENOMENA") {
    condition { true }
    score { 0.68 }
    segment {
        title = t("blueprint.trend_cult_phenomena.title")
        subtitle = t("blueprint.trend_cult_phenomena.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_keywords" to "7627",
            "sort_by" to "popularity.desc"
        )
    }
}

// 11. Независимый прорыв
val TrendIndieBreakthroughBlueprint = blueprint("TREND_INDIE_BREAKTHROUGH") {
    condition { true }
    score { 0.65 }
    segment {
        title = t("blueprint.trend_indie_breakthrough.title")
        subtitle = t("blueprint.trend_indie_breakthrough.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_keywords" to "3384",
            "sort_by" to "popularity.desc",
            "vote_count.lte" to "1500"
        )
    }
}

// 12. На грани выживания (Тематика Lampa)
val TrendSurvivalTropeBlueprint = blueprint("TREND_SURVIVAL_TROPE") {
    condition { true }
    score { 0.62 }
    segment {
        title = t("blueprint.trend_survival_trope.title")
        subtitle = t("blueprint.trend_survival_trope.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_keywords" to "10051|10292",
            "without_genres" to "878",
            "without_keywords" to "9715",
            "sort_by" to "popularity.desc"
        )
    }
}

// 13. Мировой кинематограф
val TrendWorldCinemaBlueprint = blueprint("TREND_WORLD_CINEMA") {
    condition { true }
    score { 0.60 }
    segment {
        title = t("blueprint.trend_world_cinema.title")
        subtitle = t("blueprint.trend_world_cinema.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_original_language" to "!en",
            "sort_by" to "popularity.desc",
            "vote_count.gte" to "500"
        )
    }
}

// 14. Жанровый стык в тренде
val TrendGenreHybridBlueprint = blueprint("TREND_GENRE_HYBRID") {
    condition { true }
    score { 0.55 }
    segment {
        title = t("blueprint.trend_genre_hybrid.title")
        subtitle = t("blueprint.trend_genre_hybrid.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "878,53",
            "sort_by" to "popularity.desc"
        )
    }
}

// 15. Скоро уйдут из трендов
val TrendLeavingTrendsBlueprint = blueprint("TREND_LEAVING_TRENDS") {
    condition { true }
    score { 0.50 }
    segment {
        title = t("blueprint.trend_leaving_trends.title")
        subtitle = t("blueprint.trend_leaving_trends.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "vote_average.gte" to "7.5",
            "vote_count.gte" to "1000",
            "sort_by" to "popularity.asc"
        )
    }
}

val TrendTabBlueprints: List<InterpreterBlueprint> = listOf(
    TrendDailyHeroBlueprint,
    TrendMoviesWeekBlueprint,
    TrendTvWeekBlueprint,
    TrendUpcomingHypeBlueprint,
    TrendCriticsChoiceBlueprint,
    TrendBoxOfficeGiantsBlueprint,
    TrendFestivalWinnersBlueprint,
    TrendRetroSurgeBlueprint,
    TrendNewNetflixBlueprint,
    TrendCultPhenomenaBlueprint,
    TrendIndieBreakthroughBlueprint,
    TrendSurvivalTropeBlueprint,
    TrendWorldCinemaBlueprint,
    TrendGenreHybridBlueprint,
    TrendLeavingTrendsBlueprint
)
