package org.ensodai.avalonmediacard.plugins.collaps.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality

@Serializable
data class CollapsRootSearch(
    val total: Int? = null,
    val results: List<CollapsSearchResult> = emptyList()
)

@Serializable
data class CollapsSearchResult(
    val id: Int,
    val name: String,
    val type: String? = null,
    @SerialName("origin_name") val originName: String? = null,
    val year: Int? = null,
    val quality: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("kinopoisk_id") val kinopoiskId: String? = null,
    @SerialName("iframe_url") val iframeUrl: String,
    val seasons: List<CollapsSearchSeason>? = null
)

@Serializable
data class CollapsSearchSeason(
    val season: Int,
    @SerialName("iframe_url") val iframeUrl: String? = null,
    val episodes: List<CollapsSearchEpisode>? = null
)

@Serializable
data class CollapsSearchEpisode(
    val episode: Int,
    @SerialName("iframe_url") val iframeUrl: String? = null
)

@Serializable
data class CollapsCcModel(
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class CollapsAudioData(
    val names: List<String> = emptyList()
)

@Serializable
data class CollapsEpisodeData(
    val episode: JsonPrimitive? = null,
    val hls: String? = null,
    val downloadUrl: String? = null,
    val duration: JsonPrimitive? = null,
    val audio: CollapsAudioData? = null,
    val cc: List<CollapsCcModel>? = null
) {
    val episodeNumber: Int
        get() = episode?.content?.toIntOrNull() ?: 1
    val durationSeconds: Long?
        get() = duration?.content?.toLongOrNull()
}

@Serializable
data class CollapsSeasonData(
    val season: JsonPrimitive? = null,
    val episodes: List<CollapsEpisodeData> = emptyList()
) {
    val seasonNumber: Int
        get() = season?.content?.toIntOrNull() ?: 1
}

data class CollapsEmbedParseResult(
    val hlsUrl: String? = null,
    val downloadUrl: String? = null,
    val durationSeconds: Double? = null,
    val audioNames: List<String> = emptyList(),
    val subtitles: List<CollapsCcModel> = emptyList(),
    val seasons: List<CollapsSeasonData>? = null
)

data class CollapsHlsResolved(
    val primaryUrl: String,
    val qualityVariants: List<VideoQuality> = emptyList()
)
