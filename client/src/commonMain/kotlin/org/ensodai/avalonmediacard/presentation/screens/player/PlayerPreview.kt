package org.ensodai.avalonmediacard.presentation.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.core.PlaybackState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.DeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalTvDrawerState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerState
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.UnifiedVideoPlayer
import org.ensodai.avalonmediacard.presentation.screens.player.component.tv.TvPlayerLayout
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerEngine
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import androidx.compose.ui.tooling.preview.Preview

/**
 * Мок-контроллер для изолированного тестирования и дизайна плеера в Compose Preview.
 */
class MockPlaybackController(
    initialTime: Double = 1420.0,
    initialDuration: Double = 5400.0,
    isPlaying: Boolean = true,
    isBuffering: Boolean = false,
    override val audioTracks: List<AudioTrack> = listOf(
        AudioTrack(id = "1", name = "Русский (Дубляж RHS 5.1)", isDefault = true),
        AudioTrack(id = "2", name = "Русский (HDRezka Studio)", isDefault = false),
        AudioTrack(id = "3", name = "English (Original Dolby Atmos)", isDefault = false)
    ),
    override val subtitleTracks: List<SubtitleTrack> = listOf(
        SubtitleTrack(id = "1", name = "Русские (Полные)", language = "ru"),
        SubtitleTrack(id = "2", name = "Русские (Форсированные)", language = "ru"),
        SubtitleTrack(id = "3", name = "English (SDH)", language = "en")
    )
) : PlaybackController {
    override val state = PlaybackState(
        currentTime = initialTime,
        duration = initialDuration,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        bufferAheadSeconds = 120.0,
        volume = 1.0,
        fps = 60.0
    )

    private var _selectedAudioTrack by mutableStateOf<AudioTrack?>(audioTracks.firstOrNull())
    override val selectedAudioTrack: AudioTrack? get() = _selectedAudioTrack

    private var _selectedSubtitleTrack by mutableStateOf<SubtitleTrack?>(subtitleTracks.firstOrNull())
    override val selectedSubtitleTrack: SubtitleTrack? get() = _selectedSubtitleTrack

    override fun play() { state.isPlaying = true }
    override fun pause() { state.isPlaying = false }
    override fun togglePlayPause() { state.isPlaying = !state.isPlaying }
    override fun seek(time: Double) { state.currentTime = time.coerceIn(0.0, state.duration) }
    override fun setMuted(muted: Boolean) { state.isMuted = muted }
    override fun setVolume(volume: Double) { state.volume = volume }
    override fun selectAudioTrack(track: AudioTrack) { _selectedAudioTrack = track }
    override fun selectSubtitleTrack(track: SubtitleTrack?) { _selectedSubtitleTrack = track }
}

/**
 * Заглушка видеоповерхности с красивым фоном для реалистичного превью.
 */
