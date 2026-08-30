package org.ensodai.avalonmediacard.tmdb.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbShowDetailResponse(
    val id: Int,
    val name: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    val genres: List<TmdbGenreResponse> = emptyList(),
    @SerialName("production_companies") val productionCompanies: List<TmdbCompanyResponse> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("origin_country") val originCountry: List<String> = emptyList(),
    val credits: TmdbCreditsResponse? = null,
    val videos: TmdbResultResponse<TmdbVideoResponse>? = null,
    val translations: TmdbTranslationsResponse? = null,
    val images: TmdbImagesResponse? = null,

    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    val status: String? = null,
    val networks: List<TmdbNetworkResponse> = emptyList(),
    val seasons: List<TmdbSeasonResponse> = emptyList(),
    @SerialName("external_ids") val externalIds: TmdbExternalIdsResponse? = null
)

@Serializable
data class TmdbExternalIdsResponse(
    @SerialName("imdb_id") val imdbId: String? = null
)

@Serializable
data class TmdbNetworkResponse(
    val id: Int = 0,
    val name: String = ""
)

@Serializable
data class TmdbSeasonResponse(
    val id: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("air_date") val airDate: String? = null
)
