package org.ensodai.avalonmediacard.presentation.screens.player.component.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalTvDrawerState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvEpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerCenterOverlays
import org.ensodai.avalonmediacard.presentation.screens.player.component.PremiumSeekBar
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.formatTime
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerEngine
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Состояние отображения полки серий в ТВ-плеере.
 */
enum class TvShelfState {
    COLLAPSED,
    EXPANDED
}

/**
 * Специализированная верстка плеера под ТВ-таргет (D-Pad и выезжающие ТВ-шторки).
 */
private enum class TvDrawerMenu { NONE, MAIN, QUALITY, AUDIO, SUBTITLES, PLAYER }

@Composable
fun TvPlayerLayout(
    state: PlayerViewState,
    actions: PlayerActions,
    controller: PlaybackController,
    videoSurface: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val tvDrawerState = LocalTvDrawerState.current
    var isUiVisible by remember { mutableStateOf(true) }
    var shelfState by remember { mutableStateOf(TvShelfState.COLLAPSED) }
    val isShelfExpanded = shelfState == TvShelfState.EXPANDED
    var currentDrawerMenu by remember { mutableStateOf(TvDrawerMenu.NONE) }
    var showRatingPopup by remember { mutableStateOf(false) }
    
    val currentEngine = state.defaultPlayerEngine

    var shelfHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val shelfHeightDp = with(density) { shelfHeightPx.toDp() }

    val columnOffsetY by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (!state.hasEpisodesContext) {
            0.dp
        } else if (isShelfExpanded) {
            0.dp
        } else {
            if (shelfHeightPx > 0) shelfHeightDp else 240.dp
        },
        label = "columnOffsetY"
    )

    val playPauseFocusRequester = remember { FocusRequester() }
    val mainInputFocusRequester = remember { FocusRequester() }
    var lastInteractionTrigger by remember { mutableLongStateOf(0L) }

    fun wakeUpUi() {
        isUiVisible = true
        lastInteractionTrigger = Clock.System.now().toEpochMilliseconds()
    }

    // Автоматическое скрытие UI через 5 секунд неактивности (если полка серий не раскрыта)
    LaunchedEffect(isUiVisible, controller.state.isPlaying, isShelfExpanded, lastInteractionTrigger) {
        if (isUiVisible && controller.state.isPlaying && !tvDrawerState.isOpen && !isShelfExpanded) {
            delay(5000.milliseconds)
            isUiVisible = false
        }
    }

    // Первичный фокус и переключение фокуса:
    // Когда UI виден -> фокус на Play/Pause кнопке
    // Когда UI скрыт -> фокус на mainInputFocusRequester для перехвата любых кнопок пульта
    LaunchedEffect(isUiVisible) {
        if (isUiVisible) {
            runCatching { playPauseFocusRequester.requestFocus() }
        } else {
            runCatching { mainInputFocusRequester.requestFocus() }
        }
    }

    TvPlayerInputHandler(
        controller = controller,
        isUiVisible = isUiVisible,
        isShelfVisible = isShelfExpanded,
        onWakeUpUi = { wakeUpUi() },
        onHideUi = { isUiVisible = false },
        onToggleShelf = {
            shelfState = if (isShelfExpanded) TvShelfState.COLLAPSED else TvShelfState.EXPANDED
        },
        onCloseShelf = { shelfState = TvShelfState.COLLAPSED },
        onClosePlayer = { actions.onCloseClicked() },
        focusRequester = mainInputFocusRequester,
        modifier = modifier.fillMaxSize().background(Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Видео поверхность
            videoSurface()

            // 2. Оверлеи ошибок, буферизации
            PlayerCenterOverlays(
                controller = controller,
                url = state.currentStreamUrl,
                title = state.title,
                errorOverride = state.errorMessage,
                onTap = {
                    isUiVisible = !isUiVisible
                },
                modifier = Modifier.fillMaxSize()
            )

            // 3. Центральный блок управления (Play/Pause, Перемотка, Переключение серий)
            AnimatedVisibility(
                visible = isUiVisible && !isShelfExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                val playlist = state.playlist
                val currentIndex = playlist.indexOfFirst { it.url == state.currentStreamUrl }
                val prevEp = if (currentIndex > 0) playlist.getOrNull(currentIndex - 1) else null
                val nextEp = if (currentIndex >= 0 && currentIndex < playlist.size - 1) playlist.getOrNull(currentIndex + 1) else null

                TvPlayerCenterControls(
                    controller = controller,
                    hasEpisodesContext = state.hasEpisodesContext,
                    prevEpisode = prevEp,
                    nextEpisode = nextEp,
                    onSelectEpisode = { ep -> actions.onEpisodeSelected(ep) },
                    playPauseFocusRequester = playPauseFocusRequester
                )
            }

            // 4. Верхний ТВ-бар
            AnimatedVisibility(
                visible = isUiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                val titleData = state.displayTitleData
                val currentEpisode = state.currentEpisode
                TvPlayerTopBar(
                    topText = titleData.topText,
                    bottomText = titleData.bottomText,
                    onClose = { actions.onCloseClicked() },
                    onOpenSettings = { currentDrawerMenu = TvDrawerMenu.MAIN },
                    hasCustomAudioOrSubtitle = controller.selectedAudioTrack != null || controller.selectedSubtitleTrack != null,
                    currentEpisode = currentEpisode,
                    onToggleEpisodeWatched = currentEpisode?.let { ep -> { actions.onToggleEpisodeWatched(ep) } },
                    onRateEpisode = { showRatingPopup = true }
                )
            }

            // 5. Нижняя панель управления для ТВ (SeekBar и полка серий)
            AnimatedVisibility(
                visible = isUiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = columnOffsetY)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -12f) {
                                shelfState = TvShelfState.EXPANDED // Свайп вверх открывает серии
                            } else if (dragAmount > 12f) {
                                shelfState = TvShelfState.COLLAPSED // Свайп вниз скрывает серии
                            }
                        }
                    }
            ) {
                // Контроллерная часть (SeekBar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isShelfExpanded) 6.dp else 16.dp)
                ) {
                    val expectedDur = state.duration
                    val duration = if (expectedDur > controller.state.duration) expectedDur else (if (controller.state.duration > 0.0) controller.state.duration else expectedDur)
                    val currentTime = controller.state.currentTime

                    // Фокус-группа 1: Шкала времени (SeekBar) + Время воспроизведения (скрываются при раскрытии полки)
                    AnimatedVisibility(
                        visible = !isShelfExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        if (duration > 0.0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusGroup()
                                ) {
                                    PremiumSeekBar(
                                        currentTime = currentTime,
                                        duration = duration,
                                        bufferTime = currentTime + controller.state.bufferAheadSeconds,
                                        onSeek = { controller.seek(it) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Текущее время / Общая длительность
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatTime(currentTime),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = " / ${formatTime(duration)}",
                                            color = Color.White.copy(alpha = 0.55f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }

                                    // Оставшееся время
                                    val remainingTime = (duration - currentTime).coerceAtLeast(0.0)
                                    Text(
                                        text = "-${formatTime(remainingTime)}",
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Индикатор полки серий
                    if (state.hasEpisodesContext) {
                        TvEpisodeShelfExpander(
                            isExpanded = isShelfExpanded,
                            onClick = {
                                shelfState = if (isShelfExpanded) TvShelfState.COLLAPSED else TvShelfState.EXPANDED
                            }
                        )
                    }
                }

                // Полка серий (находится под контролами в той же колонке)
                if (state.hasEpisodesContext) {
                    TvBottomEpisodeShelf(
                        seasonEpisodes = state.seasonEpisodes,
                        currentStreamId = state.currentStreamId,
                        currentUrl = state.currentStreamUrl,
                        currentEpisode = state.currentEpisode,
                        onEpisodeClick = { ep ->
                            actions.onEpisodeSelected(ep)
                        },
                        upTarget = playPauseFocusRequester,
                        onFocusChanged = { hasFocus ->
                            shelfState = if (hasFocus) TvShelfState.EXPANDED else TvShelfState.COLLAPSED
                        },
                        modifier = Modifier.onSizeChanged { size ->
                            shelfHeightPx = size.height
                        }
                    )
                }
            }
            }

            if (currentDrawerMenu != TvDrawerMenu.NONE) {
                org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect(
                    title = stringResource(Res.string.player_settings_title),
                    icon = Lucide.Settings,
                    onDismiss = { currentDrawerMenu = TvDrawerMenu.NONE }
                ) {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(currentDrawerMenu) { 
                        if (currentDrawerMenu == TvDrawerMenu.MAIN) {
                            runCatching { focusRequester.requestFocus() } 
                        }
                    }

                    val autoQuality = stringResource(Res.string.player_quality_auto)
                    val defaultAudio = stringResource(Res.string.player_audio_default)
                    val subtitlesOff = stringResource(Res.string.player_subtitles_off)

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (state.qualityVariants.isNotEmpty()) {
                            item {
                                AvalonTvDrawerItem(
                                    modifier = Modifier.focusRequester(focusRequester),
                                    title = stringResource(Res.string.player_quality),
                                    subtitle = state.currentQuality ?: state.qualityVariants.firstOrNull()?.label ?: autoQuality,
                                    icon = Lucide.Sparkles,
                                    onClick = { currentDrawerMenu = TvDrawerMenu.QUALITY }
                                )
                            }
                        }
                        item {
                            AvalonTvDrawerItem(
                                modifier = if (state.qualityVariants.isEmpty()) Modifier.focusRequester(focusRequester) else Modifier,
                                title = stringResource(Res.string.player_audio_tracks),
                                subtitle = controller.selectedAudioTrack?.name ?: defaultAudio,
                                icon = Lucide.Languages,
                                onClick = { currentDrawerMenu = TvDrawerMenu.AUDIO }
                            )
                        }
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.player_subtitles),
                                subtitle = controller.selectedSubtitleTrack?.name ?: subtitlesOff,
                                icon = Lucide.Captions,
                                onClick = { currentDrawerMenu = TvDrawerMenu.SUBTITLES }
                            )
                        }
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.player_engine_select),
                                subtitle = when (currentEngine) {
                                    PlayerEngine.MEDIA3 -> stringResource(Res.string.player_engine_media3_title)
                                    PlayerEngine.MPV -> stringResource(Res.string.player_engine_mpv_title)
                                },
                                icon = Lucide.Play,
                                onClick = { currentDrawerMenu = TvDrawerMenu.PLAYER }
                            )
                        }
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.player_btn_select_other_source),
                                icon = Lucide.RefreshCcw,
                                onClick = {
                                    currentDrawerMenu = TvDrawerMenu.NONE
                                    actions.onRequestOtherSource()
                                }
                            )
                        }
                    }
                }

                if (currentDrawerMenu == TvDrawerMenu.QUALITY) {
                    org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect(
                        title = stringResource(Res.string.player_quality),
                        icon = Lucide.Sparkles,
                        onDismiss = { currentDrawerMenu = TvDrawerMenu.MAIN }
                    ) {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

                        val activeQuality = state.currentQuality ?: state.qualityVariants.firstOrNull()?.label ?: "HD"

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                AvalonTvDrawerItem(
                                    modifier = Modifier.focusRequester(focusRequester),
                                    title = stringResource(Res.string.player_btn_back),
                                    icon = Lucide.ArrowLeft,
                                    onClick = { currentDrawerMenu = TvDrawerMenu.MAIN }
                                )
                            }
                            items(state.qualityVariants) { variant ->
                                val isSelected = variant.url == state.currentStreamUrl || variant.label == activeQuality
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
                                            actions.onQualitySelected(variant)
                                        }
                                        currentDrawerMenu = TvDrawerMenu.NONE
                                    }
                                )
                            }
                        }
                    }
                }

                if (currentDrawerMenu == TvDrawerMenu.AUDIO) {
                    org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect(
                        title = stringResource(Res.string.player_audio_select),
                        icon = Lucide.Languages,
                        onDismiss = { currentDrawerMenu = TvDrawerMenu.MAIN }
                    ) {
                        val tracks = if (controller.audioTracks.isNotEmpty()) controller.audioTracks else state.audioTracks
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                AvalonTvDrawerItem(
                                    modifier = Modifier.focusRequester(focusRequester),
                                    title = stringResource(Res.string.player_btn_back),
                                    icon = Lucide.ArrowLeft,
                                    onClick = { currentDrawerMenu = TvDrawerMenu.MAIN }
                                )
                            }
                            if (tracks.isEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(Res.string.player_audio_empty),
                                        color = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(tracks) { track ->
                                    val currentSelected = controller.selectedAudioTrack
                                    val isSelected = if (currentSelected != null) {
                                        track.id == currentSelected.id
                                    } else if (state.selectedAudioTrackIndex != null) {
                                        track.id == state.selectedAudioTrackIndex.toString()
                                    } else {
                                        track.isDefault
                                    }
                                    AvalonTvDrawerItem(
                                        title = track.name,
                                        isSelected = isSelected,
                                        onClick = {
                                            controller.selectAudioTrack(track)
                                            actions.onAudioTrackSelected(track)
                                            currentDrawerMenu = TvDrawerMenu.NONE
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (currentDrawerMenu == TvDrawerMenu.SUBTITLES) {
                    org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect(
                        title = stringResource(Res.string.player_subtitles_select),
                        icon = Lucide.Captions,
                        onDismiss = { currentDrawerMenu = TvDrawerMenu.MAIN }
                    ) {
                        val subs = if (controller.subtitleTracks.isNotEmpty()) controller.subtitleTracks else state.subtitleTracks
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

                        val currentSub = controller.selectedSubtitleTrack ?: state.selectedSubtitleTrack

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                AvalonTvDrawerItem(
                                    modifier = Modifier.focusRequester(focusRequester),
                                    title = stringResource(Res.string.player_btn_back),
                                    icon = Lucide.ArrowLeft,
                                    onClick = { currentDrawerMenu = TvDrawerMenu.MAIN }
                                )
                            }
                            item {
                                AvalonTvDrawerItem(
                                    title = stringResource(Res.string.player_subtitles_off),
                                    isSelected = currentSub == null,
                                    onClick = {
                                        controller.selectSubtitleTrack(null)
                                        actions.onSubtitleTrackSelected(null)
                                        currentDrawerMenu = TvDrawerMenu.NONE
                                    }
                                )
                            }
                            items(subs) { sub ->
                                val isSelected = currentSub?.id == sub.id
                                AvalonTvDrawerItem(
                                    title = sub.name,
                                    isSelected = isSelected,
                                    onClick = {
                                        controller.selectSubtitleTrack(sub)
                                        actions.onSubtitleTrackSelected(sub)
                                        currentDrawerMenu = TvDrawerMenu.NONE
                                    }
                                )
                            }
                        }
                    }
                }

                if (currentDrawerMenu == TvDrawerMenu.PLAYER) {
                    org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect(
                        title = stringResource(Res.string.player_engine_select),
                        icon = Lucide.Play,
                        onDismiss = { currentDrawerMenu = TvDrawerMenu.MAIN }
                    ) {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                AvalonTvDrawerItem(
                                    modifier = Modifier.focusRequester(focusRequester),
                                    title = stringResource(Res.string.player_btn_back),
                                    icon = Lucide.ArrowLeft,
                                    onClick = { currentDrawerMenu = TvDrawerMenu.MAIN }
                                )
                            }
                            item {
                                AvalonTvDrawerItem(
                                    title = stringResource(Res.string.player_engine_media3_title),
                                    subtitle = stringResource(Res.string.player_engine_media3_desc),
                                    isSelected = currentEngine == PlayerEngine.MEDIA3,
                                    onClick = {
                                        actions.onChangeDefaultPlayer(PlayerEngine.MEDIA3)
                                        currentDrawerMenu = TvDrawerMenu.NONE
                                    }
                                )
                            }
                            item {
                                AvalonTvDrawerItem(
                                    title = stringResource(Res.string.player_engine_mpv_title),
                                    subtitle = stringResource(Res.string.player_engine_mpv_desc),
                                    isSelected = currentEngine == PlayerEngine.MPV,
                                    onClick = {
                                        actions.onChangeDefaultPlayer(PlayerEngine.MPV)
                                        currentDrawerMenu = TvDrawerMenu.NONE
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 6. ТВ-попап оценки звездами
            val currentEpisode = state.currentEpisode
            if (showRatingPopup && currentEpisode != null) {
                TvEpisodeRatingPopup(
                    currentRating = currentEpisode.userRating,
                    maxRating = 10,
                    onDismiss = { showRatingPopup = false },
                    onRate = { newRating ->
                        actions.onRateEpisode(currentEpisode, newRating)
                        showRatingPopup = false
                    }
                )
            }
        }
    }
}

