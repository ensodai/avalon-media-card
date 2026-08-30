package org.ensodai.avalonmediacard.core.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.core.PlaybackState

abstract class CommonPlaybackController : PlaybackController {

    override val state: PlaybackState = PlaybackState()

    protected var _audioTracks by mutableStateOf<List<AudioTrack>>(emptyList())
    override val audioTracks: List<AudioTrack> get() = _audioTracks

    protected var _selectedAudioTrack by mutableStateOf<AudioTrack?>(null)
    override val selectedAudioTrack: AudioTrack? get() = _selectedAudioTrack

    protected var _subtitleTracks by mutableStateOf<List<SubtitleTrack>>(emptyList())
    override val subtitleTracks: List<SubtitleTrack> get() = _subtitleTracks

    protected var _selectedSubtitleTrack by mutableStateOf<SubtitleTrack?>(null)
    override val selectedSubtitleTrack: SubtitleTrack? get() = _selectedSubtitleTrack

    private var activeSubtitleCues: List<SubtitleCue> = emptyList()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val httpClient = HttpClient()

    fun setCues(cues: List<SubtitleCue>) {
        activeSubtitleCues = cues
        updateSubtitleForTime(state.currentTime)
    }

    override fun setTracks(audioTracks: List<AudioTrack>, subtitleTracks: List<SubtitleTrack>) {
        _audioTracks = audioTracks
        _subtitleTracks = subtitleTracks

        val currentAudio = _selectedAudioTrack
        if (currentAudio == null || audioTracks.none { it.id == currentAudio.id }) {
            val selectedInList = audioTracks.firstOrNull { it.isDefault } ?: audioTracks.firstOrNull()
            _selectedAudioTrack = selectedInList
        }

        val currentSub = _selectedSubtitleTrack
        if (currentSub != null && subtitleTracks.none { it.id == currentSub.id }) {
            _selectedSubtitleTrack = null
        }
    }

    override fun selectAudioTrack(track: AudioTrack) {
        _selectedAudioTrack = track
    }

    override fun selectSubtitleTrack(track: SubtitleTrack?) {
        _selectedSubtitleTrack = track
        if (track == null) {
            activeSubtitleCues = emptyList()
            state.currentSubtitleText = null
            return
        }

        val url = track.url
        println("[SUBTITLE DEBUG] selectSubtitleTrack: name='${track.name}', url='$url'")
        if (!url.isNullOrBlank()) {
            scope.launch {
                try {
                    val rawText = httpClient.get(url).bodyAsText()
                    println("[SUBTITLE DEBUG] Fetched raw VTT text bytes=${rawText.length}")
                    activeSubtitleCues = UniversalSubtitleParser.parseSubtitle(rawText)
                    println("[SUBTITLE DEBUG] Parsed activeSubtitleCues count=${activeSubtitleCues.size}")
                    updateSubtitleForTime(state.currentTime)
                } catch (e: Throwable) {
                    println("[SUBTITLE DEBUG] Failed to fetch/parse subtitle: ${e.message}")
                    activeSubtitleCues = emptyList()
                    state.currentSubtitleText = null
                }
            }
        }
    }

    fun updateTime(currentTimeSeconds: Double) {
        state.currentTime = currentTimeSeconds
        updateSubtitleForTime(currentTimeSeconds)
    }

    private fun updateSubtitleForTime(currentTimeSeconds: Double) {
        if (_selectedSubtitleTrack != null && activeSubtitleCues.isNotEmpty()) {
            val timeMs = (currentTimeSeconds * 1000).toLong()
            val activeCues = UniversalSubtitleParser.binarySearchActiveCues(activeSubtitleCues, timeMs)
            state.currentSubtitleText = activeCues.firstOrNull()?.text
        } else if (_selectedSubtitleTrack == null) {
            state.currentSubtitleText = null
        }
    }
}
