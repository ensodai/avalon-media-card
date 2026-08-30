package org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.SectionType
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.GenreTranslationLayer
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

enum class MoodKey(val value: String) {
    DARK("dark"),
    JOY("joy"),
    LAUGH("laugh"),
    TENSION("tension"),
    INTELLECTUAL("intellectual"),
    ADRENALINE("adrenaline")
}

/**
 * Шаблон "Обеденный перерыв" (TEMP_LUNCH_BREAK)
 *
 * Группа 1: Синхронизация с циркадными ритмами.
 * Психологическое обоснование: Жесткий тайм-менеджмент. Зритель знает, что серия точно закончится до конца перерыва.
 * Время активации: 12:00 - 15:00.
 *
 * Логика TMDB:
 * Строгий фокус на телевизионный контент (Ситкомы, Процедуралы) через `with_type=4` (Scripted TV) 
 * и жесткое ограничение хронометража до 25 минут. Сортировка по популярности, чтобы не тратить время на выбор.
 */
val TempLunchBreakBlueprint = blueprint("TEMP_LUNCH_BREAK") {
    condition { context.localHour in 12..15 }
    score { calculateRelevance(1.0, 1.8) }
    segment {
        title = t("blueprint.temp_lunch_break.title")
        subtitle = t("blueprint.temp_lunch_break.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_type" to "4",
            "with_runtime.lte" to "25",
            "sort_by" to "popularity.desc"
        )
    }
}

/**
 * Шаблон "Утренняя спешка" (TEMP_MORNING_RUSH)
 *
 * Группа 1: Синхронизация с циркадными ритмами.
 * Психологическое обоснование: Снижение трения. Контент для фонового потребления во время сборов.
 * Время активации: 06:00 - 10:00 (только будни).
 * 
 * Логика TMDB: 
 * Мы намеренно хардкодим жанры (Анимация | Комедия) и ограничиваем хронометраж (<= 30 мин),
 * так как утром зритель не готов к глубокому вовлечению в свои любимые серьезные жанры.
 * 
 * Зависимость от вектора: Сила рекомендации напрямую зависит от вектора скорости (pacingWeights["fast"]).
 */
val TempMorningRushBlueprint = blueprint("TEMP_MORNING_RUSH") {
    condition { !context.isWeekend && context.localHour in 6..10 }
    val fastPacing = context.affinityVector.pacingWeights["fast"] ?: 0.5
    score { calculateRelevance(fastPacing, 1.8) }
    segment {
        title = t("blueprint.temp_morning_rush.title")
        subtitle = t("blueprint.temp_morning_rush.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_runtime.lte" to "30",
            "with_genres" to "16|35"
        )
    }
}

/**
 * Шаблон "Вечерняя разгрузка" (TEMP_AFTER_WORK_DECOMPRESS)
 *
 * Группа 1: Синхронизация с циркадными ритмами.
 * Психологическое обоснование: Эго-истощение. Мозг отказывается принимать сложные решения после работы, 
 * требуется гарантированное качество в максимально знакомом и надежном жанре.
 * Время активации: 18:00 - 21:00 (будни).
 *
 * Логика TMDB:
 * Берется самый любимый жанр (Top-1) зрителя, но строго исключаются документальные фильмы (`without_genres=99`), 
 * так как они требуют слишком высокой концентрации внимания. Рейтинг ограничен `vote_average.gte=7.0`.
 */
val TempAfterWorkDecompressBlueprint = blueprint("TEMP_AFTER_WORK_DECOMPRESS") {
    condition { !context.isWeekend && context.localHour in 18..21 }
    val topGenreId = context.topGenres.firstOrNull() ?: reject()
    val genreAffinity = context.affinityVector.genreWeights[topGenreId] ?: 0.0

    var baseScore = genreAffinity * 1.5
    baseScore = baseScore.coerceIn(0.0, 1.0)
    condition { baseScore >= 0.4 }

    val genreName = context.localizedGenres[topGenreId] ?: "Ваш любимый жанр"

    score { calculateRelevance(baseScore, 1.5) }

    segment {
        title = if (genreName.isNotBlank()) t("blueprint.temp_after_work_decompress.title", genreName) else t("blueprint.temp_after_work_decompress.title_fallback")
        subtitle = t("blueprint.temp_after_work_decompress.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_genres" to topGenreId,
            "vote_average.gte" to "7.0",
            "without_genres" to "99"
        )
    }
}

/**
 * Шаблон "Изоляция сюжетного тропа" (SERENDIPITY_TROPE_ISOLATION)
 *
 * Группа 4: Разрушение эхо-камеры (Серендипные шаблоны).
 * Психологическое обоснование: Математический парадокс и создание AHA-момента. Намеренно выкидывает 
 * пользователя из зоны комфорта, предлагая его любимый сюжетный троп (keyword) в совершенно нетипичном жанре.
 * 
 * Логика TMDB:
 * Ищет совпадение по самому любимому ключевому слову зрителя (`with_keywords`), но при этом жестко блокирует 
 * выдачу Топ-2 самых любимых жанров зрителя (`without_genres`). 
 * Оценка релевантности использует формулу серендипности с множителем x4.0 для пробития барьера.
 */
val SerendipityTropeIsolationBlueprint = blueprint("SERENDIPITY_TROPE_ISOLATION") {
    val topKeywordId = context.topKeywords.firstOrNull() ?: reject()
    val keywordAffinity = context.affinityVector.keywordWeights[topKeywordId] ?: 0.0
    val topGenreId = context.topGenres.firstOrNull() ?: reject()
    val genreAffinity = context.affinityVector.genreWeights[topGenreId] ?: 0.0

    condition { keywordAffinity >= 0.7 }

    val serendipityMultiplier = (1.0 - genreAffinity) * keywordAffinity * 4.0
    var baseScore = (keywordAffinity * 0.8) * serendipityMultiplier
    baseScore = baseScore.coerceIn(0.0, 1.0)

    condition { baseScore >= 0.5 }

    val top2Genres = context.affinityVector.genreWeights.entries
        .sortedByDescending { it.value }
        .take(2)
        .map { it.key }
        .joinToString(",")

    condition { top2Genres.isNotEmpty() }

    score { calculateRelevance(baseScore, serendipityMultiplier = 4.0) }

    segment {
        title = t("blueprint.serendipity_trope_isolation.title")
        subtitle = t("blueprint.serendipity_trope_isolation.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_keywords" to topKeywordId,
            "without_genres" to top2Genres,
            "vote_average.gte" to "7.0"
        )
    }
}

/**
 * Шаблон "Синергия актера и настроения" (BLUEPRINT_ACTOR_MOOD_SYNERGY)
 *
 * Группа 3: Ассоциативные паттерны.
 * Психологическое обоснование: Знакомое лицо снижает порог входа для нового или необычного контента. 
 * Слияние любимого актера и текущего доминирующего настроения (например, "мрачный фильм с Джимом Керри").
 *
 * Логика TMDB:
 * Берет самого любимого актера (`with_cast`) и скрещивает его с самым сильным настроением из вектора 
 * (настроение маппится на `with_keywords`). Рейтинг релевантности зависит от привязанности к актеру.
 */
