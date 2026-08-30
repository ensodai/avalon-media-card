package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenuItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.PremiumSeekBar
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import org.jetbrains.compose.resources.stringResource

fun formatTime(seconds: Double): String {
    if (seconds.isNaN() || seconds.isInfinite()) return "00:00"
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

@Composable
fun PlayerBottomBar(
    state: PlayerViewState,
    actions: PlayerActions,
    controller: PlaybackController,
    modifier: Modifier = Modifier
) {
    val ctrlState = controller.state
    val currentTime = ctrlState.currentTime
    val duration = if (ctrlState.duration > 0.0 && !ctrlState.duration.isInfinite()) {
        ctrlState.duration
    } else {
        state.duration
    }
    val isPlaying = ctrlState.isPlaying
    val isMuted = ctrlState.isMuted
    val isFullscreen = state.isFullscreen

    val playlist = state.playlist
    val currentIdx = remember(playlist, state.currentStreamId, state.currentStreamUrl) {
        playlist.indexOfFirst {
            (state.currentStreamId.isNotBlank() && it.canonicalId == state.currentStreamId) ||
            (!state.currentStreamUrl.isNullOrBlank() && it.url == state.currentStreamUrl)
        }
    }
    val prevEpisode = remember(playlist, currentIdx) {
        if (currentIdx > 0) playlist.getOrNull(currentIdx - 1) else null
    }
    val nextEpisode = remember(playlist, currentIdx) {
        if (currentIdx in 0 until playlist.size - 1) playlist.getOrNull(currentIdx + 1) else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // 1. Seek Bar (Slider) - Top Row
        if (duration > 0.0) {
            val safeDuration = if (duration > currentTime) duration.toFloat() else (currentTime.toFloat() + 1f)
            val safeCurrentTime = currentTime.toFloat().coerceIn(0f, safeDuration)
            PremiumSeekBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp),
                currentTime = safeCurrentTime.toDouble(),
                duration = safeDuration.toDouble(),
                bufferTime = currentTime + ctrlState.bufferAheadSeconds,
                onSeek = { controller.seek(it) }
            )
        } else {
            Text(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                text = "Live / TS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }

        // 2. Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Previous Episode Button (for series)
                if (state.hasEpisodesContext) {
                    val isPrevEnabled = prevEpisode != null
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = if (isPrevEnabled) 1.15f else 1.0f,
                                shape = CircleShape,
                                activeBorderColor = Color.Transparent,
                                onClick = {
                                    prevEpisode?.let { actions.onEpisodeSelected(it) }
                                }
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.SkipBack,
                            contentDescription = stringResource(Res.string.player_controls_prev_episode),
                            tint = if (isPrevEnabled) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.15f,
                            shape = CircleShape,
                            activeBorderColor = Color.Transparent,
                            onClick = { if (isPlaying) controller.pause() else controller.play() }
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next Episode Button (for series)
                if (state.hasEpisodesContext) {
                    val isNextEnabled = nextEpisode != null
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = if (isNextEnabled) 1.15f else 1.0f,
                                shape = CircleShape,
                                activeBorderColor = Color.Transparent,
                                onClick = {
                                    nextEpisode?.let { actions.onEpisodeSelected(it) }
                                }
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.SkipForward,
                            contentDescription = stringResource(Res.string.player_controls_next_episode),
                            tint = if (isNextEnabled) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Timers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(currentTime),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (duration > 0.0) {
                        Text(
                            text = " / ${formatTime(duration)}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Volume Control (Icon + Slider)
                VolumeControl(
                    volume = ctrlState.volume,
                    isMuted = isMuted,
                    onVolumeChange = { newVol ->
                        if (newVol > 0.0 && isMuted) {
                            controller.setMuted(false)
                        }
                        controller.setVolume(newVol)
                    },
                    onToggleMute = {
                        if (isMuted || ctrlState.volume == 0.0) {
                            if (ctrlState.volume == 0.0) {
                                controller.setVolume(0.5)
                            }
                            controller.setMuted(false)
                        } else {
                            controller.setMuted(true)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Right Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Audio Tracks Selector
                Box(modifier = Modifier.wrapContentSize(Alignment.BottomEnd)) {
                    var showAudioMenu by remember { mutableStateOf(false) }
                    val audioTracks = if (controller.audioTracks.isNotEmpty()) controller.audioTracks else state.audioTracks

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.15f,
                                shape = CircleShape,
                                activeBorderColor = Color.Transparent,
                                onClick = { showAudioMenu = true }
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Languages,
                            contentDescription = stringResource(Res.string.player_audio_tracks),
                            tint = if (controller.selectedAudioTrack != null) Color(0xFF4CAF50) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (showAudioMenu) {
                        val density = LocalDensity.current
                        val selectedTrack = controller.selectedAudioTrack
                        AvalonDropdownMenu(
                            expanded = showAudioMenu,
                            alignment = Alignment.BottomEnd,
                            offset = IntOffset(0, with(density) { -48.dp.roundToPx() }),
                            onDismissRequest = { showAudioMenu = false }
                        ) {
                            if (audioTracks.isEmpty()) {
                                AvalonDropdownMenuItem(
                                    text = stringResource(Res.string.player_audio_loading),
                                    icon = Lucide.Volume2,
                                    onClick = { showAudioMenu = false }
                                )
                            } else {
                                audioTracks.forEach { track ->
                                    val isSelected = if (selectedTrack != null) {
                                        track.id == selectedTrack.id
                                    } else if (state.selectedAudioTrackIndex != null) {
                                        track.id == state.selectedAudioTrackIndex.toString()
                                    } else {
                                        track.isDefault
                                    }
                                    AvalonDropdownMenuItem(
                                        text = track.name + if (isSelected) "  ✓" else "",
                                        icon = Lucide.Volume2,
                                        onClick = {
                                            showAudioMenu = false
                                            if (!isSelected) {
                                                controller.selectAudioTrack(track)
                                                actions.onAudioTrackSelected(track)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Subtitles Selector
                Box(modifier = Modifier.wrapContentSize(Alignment.BottomEnd)) {
                    var showSubMenu by remember { mutableStateOf(false) }
                    val subtitleTracks = if (controller.subtitleTracks.isNotEmpty()) controller.subtitleTracks else state.subtitleTracks

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.15f,
                                shape = CircleShape,
                                activeBorderColor = Color.Transparent,
                                onClick = { showSubMenu = true }
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Captions,
                            contentDescription = stringResource(Res.string.player_subtitles),
                            tint = if (controller.selectedSubtitleTrack != null || state.selectedSubtitleTrack != null) Color(0xFF4CAF50) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (showSubMenu) {
                        val density = LocalDensity.current
                        val selectedSub = controller.selectedSubtitleTrack ?: state.selectedSubtitleTrack
                        val subtitlesOff = stringResource(Res.string.player_subtitles_off)
                        AvalonDropdownMenu(
                            expanded = showSubMenu,
                            alignment = Alignment.BottomEnd,
                            offset = IntOffset(0, with(density) { -48.dp.roundToPx() }),
                            onDismissRequest = { showSubMenu = false }
                        ) {
                            AvalonDropdownMenuItem(
                                text = subtitlesOff + if (selectedSub == null) "  ✓" else "",
                                icon = Lucide.VolumeX,
                                onClick = {
                                    showSubMenu = false
                                    controller.selectSubtitleTrack(null)
                                    actions.onSubtitleTrackSelected(null)
                                }
                            )
                            if (subtitleTracks.isEmpty()) {
                                AvalonDropdownMenuItem(
                                    text = stringResource(Res.string.player_subtitles_searching),
                                    icon = Lucide.Captions,
                                    onClick = { showSubMenu = false }
                                )
                            } else {
                                subtitleTracks.forEach { track ->
                                    val isSelected = selectedSub?.id == track.id
                                    AvalonDropdownMenuItem(
                                        text = track.name + if (isSelected) "  ✓" else "",
                                        icon = Lucide.Captions,
                                        onClick = {
                                            showSubMenu = false
                                            controller.selectSubtitleTrack(track)
                                            actions.onSubtitleTrackSelected(track)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Quality Selector
                if (state.qualityVariants.isNotEmpty()) {
                    Box(modifier = Modifier.wrapContentSize(Alignment.BottomEnd)) {
                        var showQualityMenu by remember { mutableStateOf(false) }
                        val activeQuality = state.currentQuality ?: state.qualityVariants.firstOrNull()?.label ?: "HD"

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .padding(horizontal = 8.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.15f,
                                    shape = CircleShape,
                                    activeBorderColor = Color.Transparent,
                                    onClick = { showQualityMenu = true }
                                )
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeQuality,
                                color = Color(0xFF4CAF50),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showQualityMenu) {
                            val density = LocalDensity.current
                            AvalonDropdownMenu(
                                expanded = showQualityMenu,
                                alignment = Alignment.BottomEnd,
                                width = 160.dp,
                                offset = IntOffset(0, with(density) { -48.dp.roundToPx() }),
                                onDismissRequest = { showQualityMenu = false }
                            ) {
                                state.qualityVariants.forEach { variant ->
                                    val isSelected = variant.url == state.currentStreamUrl || variant.label == activeQuality
                                    AvalonDropdownMenuItem(
                                        text = variant.label + if (isSelected) "  ✓" else "",
                                        icon = null,
                                        textColor = if (isSelected) Color(0xFF4CAF50) else Color.White,
                                        onClick = {
                                            showQualityMenu = false
                                            if (!isSelected) {
                                                actions.onQualitySelected(variant)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Settings (Gear Icon) for Popups
                Box(modifier = Modifier.wrapContentSize(Alignment.BottomEnd)) {
                    var showSettings by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.15f,
                                shape = CircleShape,
                                activeBorderColor = Color.Transparent,
                                onClick = { showSettings = true }
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Settings,
                            contentDescription = stringResource(Res.string.player_settings_title),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (showSettings) {
                        val density = LocalDensity.current
                        AvalonDropdownMenu(
                            expanded = showSettings,
                            alignment = Alignment.BottomEnd,
                            offset = IntOffset(0, with(density) { -48.dp.roundToPx() }),
                            onDismissRequest = { showSettings = false }
                        ) {
                            AvalonDropdownMenuItem(
                                text = stringResource(Res.string.player_btn_select_other_source),
                                icon = Lucide.RefreshCcw,
                                onClick = {
                                    showSettings = false
                                    actions.onRequestOtherSource()
                                }
                            )
                        }
                    }
                }

                // Fullscreen Toggle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.15f,
                            shape = CircleShape,
                            activeBorderColor = Color.Transparent,
                            onClick = { actions.onToggleFullscreen() }
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Lucide.Minimize else Lucide.Maximize,
                        contentDescription = stringResource(Res.string.player_fullscreen),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
