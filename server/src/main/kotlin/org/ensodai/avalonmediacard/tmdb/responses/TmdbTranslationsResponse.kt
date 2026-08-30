package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbTranslationsResponse(
    val translations: List<TmdbTranslationItem> = emptyList()
)

@Serializable
data class TmdbTranslationItem(
    @SerialName("iso_3166_1") val iso3166: String? = null,
    @SerialName("iso_639_1") val iso639: String? = null,
    val name: String? = null,
    @SerialName("english_name") val englishName: String? = null,
    val data: TmdbTranslationData? = null
)

@Serializable
data class TmdbTranslationData(
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null
)
