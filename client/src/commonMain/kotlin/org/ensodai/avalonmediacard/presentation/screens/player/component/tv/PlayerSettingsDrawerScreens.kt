package org.ensodai.avalonmediacard.presentation.screens.player.component.tv

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Captions
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RefreshCcw
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Sparkles
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerNavigator
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerScreen
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerEngine
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import org.jetbrains.compose.resources.stringResource

/**
 * Корневой экран настроек плеера в правой ТВ-шторке.
 */
class PlayerSettingsDrawerScreen(
    override val title: String,
    override val callerFocusRequester: FocusRequester? = null,
    private val state: PlayerViewState,
    private val controller: PlaybackController,
    private val actions: PlayerActions
) : TvDrawerScreen {
    override val key: String = "player_settings_main"
    override val icon: ImageVector = Lucide.Settings

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        val autoQuality = stringResource(Res.string.player_quality_auto)
        val defaultAudio = stringResource(Res.string.player_audio_default)
        val subtitlesOff = stringResource(Res.string.player_subtitles_off)
        val currentEngine = state.defaultPlayerEngine

        val qualityTitle = stringResource(Res.string.player_quality)
        val audioTitle = stringResource(Res.string.player_audio_select)
        val subsTitle = stringResource(Res.string.player_subtitles_select)
        val engineTitle = stringResource(Res.string.player_engine_select)

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (state.qualityVariants.isNotEmpty()) {
                item(key = "setting_quality") {
                    AvalonTvDrawerItem(
                        title = qualityTitle,
                        subtitle = state.currentQuality ?: state.qualityVariants.firstOrNull()?.label ?: autoQuality,
                        icon = Lucide.Sparkles,
                        onClick = {
                            navigator.push(
                                PlayerQualityDrawerScreen(
                                    title = qualityTitle,
                                    variants = state.qualityVariants,
                                    currentQuality = state.currentQuality,
                                    currentStreamUrl = state.currentStreamUrl,
                                    onSelectQuality = { variant ->
                                        actions.onQualitySelected(variant)
                                    }
                                )
                            )
                        }
                    )
                }
            }

            item(key = "setting_audio") {
                val currentAudioTrack = controller.selectedAudioTrack
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_audio_tracks),
                    subtitle = currentAudioTrack?.name ?: defaultAudio,
                    icon = Lucide.Languages,
                    onClick = {
                        val tracks = if (controller.audioTracks.isNotEmpty()) controller.audioTracks else state.audioTracks
                        navigator.push(
                            PlayerAudioDrawerScreen(
                                title = audioTitle,
                                tracks = tracks,
                                selectedTrack = currentAudioTrack,
                                selectedTrackIndex = state.selectedAudioTrackIndex,
                                onSelectTrack = { track ->
                                    controller.selectAudioTrack(track)
                                    actions.onAudioTrackSelected(track)
                                }
                            )
                        )
                    }
                )
            }

            item(key = "setting_subtitles") {
                val currentSub = controller.selectedSubtitleTrack ?: state.selectedSubtitleTrack
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_subtitles),
                    subtitle = currentSub?.name ?: subtitlesOff,
                    icon = Lucide.Captions,
                    onClick = {
                        val subs = if (controller.subtitleTracks.isNotEmpty()) controller.subtitleTracks else state.subtitleTracks
                        navigator.push(
                            PlayerSubtitlesDrawerScreen(
                                title = subsTitle,
                                tracks = subs,
                                selectedTrack = currentSub,
                                onSelectTrack = { sub ->
                                    controller.selectSubtitleTrack(sub)
                                    actions.onSubtitleTrackSelected(sub)
                                }
                            )
                        )
                    }
                )
            }

            item(key = "setting_engine") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_engine_select),
                    subtitle = when (currentEngine) {
                        PlayerEngine.MEDIA3 -> stringResource(Res.string.player_engine_media3_title)
                        PlayerEngine.MPV -> stringResource(Res.string.player_engine_mpv_title)
                    },
                    icon = Lucide.Play,
                    onClick = {
                        navigator.push(
                            PlayerEngineDrawerScreen(
                                title = engineTitle,
                                currentEngine = currentEngine,
                                onSelectEngine = { engine ->
                                    actions.onChangeDefaultPlayer(engine)
                                }
                            )
                        )
                    }
                )
            }

            item(key = "setting_other_source") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_btn_select_other_source),
                    icon = Lucide.RefreshCcw,
                    onClick = {
                        navigator.clear()
                        actions.onRequestOtherSource()
                    }
                )
            }
        }
    }
}

/**
 * Экран выбора качества видеопотока.
 */