val ActorMoodSynergyBlueprint = blueprint("BLUEPRINT_ACTOR_MOOD_SYNERGY") {
    condition { context.affinityVector.actorWeights.isNotEmpty() && context.affinityVector.moodWeights.isNotEmpty() }

    val topActor = context.affinityVector.actorWeights.maxByOrNull { it.value }?.key ?: reject()
    val topMood = context.affinityVector.moodWeights.maxByOrNull { it.value }?.key ?: reject()
    val actorScore = context.affinityVector.actorWeights[topActor]!!

    score { calculateRelevance(actorScore, serendipityMultiplier = 1.2) }

    segment {
        title = t("blueprint.actor_mood_synergy.title_fallback")
        subtitle = t("blueprint.actor_mood_synergy.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_AVATARS
        params = mapOf(
            "with_cast" to topActor,
            "with_keywords" to topMood
        )
    }
}

/**
 * Шаблон "Стилистическая синергия режиссера и студии" (BLUEPRINT_DIRECTOR_STUDIO_SYNERGY)
 *
 * Группа 3: Ассоциативные паттерны (Исследование 9).
 * Психологическое обоснование: Гарантия качества через комбинацию. Если зритель ценит авторский почерк режиссера 
 * и стиль определенной студии (например, A24 или HBO), их пересечение вызывает максимальное доверие к контенту.
 *
 * Логика TMDB:
 * Ищет фильмы, где пересекаются топ-1 режиссер (`with_crew`) и топ-1 студия (`with_companies`) пользователя.
 */
val DirectorStudioSynergyBlueprint = blueprint("BLUEPRINT_DIRECTOR_STUDIO_SYNERGY") {
    condition { context.affinityVector.directorWeights.isNotEmpty() && context.affinityVector.companyWeights.isNotEmpty() }

    val topDirector = context.affinityVector.directorWeights.maxByOrNull { it.value }?.key ?: reject()
    val topCompany = context.affinityVector.companyWeights.maxByOrNull { it.value }?.key ?: reject()

    val directorScore = context.affinityVector.directorWeights[topDirector]!!
    val companyScore = context.affinityVector.companyWeights[topCompany]!!

    condition { directorScore > 0.3 && companyScore > 0.3 }

    score { calculateRelevance(directorScore * companyScore, serendipityMultiplier = 1.5) }

    segment {
        title = t("blueprint.director_studio_synergy.title")
        subtitle = t("blueprint.director_studio_synergy.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_crew" to topDirector,
            "with_companies" to topCompany
        )
    }
}

/**
 * Шаблон "Триггер ностальгии" (BLUEPRINT_NOSTALGIA_TRIGGER)
 *
 * Группа 2: Временные и эмоциональные якоря (Исследование 9).
 * Психологическое обоснование: Эскапизм через возврат в прошлое. Старые фильмы в любимом жанре 
 * воспринимаются теплее в периоды стресса.
 *
 * Логика TMDB:
 * Ограничивает `primary_release_date.lte` датой 20-летней давности, жестко фиксируя любимый жанр.
 * Сортировка по зрительским оценкам.
 */
val NostalgiaTriggerBlueprint = blueprint("BLUEPRINT_NOSTALGIA_TRIGGER") {
    val topGenreId = context.topGenres.firstOrNull() ?: reject()
    val eraWeight = context.affinityVector.eraWeights["2000s"] ?: 0.5

    score { calculateRelevance(eraWeight, 1.2) }

    segment {
        title = t("blueprint.nostalgia_trigger.title")
        subtitle = t("blueprint.nostalgia_trigger.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to topGenreId,
            "primary_release_date.lte" to "2006-01-01",
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "500"
        )
    }
}

/**
 * Шаблон "Сериалы на выходные" (BINGE_WATCHING_WEEKEND)
 *
 * Группа 1: Синхронизация с циркадными ритмами (Исследование 9).
 * Психологическое обоснование: Binge-watching (запойный просмотр). Пользователь готов к марафону, 
 * поэтому предпочтение отдается ЗАВЕРШЕННЫМ сериалам, чтобы избежать фрустрации от ожидания сезонов.
 *
 * Логика TMDB:
 * Строго выходные дни. Ищет ТВ-шоу (`with_type=0|4`) со статусом "Завершен" (`with_status=4`).
 */
val BingeWatchingWeekendBlueprint = blueprint("BINGE_WATCHING_WEEKEND") {
    condition { context.isWeekend }

    // Берем топ 1-2 сериальных жанра пользователя
    val topGenres = context.topGenres.take(2).joinToString("|")
    condition { topGenres.isNotEmpty() }

    score { calculateRelevance(0.8, temporalMultiplier = 1.5) }

    segment {
        title = t("blueprint.binge_watching_weekend.title")
        subtitle = t("blueprint.binge_watching_weekend.subtitle")
        type = EntityType.TV
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_genres" to topGenres,
            "with_status" to "3", // Исправлено: 3 означает Ended (Завершен), 4 - Cancelled (Отменен)
            "sort_by" to "popularity.desc"
        )
    }
}

/**
 * Шаблон "Выход из эхо-камеры" (ECHO_CHAMBER_BREAKER)
 *
 * Группа 4: Разрушение эхо-камеры (Исследование 9).
 * Психологическое обоснование: Борьба с алгоритмической слепотой. Пользователь застревает в одних и тех же 
 * жанрах (эхо-камера). Шаблон предлагает контент из АБСОЛЮТНО нелюбимого жанра, но с монументально высоким рейтингом.
 *
 * Логика TMDB:
 * Берет жанр с наименьшим весом в векторе (или вообще отсутствующий). Запрашивает фильмы с 
 * `vote_average.gte=8.5` и огромным количеством голосов.
 */
val EchoChamberBreakerBlueprint = blueprint("ECHO_CHAMBER_BREAKER") {
    // Находим жанр с наименьшим весом
    val lowestGenreId = context.affinityVector.genreWeights.entries.minByOrNull { it.value }?.key ?: reject()
    val lowestAffinity = context.affinityVector.genreWeights[lowestGenreId] ?: 0.0

    condition { lowestAffinity < 0.2 } // Жанр действительно должен быть "нелюбимым"

    // Множитель серендипности максимальный
    score { calculateRelevance(1.0 - lowestAffinity, serendipityMultiplier = 5.0) }

    segment {
        title = t("blueprint.echo_chamber_breaker.title")
        subtitle = t("blueprint.echo_chamber_breaker.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to lowestGenreId,
            "vote_average.gte" to "8.5",
            "vote_count.gte" to "3000"
        )
    }
}

/**
 * Шаблон "Исследование микро-ниши" (MICRO_NICHE_EXPLORATION)
 *
 * Группа 4: Разрушение эхо-камеры (Исследование 9).
 * Психологическое обоснование: Чувство первооткрывателя. Сужение поиска до специфического пересечения 
 * двух не самых очевидных тегов пользователя.
 *
 * Логика TMDB:
 * Берет 2-й и 3-й по популярности keyword пользователя (чтобы избежать банального топ-1) 
 * и ищет их строгое пересечение.
 */
val MicroNicheExplorationBlueprint = blueprint("MICRO_NICHE_EXPLORATION") {
    condition { context.topKeywords.size >= 3 }

    val keywords = context.topKeywords.drop(1).take(2)
    val combinedKeywords = keywords.joinToString(",") // AND условие в TMDB

    score { calculateRelevance(0.7, serendipityMultiplier = 2.0) }

    segment {
        title = t("blueprint.micro_niche_exploration.title")
        subtitle = t("blueprint.micro_niche_exploration.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_keywords" to combinedKeywords,
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "100"
        )
    }
}

/**
 * Шаблон "Ночная меланхолия" (TEMP_MIDNIGHT_MELANCHOLY)
 *
 * Группа 1: Синхронизация с циркадными ритмами.
 * Психологическое обоснование: Эксплуатация ночной меланхолии и социальной изоляции. 
 * Глубокое погружение, когда зритель никуда не торопится.
 * Время активации: 23:00 - 04:00.
 *
 * Логика TMDB:
 * Ищем фильмы с `with_runtime.gte=100` (длинные), с мрачными/атмосферными тегами (1701, 4152),
 * и опираемся на любовь зрителя к медленному темпу (`pacingWeights["slow"]`).
 */