@Composable
fun MockVideoSurface(
    title: String = "Avalon Cinema Preview",
    subtitle: String = "4K HDR • Dolby Vision • 60 FPS",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Lucide.Clapperboard,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Создает моковые данные для фильма.
 */
fun createMockMoviePlayerState(): PlayerViewState {
    val movieStream = MediaStream(
        title = "Дюна: Часть вторая (2024)",
        url = "https://mock.stream/dune2.m3u8",
        type = StreamType.Hls,
        sourceName = "MockSource",
        durationSeconds = 9960.0,
        watchedProgressSeconds = 2840L,
        isWatched = false,
        userRating = 10
    )
    return PlayerViewState(
        title = "Дюна: Часть вторая (2024)",
        seriesTitle = null,
        mediaKey = MediaKey(
            provider = MediaProvider.Tmdb,
            type = EntityType.MOVIE,
            id = "movie_dune_2"
        ),
        currentStreamUrl = movieStream.url,
        playlist = listOf(movieStream),
        status = PlaybackStatus.PLAYING,
        currentTime = 2840.0,
        duration = 9960.0,
        bufferedTime = 3200.0,
        isFullscreen = true,
        areControlsVisible = true,
        defaultPlayerEngine = PlayerEngine.MEDIA3
    )
}

/**
 * Создает моковые данные для сериала со списком сезонов и серий.
 */
fun createMockSeriesPlayerState(): PlayerViewState {
    val episodes = listOf(
        MediaStream(
            title = "Пилотная серия",
            url = "https://mock.stream/s01e01.m3u8",
            type = StreamType.Hls,
            sourceName = "MockSource",
            episodeNumber = 1,
            seasonNumber = 1,
            episodeName = "Пилот",
            durationSeconds = 3480.0,
            watchedProgressSeconds = 3480L,
            isWatched = true,
            userRating = 9
        ),
        MediaStream(
            title = "Кот в мешке",
            url = "https://mock.stream/s01e02.m3u8",
            type = StreamType.Hls,
            sourceName = "MockSource",
            episodeNumber = 2,
            seasonNumber = 1,
            episodeName = "Кот в мешке",
            durationSeconds = 2880.0,
            watchedProgressSeconds = 1420L,
            isWatched = false,
            userRating = 9
        ),
        MediaStream(
            title = "И птичка улетела...",
            url = "https://mock.stream/s01e03.m3u8",
            type = StreamType.Hls,
            sourceName = "MockSource",
            episodeNumber = 3,
            seasonNumber = 1,
            episodeName = "И птичка улетела...",
            durationSeconds = 3120.0,
            watchedProgressSeconds = 0L,
            isWatched = false,
            userRating = 9
        ),
        MediaStream(
            title = "Семь тридцать семь",
            url = "https://mock.stream/s02e01.m3u8",
            type = StreamType.Hls,
            sourceName = "MockSource",
            episodeNumber = 1,
            seasonNumber = 2,
            episodeName = "Семь тридцать семь",
            durationSeconds = 2940.0,
            watchedProgressSeconds = 0L,
            isWatched = false,
            userRating = 9
        )
    )

    return PlayerViewState(
        title = "Во все тяжкие",
        seriesTitle = "Во все тяжкие",
        mediaKey = MediaKey(
            provider = MediaProvider.Tmdb,
            type = EntityType.TV,
            id = "tv_breaking_bad"
        ),
        currentStreamUrl = episodes[1].url,
        playlist = episodes,
        status = PlaybackStatus.PLAYING,
        currentTime = 1420.0,
        duration = 2880.0,
        bufferedTime = 1600.0,
        isFullscreen = true,
        areControlsVisible = true,
        defaultPlayerEngine = PlayerEngine.MEDIA3
    )
}

/**
 * Создает моковые действия плеера для превью.
 */
fun createMockPlayerActions(): PlayerActions {
    return PlayerActions(
        onPlayPauseClicked = {},
        onSeek = {},
        onEpisodeSelected = {},
        onAudioTrackSelected = {},
        onSubtitleTrackSelected = {},
        onToggleFullscreen = {},
        onFullscreenChanged = {},
        onControlsVisibilityChanged = {},
        onProgressUpdate = { _, _ -> },
        onPlaybackStateChanged = {},
        onError = {},
        onStreamRecovery = {},
        onCloseClicked = {},
        onToggleEpisodeWatched = {},
        onRateEpisode = { _, _ -> },
        onChangeDefaultPlayer = {},
        onRequestOtherSource = {}
    )
}

/**
 * Базовая обертка темы и провайдеров для превью плеера.
 */
@Composable
fun PlayerPreviewTheme(
    deviceTarget: DeviceTarget = DeviceTarget.ANDROID_TV,
    content: @Composable () -> Unit
) {
    val customColorScheme = darkColorScheme(
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0A),
        surfaceVariant = Color(0xFF141414),
        primary = Color.White,
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF1F1F1F),
        onPrimaryContainer = Color.White,
        secondary = Color.White,
        secondaryContainer = Color(0xFF141414),
        onSecondaryContainer = Color(0xFFA1A1AA),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFA1A1AA),
        outline = Color(0xFF27272A),
        outlineVariant = Color(0xFF3F3F46)
    )

    val drawerState = remember { TvDrawerState() }

    MaterialTheme(colorScheme = customColorScheme) {
        CompositionLocalProvider(
            LocalDeviceTarget provides deviceTarget,
            LocalTvDrawerState provides drawerState
        ) {
            content()
        }
    }
}