class PlayerQualityDrawerScreen(
    override val title: String,
    private val variants: List<VideoQuality>,
    private val currentQuality: String?,
    private val currentStreamUrl: String?,
    private val onSelectQuality: (VideoQuality) -> Unit
) : TvDrawerScreen {
    override val key: String = "player_settings_quality"
    override val icon: ImageVector = Lucide.Sparkles

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        val activeQuality = currentQuality ?: variants.firstOrNull()?.label ?: "HD"

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "quality_back_button") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_btn_back),
                    icon = Lucide.ArrowLeft,
                    onClick = { navigator.pop() }
                )
            }

            items(
                items = variants,
                key = { it.url }
            ) { variant ->
                val isSelected = variant.url == currentStreamUrl || variant.label == activeQuality
                val desc = when (variant.label.lowercase()) {
                    "1080p", "fhd" -> "Full High Definition"
                    "720p", "hd" -> "High Definition"
                    "480p", "sd" -> "Standard Definition"
                    "4k", "2160p" -> "Ultra High Definition"
                    else -> null
                }
                AvalonTvDrawerItem(
                    title = variant.label,
                    subtitle = desc,
                    isSelected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            onSelectQuality(variant)
                        }
                        navigator.clear()
                    }
                )
            }
        }
    }
}

/**
 * Экран выбора аудиодорожки.
 */
class PlayerAudioDrawerScreen(
    override val title: String,
    private val tracks: List<AudioTrack>,
    private val selectedTrack: AudioTrack?,
    private val selectedTrackIndex: Int?,
    private val onSelectTrack: (AudioTrack) -> Unit
) : TvDrawerScreen {
    override val key: String = "player_settings_audio"
    override val icon: ImageVector = Lucide.Languages

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "audio_back_button") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_btn_back),
                    icon = Lucide.ArrowLeft,
                    onClick = { navigator.pop() }
                )
            }

            if (tracks.isEmpty()) {
                item(key = "audio_empty_state") {
                    Text(
                        text = stringResource(Res.string.player_audio_empty),
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(
                    items = tracks,
                    key = { it.id }
                ) { track ->
                    val isSelected = if (selectedTrack != null) {
                        track.id == selectedTrack.id
                    } else if (selectedTrackIndex != null) {
                        track.id == selectedTrackIndex.toString()
                    } else {
                        track.isDefault
                    }
                    AvalonTvDrawerItem(
                        title = track.name,
                        isSelected = isSelected,
                        onClick = {
                            onSelectTrack(track)
                            navigator.clear()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Экран выбора субтитров.
 */
class PlayerSubtitlesDrawerScreen(
    override val title: String,
    private val tracks: List<SubtitleTrack>,
    private val selectedTrack: SubtitleTrack?,
    private val onSelectTrack: (SubtitleTrack?) -> Unit
) : TvDrawerScreen {
    override val key: String = "player_settings_subtitles"
    override val icon: ImageVector = Lucide.Captions

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "subs_back_button") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_btn_back),
                    icon = Lucide.ArrowLeft,
                    onClick = { navigator.pop() }
                )
            }

            item(key = "subs_off_option") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_subtitles_off),
                    isSelected = selectedTrack == null,
                    onClick = {
                        onSelectTrack(null)
                        navigator.clear()
                    }
                )
            }

            items(
                items = tracks,
                key = { it.id }
            ) { sub ->
                val isSelected = selectedTrack?.id == sub.id
                AvalonTvDrawerItem(
                    title = sub.name,
                    isSelected = isSelected,
                    onClick = {
                        onSelectTrack(sub)
                        navigator.clear()
                    }
                )
            }
        }
    }
}

/**
 * Экран выбора движка воспроизведения.
 */
class PlayerEngineDrawerScreen(
    override val title: String,
    private val currentEngine: PlayerEngine,
    private val onSelectEngine: (PlayerEngine) -> Unit
) : TvDrawerScreen {
    override val key: String = "player_settings_engine"
    override val icon: ImageVector = Lucide.Play

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "engine_back_button") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_btn_back),
                    icon = Lucide.ArrowLeft,
                    onClick = { navigator.pop() }
                )
            }

            item(key = "engine_media3") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_engine_media3_title),
                    subtitle = stringResource(Res.string.player_engine_media3_desc),
                    isSelected = currentEngine == PlayerEngine.MEDIA3,
                    onClick = {
                        onSelectEngine(PlayerEngine.MEDIA3)
                        navigator.clear()
                    }
                )
            }

            item(key = "engine_mpv") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_engine_mpv_title),
                    subtitle = stringResource(Res.string.player_engine_mpv_desc),
                    isSelected = currentEngine == PlayerEngine.MPV,
                    onClick = {
                        onSelectEngine(PlayerEngine.MPV)
                        navigator.clear()
                    }
                )
            }
        }
    }
}