val TempMidnightMelancholyBlueprint = blueprint("TEMP_MIDNIGHT_MELANCHOLY") {
    // В Kotlin % 24 корректно обрабатывает отрицательные числа только через rem, поэтому проще через or
    condition { context.localHour in 23..23 || context.localHour in 0..4 }

    val slowPacing = context.affinityVector.pacingWeights["slow"] ?: 0.5
    condition { slowPacing >= 0.3 }

    score { calculateRelevance(slowPacing, temporalMultiplier = 1.6) }

    segment {
        title = t("blueprint.temp_midnight_melancholy.title")
        subtitle = t("blueprint.temp_midnight_melancholy.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_runtime.gte" to "100",
            "with_keywords" to "1701|4152"
        )
    }
}

/**
 * Шаблон "Эпичные марафоны" (TEMP_WEEKEND_EPIC_BINGE)
 *
 * Группа 1: Синхронизация с циркадными ритмами.
 * Психологическое обоснование: Максимальная готовность к длительному вовлечению.
 * Выходные — идеальное время для длинных эпических полотен, которые зритель откладывает в будни.
 * День активации: Суббота / Воскресенье.
 *
 * Логика TMDB:
 * Ищем фильмы с хронометражем от 150 минут (`with_runtime.gte=150`).
 * Фокус на масштабные жанры — Фантастика или Приключения (`with_genres=12,878`),
 * и сортируем по кассовым сборам для гарантии зрелищности (`sort_by=revenue.desc`).
 */
val TempWeekendEpicBingeBlueprint = blueprint("TEMP_WEEKEND_EPIC_BINGE") {
    condition { context.isWeekend }

    val genreAffinity12 = context.affinityVector.genreWeights["12"] ?: 0.0 // Adventure
    val genreAffinity878 = context.affinityVector.genreWeights["878"] ?: 0.0 // Sci-Fi
    val maxAffinity = maxOf(genreAffinity12, genreAffinity878)

    condition { maxAffinity > 0.2 }

    score { calculateRelevance(maxAffinity, temporalMultiplier = 1.8) }

    segment {
        title = t("blueprint.temp_weekend_epic_binge.title")
        subtitle = t("blueprint.temp_weekend_epic_binge.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_runtime.gte" to "150",
            "with_genres" to "12,878",
            "sort_by" to "revenue.desc"
        )
    }
}

/**
 * Шаблон "Уютный вечер воскресенья" (TEMP_SUNDAY_COZY)
 *
 * Группа 1: Синхронизация с циркадными ритмами.
 * Психологическое обоснование: Синдром вечера воскресенья (Sunday Scaries / утренняя тревога перед рабочей неделей).
 * Требуется максимально безопасный контент без лишнего напряжения, крови и драм.
 * День активации: Воскресенье (вторая половина дня).
 *
 * Логика TMDB:
 * Ищем фильмы жанров Семейное или Комедия (`with_genres=10751|35`),
 * жестко блокируем теги, связанные с выживанием и смертью (`without_keywords=10349`),
 * и пропускаем только легкие рейтинги (`certification=PG|PG-13`).
 */
val TempSundayCozyBlueprint = blueprint("TEMP_SUNDAY_COZY") {
    // В Kotlin Calendar.SUNDAY = 1. Мы уже используем context.isWeekend, но здесь нам важно именно воскресенье вечер.
    condition { context.isWeekend && context.localHour in 16..22 }

    // Блокируем, если юзер сейчас настроен на сильное напряжение/ужасы
    val tensionMood = context.affinityVector.moodWeights[MoodKey.TENSION.value] ?: 0.0
    condition { tensionMood < 0.3 }

    score { calculateRelevance(0.8, temporalMultiplier = 1.8) }

    segment {
        title = t("blueprint.temp_sunday_cozy.title")
        subtitle = t("blueprint.temp_sunday_cozy.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "10751|35",
            "without_keywords" to "10349", // Без выживания/жести
            "certification" to "PG|PG-13",
            "certification_country" to "US", // TMDB API требует страну для сертификации
            "sort_by" to "popularity.desc"
        )
    }
}

/**
 * Шаблон "Вайб студии" (AESTHETIC_STUDIO_VIBE)
 *
 * Группа 2: Стилистические шаблоны.
 * Психологическое обоснование: Создает ощущение элитарности. Зритель чувствует принадлежность 
 * к закрытому клубу ценителей стиля конкретной студии (например, A24, Neon, HBO).
 *
 * Логика TMDB:
 * Берем самую любимую студию (`companyWeights`), но намеренно исключаем масс-маркет жанры 
 * (например, Боевик — 28), чтобы не показывать блокбастеры. Сортируем по оценкам, 
 * отсекая фильмы с малым числом голосов (`vote_count.gte=500`).
 */
val AestheticStudioVibeBlueprint = blueprint("AESTHETIC_STUDIO_VIBE") {
    condition { context.affinityVector.companyWeights.isNotEmpty() }

    val topCompanyId = context.affinityVector.companyWeights.maxByOrNull { it.value }?.key ?: reject()
    val companyAffinity = context.affinityVector.companyWeights[topCompanyId]!!

    condition { companyAffinity >= 0.4 }

    score { calculateRelevance(companyAffinity, serendipityMultiplier = 1.3) }

    segment {
        title = t("blueprint.aesthetic_studio_vibe.title")
        subtitle = t("blueprint.aesthetic_studio_vibe.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_companies" to topCompanyId,
            "without_genres" to "28", // Исключаем типичные боевики
            "vote_count.gte" to "500",
            "sort_by" to "vote_average.desc"
        )
    }
}

/**
 * Шаблон "Темы автора" (AESTHETIC_AUTEUR_THEMES)
 *
 * Группа 2: Стилистические шаблоны.
 * Психологическое обоснование: Имитирует эрудицию профессионального кинокритика. 
 * Выявляет темы, характерные для любимого режиссера пользователя, и ищет фильмы 
 * ДРУГИХ авторов в той же стилистике (расширение кругозора).
 *
 * Логика TMDB:
 * Берем Топ-3 ключевых слова (keywordWeights) и ищем фильмы с этими тропами, 
 * жестко исключая самого любимого режиссера (`without_crew`).
 */
val AestheticAuteurThemesBlueprint = blueprint("AESTHETIC_AUTEUR_THEMES") {
    condition { context.affinityVector.directorWeights.isNotEmpty() && context.topKeywords.size >= 3 }

    val topDirectorId = context.affinityVector.directorWeights.maxByOrNull { it.value }?.key ?: reject()
    val directorAffinity = context.affinityVector.directorWeights[topDirectorId]!!

    val topKeywords = context.topKeywords.take(3).joinToString("|") // ИЛИ условие для расширения воронки

    score { calculateRelevance(directorAffinity, serendipityMultiplier = 2.0) }

    segment {
        title = t("blueprint.aesthetic_auteur_themes.title")
        subtitle = t("blueprint.aesthetic_auteur_themes.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_keywords" to topKeywords,
            "without_crew" to topDirectorId,
            "vote_count.gte" to "500",
            "sort_by" to "vote_average.desc"
        )
    }
}

