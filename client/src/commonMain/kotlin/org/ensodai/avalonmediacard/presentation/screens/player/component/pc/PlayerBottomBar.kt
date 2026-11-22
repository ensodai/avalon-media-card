package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.designsystem.components.AvalonDropdownMenu
import org.ensodai.avalonmediacard.designsystem.components.AvalonDropdownMenuItem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
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
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.PremiumSeekBar
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerMode
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
    val expectedDur = state.duration
    val ctrlDur = ctrlState.duration
    val duration = when {
        ctrlDur > 0.0 && !ctrlDur.isInfinite() && !ctrlDur.isNaN() && ctrlDur > (currentTime + 1.0) -> {
            if (expectedDur > 0.0 && expectedDur > ctrlDur * 1.5) expectedDur else ctrlDur
        }
        expectedDur > 0.0 -> expectedDur
        ctrlDur > 0.0 && !ctrlDur.isInfinite() && !ctrlDur.isNaN() && !ctrlState.isBuffering && currentTime > 5.0 -> ctrlDur
        else -> 0.0
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

    val colors = AvalonTheme.colors

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
                onSeek = { actions.onSeek(it) }
            )
        } else {
            AvalonText(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                text = "Live / TS",
                color = colors.textSecondary,
                style = AvalonTheme.typography.metadata
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
                    .background(colors.bgOverlay, CircleShape)
                    .border(1.dp, colors.borderOverlay, CircleShape)
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
                                    actions.onPrevEpisodeClicked()
                                }
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AvalonIcon(
                            imageVector = Lucide.SkipBack,
                            contentDescription = stringResource(Res.string.player_controls_prev_episode),
                            tint = if (isPrevEnabled) colors.textPrimary else colors.textMuted,
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
                            onClick = { actions.onPlayPauseClicked() }
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AvalonIcon(
                        imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                        contentDescription = "Play/Pause",
                        tint = colors.textPrimary,
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
                                    actions.onNextEpisodeClicked()
                                }
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AvalonIcon(
                            imageVector = Lucide.SkipForward,
                            contentDescription = stringResource(Res.string.player_controls_next_episode),
                            tint = if (isNextEnabled) colors.textPrimary else colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Timers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvalonText(
                        text = formatTime(currentTime),
                        color = colors.textPrimary,
                        style = AvalonTheme.typography.monoTech
                    )
                    if (duration > 0.0 && (duration > currentTime || !ctrlState.isBuffering)) {
                        AvalonText(
                            text = " / ${formatTime(duration)}",
                            color = colors.textSecondary,
                            style = AvalonTheme.typography.monoTech
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
                    .background(colors.bgOverlay, CircleShape)
                    .border(1.dp, colors.borderOverlay, CircleShape)
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
                        AvalonIcon(
                            imageVector = Lucide.Languages,
                            contentDescription = stringResource(Res.string.player_audio_tracks),
                            tint = if (controller.selectedAudioTrack != null) colors.statusLive else colors.textPrimary,
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
                        AvalonIcon(
                            imageVector = Lucide.Captions,
                            contentDescription = stringResource(Res.string.player_subtitles),
                            tint = if (controller.selectedSubtitleTrack != null || state.selectedSubtitleTrack != null) colors.statusLive else colors.textPrimary,
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
                        val activeQuality = state.selectedQuality ?: state.currentQuality ?: state.qualityVariants.firstOrNull()?.label ?: "HD"

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
                            AvalonText(
                                text = activeQuality,
                                color = colors.statusLive,
                                style = AvalonTheme.typography.labelSmall
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
                                    val isSelected = variant.label == activeQuality
                                    AvalonDropdownMenuItem(
                                        text = variant.label + if (isSelected) "  ✓" else "",
                                        icon = null,
                                        textColor = if (isSelected) colors.statusLive else colors.textPrimary,
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
                val isWatchParty = state.mode == PlayerMode.WATCH_PARTY
                val canShowSettings = !isWatchParty || state.isEffectiveHost

                if (canShowSettings) {
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
                            AvalonIcon(
                                imageVector = Lucide.Settings,
                                contentDescription = stringResource(Res.string.player_settings_title),
                                tint = colors.textPrimary,
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
                                if (isWatchParty) {
                                    AvalonDropdownMenuItem(
                                        text = stringResource(Res.string.watch_party_btn_return_to_lobby),
                                        icon = Lucide.Users,
                                        onClick = {
                                            showSettings = false
                                            actions.onReturnToLobby()
                                        }
                                    )
                                } else {
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
                    AvalonIcon(
                        imageVector = if (isFullscreen) Lucide.Minimize else Lucide.Maximize,
                        contentDescription = stringResource(Res.string.player_fullscreen),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
