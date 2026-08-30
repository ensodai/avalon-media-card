package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMovieDetailResponse(
    val id: Int,
    val title: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    val runtime: Int? = null,
    val genres: List<TmdbGenreResponse> = emptyList(),
    @SerialName("production_companies") val productionCompanies: List<TmdbCompanyResponse> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("origin_country") val originCountry: List<String> = emptyList(),
    val credits: TmdbCreditsResponse? = null,
    val videos: TmdbResultResponse<TmdbVideoResponse>? = null,
    val translations: TmdbTranslationsResponse? = null,
    val images: TmdbImagesResponse? = null
)