/**
 * Шаблон "Ностальгия по эпохе" (AESTHETIC_DECADE_NOSTALGIA)
 *
 * Группа 2: Стилистические шаблоны.
 * Психологическое обоснование: Настоящий триггер ностальгии. Отсеивает современные ремейки, 
 * возвращая пользователя в зону абсолютного комфорта любимой эпохи (например, 80-е).
 *
 * Логика TMDB:
 * Ищет пик в `eraWeights`. Извлекает годы (например, "1980s" -> 1980-01-01 .. 1989-12-31).
 * Добавляет `with_genres` любимого жанра для 100% попадания.
 */
val AestheticDecadeNostalgiaBlueprint = blueprint("AESTHETIC_DECADE_NOSTALGIA") {
    condition { context.affinityVector.eraWeights.isNotEmpty() && context.topGenres.isNotEmpty() }

    val topEraEntry = context.affinityVector.eraWeights.maxByOrNull { it.value } ?: reject()
    val topEraStr = topEraEntry.key // например, "1980s"
    val eraAffinity = topEraEntry.value

    // Эпоха должна быть ярко выраженной
    condition { eraAffinity >= 0.5 }

    val yearStart = topEraStr.removeSuffix("s").toIntOrNull() ?: reject()
    val yearEnd = yearStart + 9

    val topGenre = context.topGenres.first()

    score { calculateRelevance(eraAffinity, serendipityMultiplier = 1.0) }

    segment {
        title = t("blueprint.aesthetic_decade_nostalgia.title", topEraStr)
        subtitle = t("blueprint.aesthetic_decade_nostalgia.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "primary_release_date.gte" to "$yearStart-01-01",
            "primary_release_date.lte" to "$yearEnd-12-31",
            "with_genres" to topGenre,
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "500"
        )
    }
}

/**
 * Шаблон "Инди-жемчужины" (AESTHETIC_INDIE_DARLINGS)
 *
 * Группа 2: Стилистические шаблоны.
 * Психологическое обоснование: Для синефилов, ищущих скрытые инди-жемчужины.
 * Зритель готов к медленному повествованию и авторским высказываниям.
 * 
 * Логика TMDB:
 * Ищет независимое кино (тег `3384` - independent film) с высокими оценками (`vote_average.gte=7.5`),
 * но малым числом голосов (`vote_count.lte=1500`), чтобы исключить мейнстримные инди-хиты (как у А24).
 * Активируется только при высокой терпимости к медленному темпу.
 */
val AestheticIndieDarlingsBlueprint = blueprint("AESTHETIC_INDIE_DARLINGS") {
    val fastPacing = context.affinityVector.pacingWeights["fast"] ?: 1.0
    // Терпимость к медленному повествованию = низкий вес у "fast"
    condition { fastPacing < 0.3 }

    val indieAffinity = 1.0 - fastPacing // Чем меньше фаст-пейсинг, тем выше аффинити

    score { calculateRelevance(indieAffinity, serendipityMultiplier = 2.5) }

    segment {
        title = t("blueprint.aesthetic_indie_darlings.title")
        subtitle = t("blueprint.aesthetic_indie_darlings.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_keywords" to "3384",
            "vote_average.gte" to "7.5",
            "vote_count.gte" to "100", // Минимальный порог, чтобы не брать откровенный треш с 1 голосом 10/10
            "vote_count.lte" to "1500", // Отсекаем мейнстрим
            "sort_by" to "vote_average.desc"
        )
    }
}

/**
 * Шаблон "ЧБ эстетика" (AESTHETIC_BLACK_AND_WHITE)
 *
 * Группа 2: Стилистические шаблоны.
 * Психологическое обоснование: Акцент на чистой кинематографии и классике/современном артхаусе.
 * Предназначен для эстетов и синефилов, которые ищут визуально выразительное кино.
 * 
 * Логика TMDB:
 * Ищет фильмы с тегами "black and white" (236 или 240). 
 * Жестко исключает жанры анимации (16) и боевиков (28).
 * Активируется только при высокой терпимости к медленному темпу повествования.
 */
val AestheticBlackAndWhiteBlueprint = blueprint("AESTHETIC_BLACK_AND_WHITE") {
    val fastPacing = context.affinityVector.pacingWeights["fast"] ?: 1.0
    // Терпимость к вдумчивому повествованию
    condition { fastPacing < 0.4 }

    val bwAffinity = 1.0 - fastPacing

    score { calculateRelevance(bwAffinity, serendipityMultiplier = 1.8) }

    segment {
        title = t("blueprint.aesthetic_black_and_white.title")
        subtitle = t("blueprint.aesthetic_black_and_white.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_keywords" to "236|240", // Black and White
            "without_genres" to "16,28", // Исключаем анимацию и боевики
            "vote_count.gte" to "300",
            "sort_by" to "vote_average.desc"
        )
    }
}

/**
 * Шаблон "Зарубежные хиты" (AESTHETIC_FOREIGN_GEMS)
 *
 * Группа 2: Стилистические шаблоны.
 * Психологическое обоснование: Рекомендация зарубежных хитов (Европа/Азия) для пользователей,
 * готовых к вдумчивому повествованию и открытых к субтитрам.
 * 
 * Логика TMDB:
 * Ищет популярное кино (`vote_count.gte=1000`) на языках, отличных от английского 
 * (мы используем список ключевых языков мирового кинематографа: fr|ko|ja|it|es|de).
 * Активируется при высокой терпимости к серендипности/медленному темпу 
 * (т.к. зарубежное кино часто воспринимается как артхаус).
 */
