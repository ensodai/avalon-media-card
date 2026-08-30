package org.ensodai.avalonmediacard.plugins.lampac.domain.model

import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality

/**
 * Domain entity representing an available balancer provider.
 */
data class LampacBalancer(
    val id: String,
    val name: String,
    val endpointUrl: String
)

/**
 * Domain voiceover / studio translation model.
 */
data class LampacVoice(
    val id: String,
    val name: String,
    val isActive: Boolean = false
)

/**
 * Domain season descriptor for series.
 */
data class LampacSeason(
    val seasonNumber: Int,
    val name: String,
    val url: String,
    val voices: List<LampacVoice> = emptyList(),
    val maxQuality: String? = null
)

/**
 * Domain episode descriptor for series.
 */
data class LampacEpisode(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val title: String,
    val url: String,
    val translation: String? = null,
    val qualities: List<VideoQuality> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val voices: List<LampacVoice> = emptyList()
)

/**
 * Canonical source descriptor stored in database for JIT stream resolution.
 */
@Serializable
data class LampacSourceDescriptor(
    val balancer: String,
    val externalId: String? = null,
    val title: String? = null,
    val originalTitle: String? = null,
    val year: Int? = null,
    val tmdbId: Long? = null,
    val imdbId: String? = null,
    val kinopoiskId: Long? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val translationId: String? = null,
    val isSerial: Boolean = false,
    val isTorrent: Boolean = false,
    val magnetUri: String? = null
)

/**
 * Resolved stream information ready for playback.
 */
data class LampacStreamInfo(
    val title: String,
    val translation: String?,
    val streamUrl: String,
    val qualities: List<VideoQuality> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val headers: Map<String, String> = emptyMap()
)