// =========================================================================
// PREVIEWS
// =========================================================================

/**
 * Превью мобильного плеера в портретной ориентации (Смартфон: Фильм).
 */
@Preview(name = "Mobile Portrait - Movie", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun MobilePortraitMoviePreview() {
    val state = remember { createMockMoviePlayerState() }
    val controller = remember { MockPlaybackController(initialTime = 2840.0, initialDuration = 9960.0) }
    val actions = remember { createMockPlayerActions() }

    PlayerPreviewTheme(deviceTarget = DeviceTarget.ANDROID_MOBILE) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            UnifiedVideoPlayer(
                state = state,
                actions = actions,
                controller = controller,
                videoSurface = {
                    MockVideoSurface(
                        title = state.title,
                        subtitle = "1080p Mobile Stream"
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Превью мобильного плеера в портретной ориентации (Смартфон: Сериал с полкой серий).
 */
@Preview(name = "Mobile Portrait - Series", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun MobilePortraitSeriesPreview() {
    val state = remember { createMockSeriesPlayerState() }
    val controller = remember { MockPlaybackController(initialTime = 1420.0, initialDuration = 2880.0) }
    val actions = remember { createMockPlayerActions() }

    PlayerPreviewTheme(deviceTarget = DeviceTarget.ANDROID_MOBILE) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            UnifiedVideoPlayer(
                state = state,
                actions = actions,
                controller = controller,
                videoSurface = {
                    MockVideoSurface(
                        title = state.displayTitleData.topText,
                        subtitle = state.displayTitleData.bottomText
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Превью ТВ-плеера в ландшафтном режиме (1280x720 / 16:9).
 */
@Preview(name = "TV Landscape - Movie", widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
fun TvPlayerLandscapeMoviePreview() {
    val state = remember { createMockMoviePlayerState() }
    val controller = remember { MockPlaybackController(initialTime = 2840.0, initialDuration = 9960.0) }
    val actions = remember { createMockPlayerActions() }

    PlayerPreviewTheme(deviceTarget = DeviceTarget.ANDROID_TV) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            TvPlayerLayout(
                state = state,
                actions = actions,
                controller = controller,
                videoSurface = {
                    MockVideoSurface(
                        title = state.title,
                        subtitle = "4K IMAX Enhanced • Dolby Atmos"
                    )
                }
            )
        }
    }
}

/**
 * Превью ТВ-плеера в ландшафтном режиме (Сериал с полкой серий).
 */
@Preview(name = "TV Landscape - Series (Shelf)", widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
fun TvPlayerLandscapeSeriesPreview() {
    val state = remember { createMockSeriesPlayerState() }
    val controller = remember { MockPlaybackController(initialTime = 1420.0, initialDuration = 2880.0) }
    val actions = remember { createMockPlayerActions() }

    PlayerPreviewTheme(deviceTarget = DeviceTarget.ANDROID_TV) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            TvPlayerLayout(
                state = state,
                actions = actions,
                controller = controller,
                videoSurface = {
                    MockVideoSurface(
                        title = state.displayTitleData.topText,
                        subtitle = state.displayTitleData.bottomText
                    )
                }
            )
        }
    }
}

/**
 * Превью единого плеера для Desktop / Web (1440x900).
 */
@Preview(name = "Desktop/Web Landscape", widthDp = 1440, heightDp = 900, showBackground = true)
@Composable
fun DesktopPlayerPreview() {
    val state = remember { createMockSeriesPlayerState() }
    val controller = remember { MockPlaybackController(initialTime = 1420.0, initialDuration = 2880.0) }
    val actions = remember { createMockPlayerActions() }

    PlayerPreviewTheme(deviceTarget = DeviceTarget.DESKTOP_WEB) {
        UnifiedVideoPlayer(
            state = state,
            actions = actions,
            controller = controller,
            videoSurface = {
                MockVideoSurface(
                    title = state.displayTitleData.topText,
                    subtitle = state.displayTitleData.bottomText
                )
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
