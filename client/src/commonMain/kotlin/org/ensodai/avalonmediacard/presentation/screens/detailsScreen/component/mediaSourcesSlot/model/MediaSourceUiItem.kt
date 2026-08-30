package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot.model

import androidx.compose.runtime.Immutable
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.slot.Action

/**
 * **Media Source UI Item (Sealed Interface)**
 *
 * Represents strongly-typed UI models for media sources displayed in the MediaSources drawer.
 */
@Immutable
sealed interface MediaSourceUiItem {
    val id: String
    val stream: MediaStream
    val clickAction: Action?
        get() = stream.clickAction
}

/**
 * 1. Torrent source item (Jackett / Prowlarr / TorrServer).
 */
@Immutable
data class TorrentSourceUiItem(
    override val stream: MediaStream,
    val title: String,
    val sizeBytes: Long?,
    val seeders: Int?,
    val leechers: Int?,
    val quality: String?,
    val format: String?,
    val videoCodec: String?,
    val audioCodec: String?,
    val isHdr: Boolean,
    val sourceName: String
) : MediaSourceUiItem {
    override val id: String = stream.id.ifBlank { stream.url.ifBlank { "${title}_${sizeBytes}_${seeders}" } }
}

/**
 * 2. Movie online source item (Rutube, VK, AniLibria, Collaps...).
 */
@Immutable
data class MovieSourceUiItem(
    override val stream: MediaStream,
    val title: String,
    val durationSeconds: Double?,
    val durationFormatted: String?,
    val quality: String,
    val channel: String?
) : MediaSourceUiItem {
    override val id: String = stream.id.ifBlank { stream.url.ifBlank { "${title}_${channel}_${quality}" } }
}

/**
 * 3. Season group source item (Batch of episodes: Rutube, VK...).
 */
@Immutable
data class SeasonGroupSourceUiItem(
    override val stream: MediaStream,
    val title: String,
    val seasonNumber: Int,
    val episodesCount: Int,
    val episodesTotal: Int? = null,
    val quality: String,
    val channel: String?
) : MediaSourceUiItem {
    override val id: String = stream.id.ifBlank { stream.url.ifBlank { "${title}_${seasonNumber}_${episodesCount}" } }
}

/**
 * 4. Single episode source item (Direct episode playback).
 */
@Immutable
data class SingleEpisodeSourceUiItem(
    override val stream: MediaStream,
    val title: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val durationSeconds: Double?,
    val durationFormatted: String?,
    val quality: String,
    val channel: String?
) : MediaSourceUiItem {
    override val id: String = stream.id.ifBlank { stream.url.ifBlank { "${title}_${seasonNumber}_${episodeNumber}_${channel}" } }
}

/**
 * Maps a canonical [MediaStream] into a strongly typed [MediaSourceUiItem].
 */
fun MediaStream.toSourceUiItem(isTvShow: Boolean): MediaSourceUiItem {
    val isTorrent = type == StreamType.Torrent || type == StreamType.Magnet

    if (isTorrent) {
        return TorrentSourceUiItem(
            stream = this,
            title = title.ifBlank { sourceName },
            sizeBytes = sizeBytes,
            seeders = seeders,
            leechers = leechers,
            quality = quality,
            format = format,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
            isHdr = isHdr,
            sourceName = sourceName
        )
    }

    val durationStr = durationSeconds?.let { formatDuration(it.toLong()) }
    val bestQuality = quality ?: "Auto"

    // 1. Сезонный пакет / Группа релиза (seasonNumber != null && episodeNumber == null)
    if (seasonNumber != null && episodeNumber == null) {
        return SeasonGroupSourceUiItem(
            stream = this,
            title = title,
            seasonNumber = seasonNumber!!,
            episodesCount = episodesCount ?: 1,
            episodesTotal = episodesTotal,
            quality = bestQuality,
            channel = episodeName
        )
    }

    // 2. Фильм (!isTvShow)
    if (!isTvShow) {
        return MovieSourceUiItem(
            stream = this,
            title = title.ifBlank { sourceName },
            durationSeconds = durationSeconds,
            durationFormatted = durationStr,
            quality = bestQuality,
            channel = episodeName
        )
    }

    // 3. Конкретная серия (seasonNumber != null && episodeNumber != null)
    return SingleEpisodeSourceUiItem(
        stream = this,
        title = title.ifBlank { sourceName },
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        durationSeconds = durationSeconds,
        durationFormatted = durationStr,
        quality = bestQuality,
        channel = episodeName ?: (if (seasonNumber != null && episodeNumber != null) "S${seasonNumber}E${episodeNumber}" else null)
    )
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
