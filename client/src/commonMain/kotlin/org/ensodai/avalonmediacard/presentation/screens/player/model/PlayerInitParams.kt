package org.ensodai.avalonmediacard.presentation.screens.player.model

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack

data class PlayerInitParams(
    val title: String,
    val seriesTitle: String? = null,
    val mediaKey: MediaKey,
    val streamUrl: String? = null,
    val streamId: String? = null,
    val targetSeason: Int? = null,
    val targetEpisode: Int? = null,
    val durationSeconds: Double? = null,
    val startPositionSeconds: Long? = null,
    val playlist: List<MediaStream> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val audioTrackIndex: Int? = null
)
