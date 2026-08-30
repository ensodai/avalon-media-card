package org.ensodai.avalonmediacard.tmdb

import org.ensodai.avalonmediacard.tmdb.responses.*

sealed interface TmdbDetails {
    val id: Int
    val title: String?
    val subtitle: String?
    val englishTitle: String?
    val genres: List<TmdbGenreResponse>
    val posterPath: String?
    val backdropPath: String?
    val voteAverage: Float?
    val overview: String?
    val tagline: String?
    val releaseDate: String?
    val credits: TmdbCreditsResponse?
    val videos: TmdbResultResponse<TmdbVideoResponse>?
    val translations: TmdbTranslationsResponse?
    val images: TmdbImagesResponse?
    val originalLanguage: String?
    val originCountry: List<String>

    data class Movie(val detail: TmdbMovieDetailResponse) : TmdbDetails {
        override val id: Int get() = detail.id
        override val title: String? get() = detail.title
        override val subtitle: String? get() = detail.originalTitle
        override val englishTitle: String? get() = detail.translations?.translations?.find { it.iso639 == "en" }?.data?.title
        override val genres: List<TmdbGenreResponse> get() = detail.genres
        override val posterPath: String? get() = detail.posterPath
        override val backdropPath: String? get() = detail.backdropPath
        override val voteAverage: Float get() = detail.voteAverage
        override val overview: String? get() = detail.overview
        override val tagline: String? get() = detail.tagline
        override val releaseDate: String? get() = detail.releaseDate
        override val credits: TmdbCreditsResponse? get() = detail.credits
        override val videos: TmdbResultResponse<TmdbVideoResponse>? get() = detail.videos
        override val translations: TmdbTranslationsResponse? get() = detail.translations
        override val images: TmdbImagesResponse? get() = detail.images
        override val originalLanguage: String? get() = detail.originalLanguage
        override val originCountry: List<String> get() = detail.originCountry
    }

    data class Show(val detail: TmdbShowDetailResponse) : TmdbDetails {
        override val id: Int get() = detail.id
        override val title: String? get() = detail.name
        override val subtitle: String? get() = detail.originalName
        override val englishTitle: String? get() = detail.translations?.translations?.find { it.iso639 == "en" }?.data?.name
        override val genres: List<TmdbGenreResponse> get() = detail.genres
        override val posterPath: String? get() = detail.posterPath
        override val backdropPath: String? get() = detail.backdropPath
        override val voteAverage: Float get() = detail.voteAverage
        override val overview: String? get() = detail.overview
        override val tagline: String? get() = detail.tagline
        override val releaseDate: String? get() = detail.firstAirDate
        override val credits: TmdbCreditsResponse? get() = detail.credits
        override val videos: TmdbResultResponse<TmdbVideoResponse>? get() = detail.videos
        override val translations: TmdbTranslationsResponse? get() = detail.translations
        override val images: TmdbImagesResponse? get() = detail.images
        override val originalLanguage: String? get() = detail.originalLanguage
        override val originCountry: List<String> get() = detail.originCountry
    }
}
