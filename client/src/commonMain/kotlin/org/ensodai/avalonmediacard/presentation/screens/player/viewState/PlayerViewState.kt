package org.ensodai.avalonmediacard.presentation.screens.player.viewState

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewState
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerEngine
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerMode
import kotlin.uuid.Uuid

data class PlayerTitleData(
    val topText: String,
    val bottomText: String
)

data class PlayerViewState(
    val title: String = "",
    val seriesTitle: String? = null,
    val mediaKey: MediaKey? = null,
    val currentStreamId: String = "",
    val currentStreamUrl: String? = null,
    val playlist: List<MediaStream> = emptyList(),
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentTime: Double = 0.0,
    val duration: Double = 0.0,
    val bufferedTime: Double = 0.0,
    val audioTracks: List<AudioTrack> = emptyList(),
    val selectedAudioTrackIndex: Int? = null,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedSubtitleTrack: SubtitleTrack? = null,
    val currentSubtitleText: String = "",
    val isFullscreen: Boolean = false,
    val areControlsVisible: Boolean = true,
    val errorMessage: String? = null,
    val defaultPlayerEngine: PlayerEngine = PlayerEngine.MEDIA3,
    val mode: PlayerMode = PlayerMode.STANDARD,
    val watchRoomId: Uuid? = null,
    val watchRoomParticipants: List<WatchRoomParticipantDto> = emptyList(),
    val isParticipantsPanelVisible: Boolean = true,
    val currentUserId: String? = null,
    val isHost: Boolean = false
) : BaseViewState() {
    val isEffectiveHost: Boolean
        get() = isHost || currentUserId?.let { uid ->
            watchRoomParticipants.find { it.userId.toString() == uid }?.role == WatchRoomParticipantRole.HOST
        } ?: false

    val onlineParticipantsCount: Int
        get() = watchRoomParticipants.count { it.isOnline }
    val isBuffering: Boolean
        get() = status == PlaybackStatus.BUFFERING || status == PlaybackStatus.RECOVERING

    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING

    val currentEpisode: MediaStream?
        get() = playlist.find { currentStreamId.isNotBlank() && it.canonicalId == currentStreamId }
            ?: playlist.find { !currentStreamUrl.isNullOrBlank() && it.url == currentStreamUrl }
            ?: playlist.firstOrNull()

    val currentQuality: String?
        get() = currentEpisode?.qualityVariants?.find {
            it.url == currentStreamUrl || (!currentStreamUrl.isNullOrBlank() && currentStreamUrl.endsWith(it.url))
        }?.label ?: currentEpisode?.quality


    val qualityVariants: List<VideoQuality>
        get() = currentEpisode?.qualityVariants ?: emptyList()

    val seasonEpisodes: Map<Int, List<MediaStream>>
        get() = playlist.groupBy { it.seasonNumber ?: 1 }

    val currentEpisodeIndex: Int
        get() = playlist.indexOfFirst {
            (currentStreamId.isNotBlank() && it.canonicalId == currentStreamId) ||
            (!currentStreamUrl.isNullOrBlank() && it.url == currentStreamUrl)
        }

    val prevEpisode: MediaStream?
        get() = if (currentEpisodeIndex > 0) playlist.getOrNull(currentEpisodeIndex - 1) else null

    val nextEpisode: MediaStream?
        get() = if (currentEpisodeIndex in 0 until playlist.size - 1) playlist.getOrNull(currentEpisodeIndex + 1) else null

    val hasEpisodesContext: Boolean
        get() = (mediaKey?.type == EntityType.TV) && playlist.isNotEmpty() && currentEpisode != null

    val displayTitleData: PlayerTitleData
        get() {
            val isMovie = mediaKey?.type == EntityType.MOVIE
            val ep = currentEpisode
            val season = ep?.seasonNumber
            val episode = ep?.episodeNumber
            val epNameRaw = ep?.episodeName ?: ""
            val cleanEpName = if (episode != null && epNameRaw.startsWith("$episode. ")) {
                epNameRaw.removePrefix("$episode. ").trim()
            } else {
                epNameRaw
            }

            val cleanSeriesTitle = seriesTitle?.takeIf { it.isNotBlank() }
                ?: if (!title.equals(cleanEpName, ignoreCase = true)) title else ""

            val topText = if (isMovie) {
                ""
            } else {
                cleanSeriesTitle
            }

            val bottomText = if (isMovie) {
                if (cleanSeriesTitle.isNotBlank()) cleanSeriesTitle
                else if (cleanEpName.isNotBlank()) cleanEpName
                else title
            } else if (season != null && episode != null) {
                val s = season.toString().padStart(2, '0')
                val e = episode.toString().padStart(2, '0')
                if (cleanEpName.isNotBlank()) "S${s}E${e} • $cleanEpName" else "S${s}E${e}"
            } else if (cleanEpName.isNotBlank()) {
                cleanEpName
            } else {
                title
            }

            return PlayerTitleData(topText, bottomText)
        }

}