val AestheticForeignGemsBlueprint = blueprint("AESTHETIC_FOREIGN_GEMS") {
    val fastPacing = context.affinityVector.pacingWeights["fast"] ?: 1.0
    // Терпимость к вдумчивому повествованию
    condition { fastPacing < 0.4 }

    val foreignAffinity = 1.0 - fastPacing

    score { calculateRelevance(foreignAffinity, serendipityMultiplier = 2.0) }

    segment {
        title = t("blueprint.aesthetic_foreign_gems.title")
        subtitle = t("blueprint.aesthetic_foreign_gems.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS
        params = mapOf(
            "with_original_language" to "fr|ko|ja|it|es|de|da|sv", // Европа и Азия
            "vote_count.gte" to "1000",
            "sort_by" to "vote_average.desc"
        )
    }
}

/**
 * Шаблон "Черная комедия" (MOOD_DARK_COMEDY_MIX)
 *
 * Группа 3: Шаблоны настроения.
 * Психологическое обоснование: Объединяет два противоречивых вектора в один кураторский слот.
 * Решает ситуацию, когда зритель хочет посмеяться, но при этом находится в напряжении/тревоге.
 * 
 * Логика TMDB:
 * Запрашиваем фильмы, у которых есть И Комедия (35) И Триллер (53).
 * Дополнительно фильтруем по тегу Dark Comedy (9714).
 */
val MoodDarkComedyMixBlueprint = blueprint("MOOD_DARK_COMEDY_MIX") {
    val laughMood = context.affinityVector.moodWeights[MoodKey.LAUGH.value] ?: 0.0
    val tensionMood = context.affinityVector.moodWeights[MoodKey.TENSION.value] ?: 0.0

    // Активируется только когда оба вектора достаточно сильны
    condition { laughMood > 0.4 && tensionMood > 0.4 }

    val combinedAffinity = (laughMood + tensionMood) / 2.0

    // Очень высокая серендипность, т.к. это крайне точное попадание в сложное эмоциональное состояние
    score { calculateRelevance(combinedAffinity, serendipityMultiplier = 2.5) }

    segment {
        title = t("blueprint.mood_dark_comedy_mix.title")
        subtitle = t("blueprint.mood_dark_comedy_mix.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "35,53", // Комедия И Триллер (запятая = AND в TMDB)
            "with_keywords" to "9714", // Dark Comedy
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "500"
        )
    }
}

/**
 * Шаблон "Клаустрофобия" (MOOD_CLAUSTROPHOBIA)
 *
 * Группа 3: Шаблоны настроения.
 * Психологическое обоснование: Выбор для зрителя, ищущего замкнутый, медленно нагнетающийся саспенс.
 *
 * Логика TMDB:
 * Запрашиваем фильмы с тегами Клаустрофобия (4379) ИЛИ Одна локация (10292).
 * Активируется только при высоком запросе на напряжение, но готовности к медленному темпу 
 * (т.к. фильмы в одной комнате обычно строятся на диалогах).
 */
val MoodClaustrophobiaBlueprint = blueprint("MOOD_CLAUSTROPHOBIA") {
    val tensionMood = context.affinityVector.moodWeights[MoodKey.TENSION.value] ?: 0.0
    val fastPacing = context.affinityVector.pacingWeights["fast"] ?: 1.0

    // Напряжение должно быть высоким, но темп - вдумчивым
    condition { tensionMood > 0.4 && fastPacing < 0.4 }

    // Чем ниже фаст пейсинг и выше теншен - тем лучше
    val claustroAffinity = (tensionMood + (1.0 - fastPacing)) / 2.0

    score { calculateRelevance(claustroAffinity, serendipityMultiplier = 1.5) }

    segment {
        title = t("blueprint.mood_claustrophobia.title")
        subtitle = t("blueprint.mood_claustrophobia.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_keywords" to "4379|10292", // Claustrophobia ИЛИ One Location
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "500"
        )
    }
}

/**
 * Шаблон "Головоломки" (MOOD_MIND_BENDER)
 *
 * Группа 3: Шаблоны настроения.
 * Психологическое обоснование: Для зрителя, который хочет, чтобы кино бросало вызов его интеллекту.
 *
 * Логика TMDB:
 * Ищет фильмы с тегами Mindfuck (4842) ИЛИ Внезапный поворот сюжета (12988).
 * Жестко исключает комедии (35) и семейное кино (10751), чтобы не размывать серьезность тона.
 * Активируется высоким весом интеллектуального настроения (intellectual).
 */
val MoodMindBenderBlueprint = blueprint("MOOD_MIND_BENDER") {
    val intellectualMood = context.affinityVector.moodWeights[MoodKey.INTELLECTUAL.value] ?: 0.0

    // Активируется при запросе на интеллектуальное кино
    condition { intellectualMood > 0.4 }

    score { calculateRelevance(intellectualMood, serendipityMultiplier = 1.6) }

    segment {
        title = t("blueprint.mood_mind_bender.title")
        subtitle = t("blueprint.mood_mind_bender.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_keywords" to "4842|12988", // Mindfuck ИЛИ Plot Twist
            "without_genres" to "10751,35", // Без комедий и семейного
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "1000" // Головоломки должны быть признанными
        )
    }
}

/**
 * Шаблон "Суровое кино" (MOOD_UNAPOLOGETIC_GRIT)
 *
 * Группа 3: Шаблоны настроения.
 * Психологическое обоснование: Рекомендация сурового, мрачного кино для тех, кто устал от "ванильных" сюжетов.
 * 
 * Логика TMDB:
 * Ищет фильмы с тегами Gritty (14606), Dark (33810) ИЛИ Neo-noir (10077).
 * Жестко исключает комедии (35) и семейное кино (10751).
 * Активируется высоким запросом на мрачность.
 */
val MoodUnapologeticGritBlueprint = blueprint("MOOD_UNAPOLOGETIC_GRIT") {
    val darkMood = context.affinityVector.moodWeights[MoodKey.DARK.value] ?: 0.0

    // Активируется только когда зритель готов к суровой реальности
    condition { darkMood > 0.4 }

    score { calculateRelevance(darkMood, serendipityMultiplier = 1.4) }

    segment {
        title = t("blueprint.mood_unapologetic_grit.title")
        subtitle = t("blueprint.mood_unapologetic_grit.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_keywords" to "14606|33810|10077", // Gritty ИЛИ Dark ИЛИ Neo-noir
            "without_genres" to "35,10751", // Без комедий и семейного
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "500"
        )
    }
}

/**
 * Шаблон "Душевное кино" (MOOD_HEARTWARMING)
 *
 * Группа 3: Шаблоны настроения.
 * Психологическое обоснование: Для тех, кому нужна эмоциональная поддержка от кино.
 * 
 * Логика TMDB:
 * Ищет фильмы с тегами Heartwarming (9799) ИЛИ Feel-good (13130).
 * Жестко исключает триллеры (53), ужасы (27) и криминал (80).
 * Активируется высоким запросом на радость/уют (joy).
 */
val MoodHeartwarmingBlueprint = blueprint("MOOD_HEARTWARMING") {
    val joyMood = context.affinityVector.moodWeights[MoodKey.JOY.value] ?: 0.0

    // Активируется при высоком запросе на радость и уют
    condition { joyMood > 0.4 }

    score { calculateRelevance(joyMood, serendipityMultiplier = 1.3) }

    segment {
        title = t("blueprint.mood_heartwarming.title")
        subtitle = t("blueprint.mood_heartwarming.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_keywords" to "9799|13130", // Heartwarming ИЛИ Feel-good
            "without_genres" to "53,27,80", // Без триллеров, ужасов и криминала
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "500"
        )
    }
}

/**
 * Шаблон "Адреналин" (MOOD_ADRENALINE_SPIKE)
 *
 * Группа 3: Шаблоны настроения.
 * Психологическое обоснование: Для зрителя, который хочет просто расслабиться, 
 * выключить мозг и смотреть на эпичные взрывы и экшен.
 * 
 * Логика TMDB:
 * Ищет фильмы в жанре Боевик (28) со специфическими тегами: 
 * Супергероика (9715), Боевые искусства (10051) ИЛИ Автомобильные погони (10090).
 * Сортирует по популярности (эпичные блокбастеры).
 * Активируется высоким запросом на адреналин И высоким темпом (fastPacing).
 */
val MoodAdrenalineSpikeBlueprint = blueprint("MOOD_ADRENALINE_SPIKE") {
    val adrenalineMood = context.affinityVector.moodWeights[MoodKey.ADRENALINE.value] ?: 0.0
    val fastPacing = context.affinityVector.pacingWeights["fast"] ?: 0.0

    // Активируется только при мощном запросе на экшен и скорость
    condition { adrenalineMood > 0.4 && fastPacing > 0.4 }

    val combinedAffinity = (adrenalineMood + fastPacing) / 2.0
    score { calculateRelevance(combinedAffinity, serendipityMultiplier = 1.2) }

    segment {
        title = t("blueprint.mood_adrenaline_spike.title")
        subtitle = t("blueprint.mood_adrenaline_spike.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "28", // Боевик
            "with_keywords" to "9715|10051|10090", // Superhero ИЛИ Martial Arts ИЛИ Car Chase
            "sort_by" to "popularity.desc" // Масштаб решает
        )
    }
}

/**
 * Шаблон "Неожиданное амплуа актера" (SERENDIPITY_ACTOR_PIVOT)
 *
 * Группа 4: Серендипные шаблоны.
 * Психологическое обоснование: Показывает любимого актера зрителя в жанре, 
 * который совершенно не свойственен вкусу самого зрителя. 
 * Расширяет кругозор за счет знакомого лица.
 *
 * Логика TMDB:
 * Берет topActor и topGenre из векторов. Ищет фильмы с этим актером, 
 * но жестко исключает любимый жанр (without_genres=topGenre).
 */
val SerendipityActorPivotBlueprint = blueprint("SERENDIPITY_ACTOR_PIVOT") {
    condition { context.affinityVector.actorWeights.isNotEmpty() && context.affinityVector.genreWeights.isNotEmpty() }

    val topActor = context.affinityVector.actorWeights.maxByOrNull { it.value }?.key ?: reject()
    val actorScore = context.affinityVector.actorWeights[topActor] ?: 0.0
    val rawTopGenre = context.affinityVector.genreWeights.maxByOrNull { it.value }?.key ?: reject()

    // Трансляция любимого жанра (который может быть телевизионным) в пространство кино
    val topMovieGenre = GenreTranslationLayer.translate(rawTopGenre, from = EntityType.TV, to = EntityType.MOVIE)

    // Активируем только если актер действительно любимый
    condition { actorScore > 0.3 }

    // Высокий множитель серендипности, так как мы выталкиваем из зоны комфорта
    score { calculateRelevance(actorScore, serendipityMultiplier = 1.9) }

    segment {
        title = t("blueprint.serendipity_actor_pivot.title")
        subtitle = t("blueprint.serendipity_actor_pivot.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_cast" to topActor,
            "without_genres" to topMovieGenre,
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "300"
        )
    }
}

/**
 * Шаблон "Исторические корни жанра" (SERENDIPITY_DECADE_SWAP)
 *
 * Группа 4: Серендипные шаблоны.
 * Психологическое обоснование: Обучает зрителя истории его любимого жанра.
 * Берет жанр номер один, но резко отбрасывает зрителя в прошлое.
 *
 * Логика TMDB:
 * Ищет фильмы в topGenre, но с фильтром primary_release_date.lte (до 1990 года).
 * Высокий vote_count.gte гарантирует, что мы покажем признанную классику.
 */
val SerendipityDecadeSwapBlueprint = blueprint("SERENDIPITY_DECADE_SWAP") {
    condition { context.affinityVector.genreWeights.isNotEmpty() }

    val topGenre = context.affinityVector.genreWeights.maxByOrNull { it.value }?.key ?: reject()
    val genreScore = context.affinityVector.genreWeights[topGenre] ?: 0.0

    condition { genreScore > 0.3 }

    // Высокая серендипность, так как старое кино часто отпугивает, нужен буст
    score { calculateRelevance(genreScore, serendipityMultiplier = 1.7) }

    segment {
        title = t("blueprint.serendipity_decade_swap.title")
        subtitle = t("blueprint.serendipity_decade_swap.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to topGenre,
            "primary_release_date.lte" to "1990-12-31", // Все, что вышло до 1990 года
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "500" // Только признанные шедевры
        )
    }
}

/**
 * Шаблон "Жанровые гибриды" (SERENDIPITY_GENRE_BEND)
 *
 * Группа 4: Серендипные шаблоны.
 * Психологическое обоснование: Находит фильмы на жестком стыке двух любимых 
 * (но часто несовместимых) жанров зрителя.
 *
 * Логика TMDB:
 * Берет топ-2 жанра из genreWeights. Ищет фильмы, в которых 
 * ОБЯЗАТЕЛЬНО присутствуют оба (через запятую).
 */
val SerendipityGenreBendBlueprint = blueprint("SERENDIPITY_GENRE_BEND") {
    condition { context.affinityVector.genreWeights.size >= 2 }

    // Берем топ-2 любимых жанра
    val topGenres = context.affinityVector.genreWeights.entries
        .sortedByDescending { it.value }
        .take(2)

    val genre1 = topGenres[0]
    val genre2 = topGenres[1]

    // Активируется только если оба жанра достаточно сильны, чтобы был смысл их скрещивать
    condition { genre1.value > 0.3 && genre2.value > 0.3 }

    // Серендипность максимальная, так как это редкий гибрид
    val avgScore = (genre1.value + genre2.value) / 2.0
    score { calculateRelevance(avgScore, serendipityMultiplier = 2.0) }

    segment {
        title = t("blueprint.serendipity_genre_bend.title")
        subtitle = t("blueprint.serendipity_genre_bend.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "${genre1.key},${genre2.key}", // Запятая означает логическое И (AND)
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "200"
        )
    }
}

/**
 * Шаблон "Взрослая анимация" (SERENDIPITY_ANIMATION_TRAP)
 *
 * Группа 4: Серендипные шаблоны.
 * Психологическое обоснование: Ловит зрителей, которые игнорируют мультфильмы, 
 * но любят тяжелые, серьезные жанры. Взламываем их предубеждения.
 *
 * Логика TMDB:
 * Активируется только при НИЗКОМ score на жанр Animation (16) 
 * и ВЫСОКОМ score на Драму (18), Фантастику (878) или Триллер (53).
 * Ищет анимацию со взрослыми тегами: Cyberpunk (4565), Psychological (9673), Dark (33810).
 */
val SerendipityAnimationTrapBlueprint = blueprint("SERENDIPITY_ANIMATION_TRAP") {
    val animationScore = context.affinityVector.genreWeights["16"] ?: 0.0

    val sciFiScore = context.affinityVector.genreWeights["878"] ?: 0.0
    val thrillerScore = context.affinityVector.genreWeights["53"] ?: 0.0
    val dramaScore = context.affinityVector.genreWeights["18"] ?: 0.0

    val matureScore = maxOf(sciFiScore, thrillerScore, dramaScore)

    // Зритель не должен быть фанатом мультиков, но должен любить серьезное кино
    condition { animationScore < 0.2 && matureScore > 0.4 }

    // Редкое и меткое попадание, высокий множитель серендипности
    score { calculateRelevance(matureScore, serendipityMultiplier = 1.9) }

    segment {
        title = t("blueprint.serendipity_animation_trap.title")
        subtitle = t("blueprint.serendipity_animation_trap.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_genres" to "16", // Анимация
            "with_keywords" to "4565|9673|33810", // Cyberpunk ИЛИ Psychological ИЛИ Dark
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "200"
        )
    }
}

/**
 * Шаблон "Шедевры аниме" (ANIME_SPECIALIST_COHORT)
 * Выделенный шаблон для истинных ценителей аниме.
 */
val AnimeSpecialistBlueprint = blueprint("ANIME_SPECIALIST_COHORT") {
    val animationScore = context.affinityVector.genreWeights["16"] ?: 0.0

    condition { animationScore >= 0.3 }

    score { calculateRelevance(animationScore, serendipityMultiplier = 1.4) }

    segment {
        title = t("blueprint.anime_specialist_cohort.title")
        subtitle = t("blueprint.anime_specialist_cohort.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "16",
            "with_original_language" to "ja",
            "vote_average.gte" to "7.5",
            "vote_count.gte" to "150",
            "sort_by" to "popularity.desc"
        )
    }
}

/**
 * Шаблон "Слепая зона" (SERENDIPITY_BLIND_SPOT)
 *
 * Группа 4: Серендипные шаблоны.
 * Психологическое обоснование: Вытаскивает зрителя из зоны комфорта, 
 * предлагая абсолютный шедевр в жанре, который он обычно избегает или игнорирует.
 *
 * Логика TMDB:
 * Ищет жанр с наименьшим весом в профиле (minByOrNull).
 * Требует vote_count.gte = 2000, чтобы пробить блок недоверия признанным хитом.
 */
val SerendipityBlindSpotBlueprint = blueprint("SERENDIPITY_BLIND_SPOT") {
    // Активируется только если у нас уже достаточно широкая картина профиля
    condition { context.affinityVector.genreWeights.size > 5 }

    // Ищем жанр с наименьшим весом
    val blindSpotGenre = context.affinityVector.genreWeights.minByOrNull { it.value }?.key ?: reject()

    // Базовая релевантность средняя, но серендипность максимальная x2.0
    score { calculateRelevance(0.5, serendipityMultiplier = 2.0) }

    segment {
        title = t("blueprint.serendipity_blind_spot.title")
        subtitle = t("blueprint.serendipity_blind_spot.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_genres" to blindSpotGenre,
            "sort_by" to "vote_average.desc",
            "vote_count.gte" to "2000" // Пробиваем блок только железобетонными хитами
        )
    }
}

/**
 * Шаблон "Скрытые сокровища" (SOCIAL_HIDDEN_GEMS)
 *
 * Группа 5: Социальные шаблоны.
 * Психологическое обоснование: Дает зрителю почувствовать себя эстетом.
 * Показываем фильмы, которые получили высокие оценки, но их мало кто видел.
 *
 * Логика TMDB:
 * Берет topGenre.
 * Ищет vote_average.gte = 7.0 (качественные).
 * Ограничивает vote_count.lte = 300 (малоизвестные).
 */
val SocialHiddenGemsBlueprint = blueprint("SOCIAL_HIDDEN_GEMS") {
    condition { context.affinityVector.genreWeights.isNotEmpty() }

    val topGenre = context.affinityVector.genreWeights.maxByOrNull { it.value }?.key ?: reject()
    val genreScore = context.affinityVector.genreWeights[topGenre] ?: 0.0

    condition { genreScore > 0.4 }

    score { calculateRelevance(genreScore, serendipityMultiplier = 1.3) }

    segment {
        title = t("blueprint.social_hidden_gems.title")
        subtitle = t("blueprint.social_hidden_gems.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.EXPLORATION
        params = mapOf(
            "with_genres" to topGenre,
            "vote_average.gte" to "7.0",
            "vote_count.gte" to "50", // Отсекаем совсем любительские проекты без голосов
            "vote_count.lte" to "300", // Ограничиваем популярность
            "sort_by" to "vote_average.desc"
        )
    }
}

/**
 * Шаблон "В тренде" (SOCIAL_TRENDING_NOW)
 *
 * Группа 5: Социальные шаблоны.
 * Психологическое обоснование: "Синдром упущенной выгоды" (FOMO).
 * Людям важно обсуждать с другими популярные новинки.
 *
 * Логика TMDB:
 * Не использует векторы (одинаково для всех).
 * Берет popularity.desc с защитой от накруток (vote_count.gte=100).
 * Релевантность базовая, чтобы не перебивать глубокую персонализацию.
 */
val SocialTrendingNowBlueprint = blueprint("SOCIAL_TRENDING_NOW") {
    // Активируется всегда как базовый социальный якорь
    condition { true }

    // Релевантность небольшая, чтобы персональные подборки выигрывали
    score { calculateRelevance(0.4, serendipityMultiplier = 1.0) }

    segment {
        title = t("blueprint.social_trending_now.title")
        subtitle = t("blueprint.social_trending_now.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "sort_by" to "popularity.desc",
            "vote_count.gte" to "100" // Отсекаем накрутки и случайный шум
        )
    }
}

/**
 * Шаблон "Ожидаемые премьеры" (SOCIAL_UPCOMING_HYPE)
 *
 * Группа 5: Социальные шаблоны.
 * Психологическое обоснование: Прогрев аудитории и создание отложенного спроса.
 *
 * Логика TMDB:
 * Берем фильмы, которые выходят от сегодняшнего дня до +60 дней.
 * Сортируем по popularity.desc, так как vote_average у них еще нет.
 */
val SocialUpcomingHypeBlueprint = blueprint("SOCIAL_UPCOMING_HYPE") {
    condition { true }

    score { calculateRelevance(0.5, serendipityMultiplier = 1.0) }

    val currentDate = Clock.System.now().toString().substringBefore("T")
    val futureDate = Clock.System.now().plus(60.days).toString().substringBefore("T")

    segment {
        title = t("blueprint.social_upcoming_hype.title")
        subtitle = t("blueprint.social_upcoming_hype.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "primary_release_date.gte" to currentDate,
            "primary_release_date.lte" to futureDate,
            "sort_by" to "popularity.desc"
        )
    }
}

/**
 * Шаблон "Выбор критиков" (SOCIAL_CRITICS_CHOICE)
 *
 * Группа 5: Социальные шаблоны.
 * Психологическое обоснование: Опирается на желание приобщиться к "высокому искусству"
 * и авторитетное мнение.
 *
 * Логика TMDB:
 * Берет жанры: Драма (18) или История (36).
 * Высочайший vote_average.gte = 8.0 и vote_count.gte = 5000.
 */
val SocialCriticsChoiceBlueprint = blueprint("SOCIAL_CRITICS_CHOICE") {
    // Активируется для всех
    condition { true }

    // Релевантность умеренная, чтобы не доминировать, но присутствовать
    score { calculateRelevance(0.6, serendipityMultiplier = 1.1) }

    segment {
        title = t("blueprint.social_critics_choice.title")
        subtitle = t("blueprint.social_critics_choice.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to "18|36", // Драма ИЛИ История
            "vote_average.gte" to "8.0",
            "vote_count.gte" to "5000",
            "sort_by" to "vote_average.desc"
        )
    }
}

/**
 * Шаблон "Постыдное удовольствие" (SOCIAL_GUILTY_PLEASURE)
 *
 * Группа 5: Социальные шаблоны.
 * Психологическое обоснование: Иногда люди хотят посмотреть откровенный треш,
 * который стал культовым и растаскан на мемы.
 *
 * Логика TMDB:
 * Берем самый любимый жанр зрителя.
 * Ищем фильмы с vote_average.lte = 5.5, но с огромным vote_count.gte = 2000.
 */
val SocialGuiltyPleasureBlueprint = blueprint("SOCIAL_GUILTY_PLEASURE") {
    condition { context.affinityVector.genreWeights.isNotEmpty() }

    val topGenre = context.affinityVector.genreWeights.maxByOrNull { it.value }?.key ?: reject()

    // Релевантность низкая (0.3), это фановый блок, он не должен доминировать
    score { calculateRelevance(0.3, serendipityMultiplier = 1.0) }

    segment {
        title = t("blueprint.social_guilty_pleasure.title")
        subtitle = t("blueprint.social_guilty_pleasure.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to topGenre, // Привязываем треш к любимому жанру
            "vote_average.lte" to "5.5", // Рейтинг ниже среднего
            "vote_count.gte" to "2000", // Но все это смотрели
            "sort_by" to "popularity.desc" // И это до сих пор популярно (мемы, культ)
        )
    }
}

/**
 * Шаблон "Марафон франшизы" (SOCIAL_FRANCHISE_BINGE)
 *
 * Группа 5: Социальные шаблоны.
 * Психологическое обоснование: Паттерн глубокого погружения в масштабные киновселенные
 * на долгие выходные.
 *
 * Логика TMDB:
 * Берет любимый жанр.
 * Ищет фильмы по тегам sequel (9663) ИЛИ cinematic universe (172551).
 * Сортирует по кассовым сборам (revenue.desc).
 * Использует формат WIDE_BACKDROP для эпичности.
 */
val SocialFranchiseBingeBlueprint = blueprint("SOCIAL_FRANCHISE_BINGE") {
    condition { context.affinityVector.genreWeights.isNotEmpty() }

    val topGenre = context.affinityVector.genreWeights.maxByOrNull { it.value }?.key ?: reject()
    val genreScore = context.affinityVector.genreWeights[topGenre] ?: 0.0

    // Релевантность умеренная, чтобы разбавлять выдачу
    score { calculateRelevance(genreScore, serendipityMultiplier = 1.1) }

    segment {
        title = t("blueprint.social_franchise_binge.title")
        subtitle = t("blueprint.social_franchise_binge.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_BACKDROPS // Эпичным франшизам - эпичные постеры
        params = mapOf(
            "with_genres" to topGenre,
            "with_keywords" to "9663|172551", // sequel ИЛИ cinematic universe
            "sort_by" to "revenue.desc" // Самые кассовые блокбастеры
        )
    }
}

/**
 * Шаблон "Запой по актеру" (EXPLOIT_ACTOR_BINGE)
 *
 * Группа 6: Эксплуатационные шаблоны.
 * Психологическое обоснование: Прямая эксплуатация сильной привязанности зрителя
 * к конкретному лицу на экране. Безотказный прием.
 *
 * Логика TMDB:
 * Берет актера с максимальным весом в векторе.
 * Ищет лучшие фильмы с его участием (vote_average.desc, vote_count.gte=500).
 */
val ExploitActorBingeBlueprint = blueprint("EXPLOIT_ACTOR_BINGE") {
    condition { context.affinityVector.actorWeights.isNotEmpty() }

    val topActor = context.affinityVector.actorWeights.maxByOrNull { it.value }?.key ?: reject()
    val actorScore = context.affinityVector.actorWeights[topActor] ?: 0.0

    // Релевантность крайне высокая, так как это прямое попадание в любимчика
    score { calculateRelevance(actorScore, serendipityMultiplier = 1.3) }

    segment {
        title = t("blueprint.exploit_actor_binge.title")
        subtitle = t("blueprint.exploit_actor_binge.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_cast" to topActor,
            "vote_count.gte" to "500", // Только состоявшиеся фильмы
            "sort_by" to "vote_average.desc"
        )
    }
}

/**
 * Шаблон "Режиссерское погружение" (EXPLOIT_DIRECTOR_DEEP_DIVE)
 *
 * Группа 6: Эксплуатационные шаблоны.
 * Психологическое обоснование: Если актер - это лицо, то режиссер - это стиль и душа.
 * Прямая эксплуатация привязанности к авторскому почерку.
 *
 * Логика TMDB:
 * Берет режиссера с максимальным весом в векторе.
 * Ищет лучшие фильмы с его участием (with_crew).
 */
val ExploitDirectorDeepDiveBlueprint = blueprint("EXPLOIT_DIRECTOR_DEEP_DIVE") {
    condition { context.affinityVector.directorWeights.isNotEmpty() }

    val topDirector = context.affinityVector.directorWeights.maxByOrNull { it.value }?.key ?: reject()
    val directorScore = context.affinityVector.directorWeights[topDirector] ?: 0.0

    // Релевантность высокая, так как это мощный фактор вкуса
    score { calculateRelevance(directorScore, serendipityMultiplier = 1.2) }

    segment {
        title = t("blueprint.exploit_director_deep_dive.title")
        subtitle = t("blueprint.exploit_director_deep_dive.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_crew" to topDirector,
            "vote_count.gte" to "300",
            "sort_by" to "vote_average.desc"
        )
    }
}

/**
 * Шаблон "Потому что вы смотрели" (EXPLOIT_BECAUSE_YOU_WATCHED)
 *
 * Группа 6: Эксплуатационные шаблоны.
 * Психологическое обоснование: Эффект послевкусия (Aftertaste). Желание зрителя
 * получить эмоции, схожие с только что просмотренным тайтлом.
 *
 * Логика:
 * Берем самый свежий ID из recentWatchedIds (кратковременная память вектора).
 * Передаем pseudo-параметр similar_to, который адаптер превратит в запрос к TMDB.
 */
val ExploitBecauseYouWatchedBlueprint = blueprint("EXPLOIT_BECAUSE_YOU_WATCHED") {
    // Активируется только если история просмотров не пуста
    condition { context.affinityVector.recentWatchedIds.isNotEmpty() }

    val lastWatchedId = context.affinityVector.recentWatchedIds.firstOrNull() ?: reject()

    // Релевантность крайне высокая, ловим юзера на "горяченьком" интересе
    score { calculateRelevance(0.85, serendipityMultiplier = 1.0) }

    segment {
        title = t("blueprint.exploit_because_you_watched.title")
        subtitle = t("blueprint.exploit_because_you_watched.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            // Псевдо-параметр. Оркестратор TMDB должен перехватить его 
            // и выполнить запрос /movie/{id}/similar
            "similar_to" to lastWatchedId
        )
    }
}

/**
 * Шаблон "Текущая одержимость" (EXPLOIT_CURRENT_OBSESSION)
 *
 * Группа 6: Эксплуатационные шаблоны.
 * Психологическое обоснование: Реакция на сиюминутный "запой" пользователя.
 *
 * Логика:
 * Берем жанр из sessionBingeVector (вектор кратковременной памяти).
 * Выдаем блок с АБСОЛЮТНОЙ релевантностью, чтобы он всплыл на самый верх.
 */
val ExploitCurrentObsessionBlueprint = blueprint("EXPLOIT_CURRENT_OBSESSION") {
    // HERO-баннер на дашборде активен всегда
    condition { true }
    score { 1.0 }

    val currentDate = Clock.System.now().toString().substringBefore("T")
    val bingeGenre = context.affinityVector.sessionBingeVector.maxByOrNull { it.value }?.key

    segment {
        type = EntityType.MOVIE
        layout = SectionType.HERO

        if (bingeGenre != null) {
            title = t("blueprint.exploit_current_obsession.title")
            subtitle = t("blueprint.exploit_current_obsession.subtitle")
            params = mapOf(
                "with_genres" to bingeGenre,
                "primary_release_date.lte" to currentDate,
                "sort_by" to "vote_average.desc",
                "vote_count.gte" to "500"
            )
        } else {
            title = t("blueprint.exploit_current_obsession.fallback_title")
            subtitle = t("blueprint.exploit_current_obsession.fallback_subtitle")
            params = mapOf(
                "primary_release_date.lte" to currentDate,
                "sort_by" to "popularity.desc",
                "vote_count.gte" to "300"
            )
        }
    }
}

/**
 * Шаблон "Мастера жанра" (EXPLOIT_GENRE_MASTERS)
 *
 * Группа 6: Эксплуатационные шаблоны.
 * Психологическое обоснование: Дать зрителю "золотой фонд" в его самом любимом жанре.
 *
 * Логика:
 * Берем самый любимый жанр (из глобального AffinityVector).
 * Фильтруем по vote_average >= 8.0 и vote_count >= 5000.
 */
val ExploitGenreMastersBlueprint = blueprint("EXPLOIT_GENRE_MASTERS") {
    condition { context.affinityVector.genreWeights.isNotEmpty() }

    val topGenre = context.affinityVector.genreWeights.maxByOrNull { it.value }?.key ?: reject()
    val genreScore = context.affinityVector.genreWeights[topGenre] ?: 0.0

    // Релевантность высокая, так как опираемся на самый сильный базовый интерес
    score { calculateRelevance(genreScore, serendipityMultiplier = 1.1) }

    segment {
        title = t("blueprint.exploit_genre_masters.title")
        subtitle = t("blueprint.exploit_genre_masters.subtitle")
        type = EntityType.MOVIE
        layout = SectionType.CAROUSEL_POSTERS
        params = mapOf(
            "with_genres" to topGenre,
            "vote_average.gte" to "8.0",
            "vote_count.gte" to "5000",
            "sort_by" to "vote_average.desc"
        )
    }
}
