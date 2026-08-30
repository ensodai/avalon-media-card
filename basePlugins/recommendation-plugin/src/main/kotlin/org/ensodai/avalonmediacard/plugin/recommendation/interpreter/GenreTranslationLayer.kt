package org.ensodai.avalonmediacard.plugin.recommendation.interpreter

import org.ensodai.avalonmediacard.contract.model.EntityType

/**
 * Двунаправленный слой трансляции жанров между пространствами TMDB Movie (фильмы) и TV (сериалы).
 * Решает проблему с раздельными ID жанров в API TMDB (например, Sci-Fi: Movie 878 vs TV 10765).
 */
object GenreTranslationLayer {
    private val MOVIE_TO_TV_MAP = mapOf(
        "878" to "10765",   // Science Fiction (Movie) -> Sci-Fi & Fantasy (TV)
        "28" to "10759",    // Action (Movie) -> Action & Adventure (TV)
        "12" to "10759",    // Adventure (Movie) -> Action & Adventure (TV)
        "10752" to "10768"  // War (Movie) -> War & Politics (TV)
    )

    private val TV_TO_MOVIE_MAP = mapOf(
        "10765" to "878",   // Sci-Fi & Fantasy (TV) -> Science Fiction (Movie)
        "10759" to "28",    // Action & Adventure (TV) -> Action (Movie)
        "10768" to "10752"  // War & Politics (TV) -> War (Movie)
    )

    fun translateSingle(genreId: String, targetType: EntityType): String {
        return when (targetType) {
            EntityType.MOVIE -> TV_TO_MOVIE_MAP[genreId] ?: genreId
            EntityType.TV -> MOVIE_TO_TV_MAP[genreId] ?: genreId
            else -> genreId
        }
    }

    /**
     * Транслирует строку жанров (разделенную запятыми или пайпами) под нужный targetType.
     * Например: "10765|10759" для MOVIE превратится в "878|28".
     */
    fun translateToTarget(genreStr: String, targetType: EntityType): String {
        if (genreStr.isBlank()) return genreStr

        val delimiter = when {
            genreStr.contains("|") -> "|"
            genreStr.contains(",") -> ","
            else -> null
        }

        if (delimiter == null) {
            return translateSingle(genreStr.trim(), targetType)
        }

        return genreStr.split(delimiter)
            .map { translateSingle(it.trim(), targetType) }
            .distinct()
            .joinToString(delimiter)
    }

    fun translate(genreId: String, from: EntityType, to: EntityType): String {
        if (from == to) return genreId
        return translateToTarget(genreId, to)
    }
}
