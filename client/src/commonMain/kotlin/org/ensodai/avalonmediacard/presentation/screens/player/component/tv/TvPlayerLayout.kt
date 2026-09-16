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
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.LocalTvDrawerState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvEpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerCenterOverlays
import org.ensodai.avalonmediacard.presentation.screens.player.component.PremiumSeekBar
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.formatTime
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
    var showRatingPopup by remember { mutableStateOf(false) }

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
    val settingsButtonFocusRequester = remember { FocusRequester() }
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

    // При закрытии ТВ-шторки пробуждаем интерфейс плеера, чтобы фокус вернулся на активную кнопку
    LaunchedEffect(tvDrawerState.isOpen) {
        if (!tvDrawerState.isOpen) {
            wakeUpUi()
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
                val settingsTitle = stringResource(Res.string.player_settings_title)
                TvPlayerTopBar(
                    topText = titleData.topText,
                    bottomText = titleData.bottomText,
                    onClose = { actions.onCloseClicked() },
                    onOpenSettings = {
                        wakeUpUi()
                        tvDrawerState.open(
                            screen = PlayerSettingsDrawerScreen(
                                title = settingsTitle,
                                callerFocusRequester = settingsButtonFocusRequester,
                                state = state,
                                controller = controller,
                                actions = actions
                            ),
                            caller = settingsButtonFocusRequester
                        )
                    },
                    hasCustomAudioOrSubtitle = controller.selectedAudioTrack != null || controller.selectedSubtitleTrack != null,
                    currentEpisode = currentEpisode,
                    onToggleEpisodeWatched = currentEpisode?.let { ep -> { actions.onToggleEpisodeWatched(ep) } },
                    onRateEpisode = { showRatingPopup = true },
                    settingsFocusRequester = settingsButtonFocusRequester
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

