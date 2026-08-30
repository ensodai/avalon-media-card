package org.ensodai.avalonmediacard.plugins.mediadetails.domain

import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto

data class DetailsWidgetState(
    val movies: List<TmdbMovieDto> = emptyList(),
    val page: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class MediaDetailsState(
    val metadata: MediaMetadata? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedSeasonNumber: Int = 1,
    val seasonContents: Map<Int, org.ensodai.avalonmediacard.contract.slot.SeasonContent> = emptyMap()
)
