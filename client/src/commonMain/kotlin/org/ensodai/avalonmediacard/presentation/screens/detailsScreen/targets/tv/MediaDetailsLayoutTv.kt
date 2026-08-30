package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.backdropImageSlot.BackdropImageSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.carouselsSlot.CarouselsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.castSlot.CastSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot.CollectionButtonsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.commentsSlot.CommentsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.continueWatchingSlot.ContinueWatchingSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.criticsRatingsSlot.CriticsRatingsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaDescriptionSlot.MediaDescriptionSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot.MediaSourcesSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.metadataSlot.MetadataSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playButtonsSlot.PlayButtonsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playerSlot.PlayerSectionSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.statusAndRatingSlot.StatusAndRatingSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.syncStatusSlot.SyncStatusSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.titleSlot.TitleSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components.TvTvSeasonsSection
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.viewState.DetailsViewState

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun MediaDetailsLayoutTv(
    state: DetailsViewState,
    onAction: (Action) -> Unit,
    onClosePlayer: (() -> Unit)? = null,
    onRequestOtherSource: (() -> Unit)? = null,
    onCloseSources: (() -> Unit)? = null,
    onSelectSource: ((providerId: String, sourceId: String, seasonNumber: Int?, episodeNumber: Int?, onComplete: () -> Unit) -> Unit)? = null,
    onRefreshSources: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isPlayerOpen = state.playerState !is DetailsViewState.PlayerState.Idle
    val scrollState = rememberScrollState()
    val backgroundColor = MaterialTheme.colorScheme.background
    val buttonsFocusRequester = remember { FocusRequester() }
    val focusZone = remember { mutableStateOf(TvFocusZone.HEADER) }

    LaunchedEffect(isPlayerOpen) {
        if (!isPlayerOpen) {
            runCatching { buttonsFocusRequester.requestFocus() }
        }
    }

    val dynamicSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float, size: Float, containerSize: Float
            ): Float {
                return when (focusZone.value) {
                    TvFocusZone.HEADER -> {
                        val targetPivot = containerSize * 0.90f
                        val elementCenter = offset + (size / 2f)
                        elementCenter - targetPivot
                    }
                    TvFocusZone.SEASONS_HEADER -> {
                        val targetPivot = containerSize * 0.08f
                        val elementCenter = offset + (size / 2f)
                        elementCenter - targetPivot
                    }
                    TvFocusZone.SEASONS_BANNER -> {
                        val minTop = containerSize * 0.04f
                        val maxBottom = containerSize * 0.88f
                        if (offset >= minTop && (offset + size) <= maxBottom) {
                            0f // Карточка описания внутри безопасной зоны — экран 100% неподвижен!
                        } else {
                            (offset + (size / 2f)) - (containerSize * 0.25f)
                        }
                    }
                    TvFocusZone.SEASONS_EPISODES -> {
                        val minTopForFullSection = containerSize * 0.45f // Серия не должна быть выше 45% экрана, чтобы шапка сезона над ней не была обрезана
                        val bottomThreshold = containerSize * 0.88f      // Серия не должна быть ниже 88% экрана (пик рекомендаций)
                        val elementCenter = offset + (size / 2f)

                        if (offset >= minTopForFullSection && (offset + size) <= bottomThreshold) {
                            0f // Вся секция сезонов целиком на экране — 0px сдвига, полная неподвижность!
                        } else if (offset < minTopForFullSection) {
                            // Пришли снизу (от рекомендаций), шапка сезонов спрятана выше экрана -> плавно опускаем экран вниз
                            elementCenter - (containerSize * 0.65f)
                        } else {
                            (offset + size) - bottomThreshold
                        }
                    }
                    TvFocusZone.MIDDLE -> {
                        val targetPivot = containerSize * 0.35f
                        val elementCenter = offset + (size / 2f)
                        elementCenter - targetPivot
                    }
                    TvFocusZone.BOTTOM -> {
                        val targetPivot = containerSize * 0.38f
                        val elementCenter = offset + (size / 2f)
                        elementCenter - targetPivot
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val scrollThresholdPx = with(density) { (maxHeight * 0.3f).toPx() }
        val isDescriptionVisible = focusZone.value != TvFocusZone.HEADER || scrollState.value > scrollThresholdPx

        // Backdrop — fixed layer outside scroll
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.85f)
                .graphicsLayer {
                    translationY = -scrollState.value * 0.3f
                    alpha = 1f - (scrollState.value / 800f).coerceIn(0f, 1f)
                }
        ) {
            BackdropImageSlot(state = state.header?.state)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.3f to backgroundColor.copy(alpha = 0.2f),
                                0.6f to backgroundColor.copy(alpha = 0.7f),
                                1.0f to backgroundColor
                            )
                        )
                    )
            )
        }

        // Scrollable content with dynamic pivot
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides dynamicSpec
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight * 0.7f)
                        .padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (it.hasFocus) focusZone.value = TvFocusZone.HEADER }
                            .focusGroup(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TitleSlot(state = state.header?.state)
                        MetadataSlot(state = state.header?.state, onAction = onAction)
                        CriticsRatingsSlot(state = state.header?.state)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Левая колонка: кнопки смотреть, источники, статус, оценка
                    Column(
                        modifier = Modifier.weight(0.5f),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Эта часть всё ещё держит фокус шапки (pivot = 0.9f)
                        Row(
                            modifier = Modifier
                                .focusRequester(buttonsFocusRequester)
                                .focusGroup()
                                .onFocusChanged { if (it.hasFocus) focusZone.value = TvFocusZone.HEADER }
                                .padding(start = 32.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayButtonsSlot(state = state.playButtons?.state, onAction = onAction)
                            CollectionButtonsSlot(state = state.collectionButtons?.state, onAction = onAction)
                        }

                        // Эта часть смещает фокус вниз (pivot = 0.35f)
                        Column(
                            modifier = Modifier.onFocusChanged { if (it.hasFocus) focusZone.value = TvFocusZone.MIDDLE }
                        ) {
                            Box(modifier = Modifier.focusGroup()) {
                                ContinueWatchingSlot(state = state.continueWatching?.state, onAction = onAction)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.focusGroup()) {
                                MediaSourcesSlot(
                                    isExpanded = state.isSourcesExpanded,
                                    mediaSourcesList = state.mediaSourcesList,
                                    torrentInspectorState = state.torrentInspector?.state,
                                    onClose = { onCloseSources?.invoke() },
                                    onSelectSource = onSelectSource,
                                    onRefreshSources = onRefreshSources,
                                    onAction = onAction
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                                    .padding(top = 16.dp)
                                    .focusGroup(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatusAndRatingSlot(state = state.userActions?.state, onAction = onAction)
                                SyncStatusSlot(state = state.syncStatus?.state)
                            }
                        }
                    }

                    // Правая колонка: описание фильма
                    val descriptionAlpha by animateFloatAsState(
                        targetValue = if (isDescriptionVisible) 1f else 0f,
                        animationSpec = tween(durationMillis = 300)
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.5f)
                            .alpha(descriptionAlpha)
                            .focusProperties {
                                if (!isDescriptionVisible) {
                                    onEnter = { FocusRequester.Cancel }
                                }
                            }
                            .onFocusChanged { if (it.hasFocus) focusZone.value = TvFocusZone.MIDDLE },
                        verticalArrangement = Arrangement.Top
                    ) {
                        Box(modifier = Modifier.focusGroup()) {
                            MediaDescriptionSlot(state = state.description?.state)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.focusGroup().onFocusChanged { if (it.hasFocus) focusZone.value = TvFocusZone.MIDDLE }) {
                    CastSlot(state = state.cast?.state, onAction = onAction)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.focusGroup()) {
                    TvTvSeasonsSection(
                        state = state.tvSeasons?.state,
                        onAction = onAction,
                        onHeaderFocus = { focusZone.value = TvFocusZone.SEASONS_HEADER },
                        onBannerFocus = { focusZone.value = TvFocusZone.SEASONS_BANNER },
                        onEpisodesFocus = { focusZone.value = TvFocusZone.SEASONS_EPISODES }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.focusGroup().onFocusChanged { if (it.hasFocus) focusZone.value = TvFocusZone.BOTTOM }) {
                    CommentsSlot(state = state.comments?.state, onAction = onAction)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.focusGroup().onFocusChanged { if (it.hasFocus) focusZone.value = TvFocusZone.BOTTOM }) {
                    CarouselsSlot(state = state.carousels, onAction = onAction)
                }
            }
        }

        if (isPlayerOpen) {
            PlayerSectionSlot(
                mediaKey = state.mediaKey,
                seriesTitle = state.header?.state?.data?.title ?: "",
                playerState = state.playerState,
                onClose = { onClosePlayer?.invoke() },
                onRequestOtherSource = onRequestOtherSource,
                onAction = onAction
            )
        }
    }
}

enum class TvFocusZone {
    HEADER,
    MIDDLE,
    SEASONS_HEADER,
    SEASONS_BANNER,
    SEASONS_EPISODES,
    BOTTOM
}
