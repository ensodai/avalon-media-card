package org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.SectionType
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.InterpreterBlueprint
import kotlin.time.Clock

/**
 * Пул блюпринтов для вкладки "Фильмы" (MovieTabBlueprints)
 * Содержит 15 спек-блоков по спецификации исследования №10
 */

// 1. Продолжить просмотр (Фильмы)
val MovieContinueWatchingBlueprint = blueprint("MOVIE_CONTINUE_WATCHING") {
    condition { context.continueWatchingKeys.isNotEmpty() }
    val keys = context.continueWatchingKeys
    score { 1.0 }
    segment {
        title = t("blueprint.movie_continue_watching.title")
        subtitle = t("blueprint.movie_continue_watching.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        mediaIds = keys
    }
}

// 2. Главный жанр (Hero Баннер)
val MovieTopGenreHeroBlueprint = blueprint("MOVIE_TOP_GENRE_HERO") {
    condition { context.topGenres.isNotEmpty() }
    val topGenre = context.topGenres.firstOrNull() ?: reject()
    val genreName = context.localizedGenres[topGenre] ?: "Любимый жанр"
    score { 1.0 }
    val currentDate = Clock.System.now().toString().substringBefore("T")
    segment {
        title = if (genreName.isNotBlank()) t("blueprint.movie_top_genre_hero.title", genreName) else t("blueprint.movie_top_genre_hero.title_fallback")
        subtitle = t("blueprint.movie_top_genre_hero.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.HERO
        params = mapOf(
            "with_genres" to topGenre,
            "primary_release_date.lte" to currentDate,
            "sort_by" to "popularity.desc"
        )
    }
}

// 3. Потому что вы смотрели
val MovieBecauseYouWatchedBlueprint = blueprint("MOVIE_BECAUSE_YOU_WATCHED") {
    condition { context.affinityVector.recentWatchedIds.isNotEmpty() }
    val lastId = context.affinityVector.recentWatchedIds.firstOrNull() ?: reject()
    val movieTitle = context.lastWatchedTitle
    score { 0.95 }
    segment {
        title = if (movieTitle != null) t("blueprint.movie_because_you_watched.title", movieTitle) else t("blueprint.movie_because_you_watched.title_fallback")
        subtitle = t("blueprint.movie_because_you_watched.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf("similar_to" to lastId)
    }
}

// 4. Интеллектуальные игры разума
val MovieMindbenderBlueprint = blueprint("MOVIE_MINDBENDER") {
    condition { true }
    score { 0.85 }
    segment {
        title = t("blueprint.movie_mindbender.title")
        subtitle = t("blueprint.movie_mindbender.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_keywords" to "4842|12988",
            "without_genres" to "35,10751",
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "500"
        )
    }
}

// 5. Эстетика любимой студии
val MovieStudioAestheticBlueprint = blueprint("MOVIE_STUDIO_AESTHETIC") {
    val candidateCompany = context.affinityVector.companyWeights.maxByOrNull { it.value }?.key
    val topCompany =
        if (candidateCompany == null || candidateCompany == "420" || candidateCompany == "2" || candidateCompany == "174") "41077" else candidateCompany
    val companyName = context.topCompanyName ?: if (topCompany == "41077") "A24" else null
    condition { true }
    score { 0.80 }
    segment {
        title = if (companyName != null) t("blueprint.movie_studio_aesthetic.title", companyName) else t("blueprint.movie_studio_aesthetic.title_fallback")
        subtitle = t("blueprint.movie_studio_aesthetic.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_companies" to topCompany,
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "100"
        )
    }
}

// 6. В главных ролях
val MovieTopActorBlueprint = blueprint("MOVIE_TOP_ACTOR") {
    val topActor = context.affinityVector.actorWeights.maxByOrNull { it.value }?.key ?: reject()
    val actorName = context.topActorName
    condition { true }
    score { 0.78 }
    segment {
        title = if (actorName != null) t("blueprint.movie_top_actor.title", actorName) else t("blueprint.movie_top_actor.title_fallback")
        subtitle = t("blueprint.movie_top_actor.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_cast" to topActor,
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "300"
        )
    }
}

// 7. Режиссерский взгляд
val MovieTopDirectorBlueprint = blueprint("MOVIE_TOP_DIRECTOR") {
    val topDirector = context.affinityVector.directorWeights.maxByOrNull { it.value }?.key ?: reject()
    val directorName = context.topDirectorName
    condition { true }
    score { 0.75 }
    segment {
        title = if (directorName != null) t("blueprint.movie_top_director.title", directorName) else t("blueprint.movie_top_director.title_fallback")
        subtitle = t("blueprint.movie_top_director.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_crew" to topDirector,
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "100"
        )
    }
}

// 8. Уголок ностальгии
val MovieNostalgiaEraBlueprint = blueprint("MOVIE_NOSTALGIA_ERA") {
    condition { true }
    val topGenre = context.topGenres.firstOrNull() ?: "878"
    score { 0.72 }
    segment {
        title = t("blueprint.movie_nostalgia_era.title")
        subtitle = t("blueprint.movie_nostalgia_era.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "primary_release_date.gte" to "1980-01-01",
            "primary_release_date.lte" to "1999-12-31",
            "with_genres" to topGenre,
            "sort_by" to "vote_average.desc"
        )
    }
}

// 9. Скрытые инди-жемчужины
val MovieIndieGemsBlueprint = blueprint("MOVIE_INDIE_GEMS") {
    condition { true }
    score { 0.70 }
    segment {
        title = t("blueprint.movie_indie_gems.title")
        subtitle = t("blueprint.movie_indie_gems.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_keywords" to "3384",
            "vote_average.gte" to "7.5",
            "vote_count.lte" to "1000",
            "sort_by" to "vote_average.desc"
        )
    }
}

// 10. Чистый адреналин
val MovieAdrenalineSpikeBlueprint = blueprint("MOVIE_ADRENALINE_SPIKE") {
    condition { true }
    score { 0.68 }
    segment {
        title = t("blueprint.movie_adrenaline_spike.title")
        subtitle = t("blueprint.movie_adrenaline_spike.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "28",
            "with_keywords" to "9715|10090",
            "sort_by" to "popularity.desc"
        )
    }
}

// 11. Семейный вечер
val MovieFamilyNightBlueprint = blueprint("MOVIE_FAMILY_NIGHT") {
    condition { true }
    score { 0.65 }
    segment {
        title = t("blueprint.movie_family_night.title")
        subtitle = t("blueprint.movie_family_night.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "10751|35",
            "without_keywords" to "10349",
            "certification" to "G|PG",
            "certification_country" to "US",
            "sort_by" to "popularity.desc"
        )
    }
}

// 12. Душевное настроение
val MovieHeartwarmingBlueprint = blueprint("MOVIE_HEARTWARMING") {
    condition { true }
    score { 0.62 }
    segment {
        title = t("blueprint.movie_heartwarming.title")
        subtitle = t("blueprint.movie_heartwarming.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_keywords" to "9799|13130",
            "without_genres" to "53,27,80",
            "sort_by" to "vote_average.desc"
        )
    }
}

// 13. Скрытые сокровища
val MovieHiddenTreasuresBlueprint = blueprint("MOVIE_HIDDEN_TREASURES") {
    val topGenre = context.topGenres.firstOrNull() ?: "18"
    condition { true }
    score { 0.60 }
    segment {
        title = t("blueprint.movie_hidden_treasures.title")
        subtitle = t("blueprint.movie_hidden_treasures.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_genres" to topGenre,
            "vote_average.gte" to "7.0",
            "vote_count.gte" to "50",
            "vote_count.lte" to "300",
            "sort_by" to "vote_average.desc"
        )
    }
}

// 14. Постыдное удовольствие
val MovieGuiltyPleasuresBlueprint = blueprint("MOVIE_GUILTY_PLEASURES") {
    val topGenre = context.topGenres.firstOrNull() ?: "28"
    condition { true }
    score { 0.55 }
    segment {
        title = t("blueprint.movie_guilty_pleasures.title")
        subtitle = t("blueprint.movie_guilty_pleasures.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to topGenre,
            "vote_average.lte" to "5.5",
            "vote_count.gte" to "2000",
            "sort_by" to "popularity.desc"
        )
    }
}

// 15. Выход из зоны комфорта
val MovieComfortZoneBreakBlueprint = blueprint("MOVIE_COMFORT_ZONE_BREAK") {
    condition { true }
    score { 0.50 }
    segment {
        title = t("blueprint.movie_comfort_zone_break.title")
        subtitle = t("blueprint.movie_comfort_zone_break.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_genres" to "9648",
            "vote_average.gte" to "8.5",
            "vote_count.gte" to "2000",
            "sort_by" to "vote_average.desc"
        )
    }
}

val MovieTabBlueprints: List<InterpreterBlueprint> = listOf(
    MovieContinueWatchingBlueprint,
    MovieTopGenreHeroBlueprint,
    MovieBecauseYouWatchedBlueprint,
    MovieMindbenderBlueprint,
    MovieStudioAestheticBlueprint,
    MovieTopActorBlueprint,
    MovieTopDirectorBlueprint,
    MovieNostalgiaEraBlueprint,
    MovieIndieGemsBlueprint,
    MovieAdrenalineSpikeBlueprint,
    MovieFamilyNightBlueprint,
    MovieHeartwarmingBlueprint,
    MovieHiddenTreasuresBlueprint,
    MovieGuiltyPleasuresBlueprint,
    MovieComfortZoneBreakBlueprint
)
