package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import org.ensodai.avalonmediacard.contract.logging.AppLogging

private val logger = AppLogging.logger("MediaDetailsLayout")

@Composable
fun MediaDetailsLayout(
    backdrop: @Composable () -> Unit,
    poster: @Composable () -> Unit,
    title: @Composable () -> Unit,
    metadata: @Composable () -> Unit,
    criticsRatings: @Composable () -> Unit,
    playButtons: @Composable () -> Unit,
    collectionButtons: @Composable () -> Unit,
    continueWatching: @Composable () -> Unit,
    statusAndRating: @Composable () -> Unit,
    syncStatus: @Composable () -> Unit,
    description: @Composable () -> Unit,
    tvSeasons: @Composable () -> Unit,
    mediaSources: @Composable () -> Unit,
    cast: @Composable () -> Unit,
    carousels: @Composable () -> Unit,
    comments: @Composable () -> Unit,
    otherContent: @Composable ColumnScope.() -> Unit,
    playerSlot: @Composable () -> Unit,
    isPlayerOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val deviceTarget = LocalDeviceTarget.current
    logger.d { "[TARGET_LOG] MediaDetailsLayout root entry: isTv=${deviceTarget.isTv}, target=$deviceTarget" }
    if (deviceTarget.isTv || deviceTarget.isTouch) {
        MediaDetailsLayoutTv(
            backdrop,
            poster,
            title,
            metadata,
            criticsRatings,
            playButtons,
            collectionButtons,
            continueWatching,
            statusAndRating,
            syncStatus,
            description,
            tvSeasons,
            mediaSources,
            cast,
            carousels,
            comments,
            otherContent,
            playerSlot,
            isPlayerOpen,
            modifier
        )
    } else {
        MediaDetailsLayoutWeb(
            backdrop,
            poster,
            title,
            metadata,
            criticsRatings,
            playButtons,
            collectionButtons,
            continueWatching,
            statusAndRating,
            syncStatus,
            description,
            tvSeasons,
            mediaSources,
            cast,
            carousels,
            comments,
            otherContent,
            playerSlot,
            modifier
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun MediaDetailsLayoutTv(
    backdrop: @Composable () -> Unit,
    poster: @Composable () -> Unit,
    title: @Composable () -> Unit,
    metadata: @Composable () -> Unit,
    criticsRatings: @Composable () -> Unit,
    playButtons: @Composable () -> Unit,
    collectionButtons: @Composable () -> Unit,
    continueWatching: @Composable () -> Unit,
    statusAndRating: @Composable () -> Unit,
    syncStatus: @Composable () -> Unit,
    description: @Composable () -> Unit,
    tvSeasons: @Composable () -> Unit,
    mediaSources: @Composable () -> Unit,
    cast: @Composable () -> Unit,
    carousels: @Composable () -> Unit,
    comments: @Composable () -> Unit,
    otherContent: @Composable ColumnScope.() -> Unit,
    playerSlot: @Composable () -> Unit,
    isPlayerOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    val buttonsFocusRequester = remember { FocusRequester() }
    val headerFocusState = remember { mutableStateOf(true) }

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
                val pf = if (headerFocusState.value) 0.9f else 0.35f
                val targetPivot = containerSize * pf
                val elementCenter = offset + (size / 2f)
                return elementCenter - targetPivot
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        val screenHeight = maxHeight
        val density = androidx.compose.ui.platform.LocalDensity.current
        val scrollThresholdPx = with(density) { (maxHeight * 0.3f).toPx() }
        val isDescriptionVisible = !headerFocusState.value || scrollState.value > scrollThresholdPx

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
            backdrop()
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
                            .onFocusChanged { if (it.hasFocus) headerFocusState.value = true }
                            .focusGroup(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        title()
                        metadata()
                        criticsRatings()
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
                                .onFocusChanged { if (it.hasFocus) headerFocusState.value = true }
                                .padding(start = 32.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            playButtons()
                            collectionButtons()
                        }

                        // Эта часть смещает фокус вниз (pivot = 0.35f)
                        Column(
                            modifier = Modifier.onFocusChanged { if (it.hasFocus) headerFocusState.value = false }
                        ) {
                            Box(modifier = Modifier.focusGroup()) { continueWatching() }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.focusGroup()) { mediaSources() }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                                    .padding(top = 16.dp)
                                    .focusGroup(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                statusAndRating()
                                syncStatus()
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
                            .onFocusChanged { if (it.hasFocus) headerFocusState.value = false },
                        verticalArrangement = Arrangement.Top
                    ) {
                        Box(modifier = Modifier.focusGroup()) { description() }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.focusGroup()) { cast() }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.focusGroup()) { tvSeasons() }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.focusGroup()) { comments() }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.focusGroup()) { carousels() }
                Spacer(modifier = Modifier.height(24.dp))
                otherContent()
            }
        }
        playerSlot()
    }
}

@Composable
fun MediaDetailsLayoutWeb(
    backdrop: @Composable () -> Unit,
    poster: @Composable () -> Unit,
    title: @Composable () -> Unit,
    metadata: @Composable () -> Unit,
    criticsRatings: @Composable () -> Unit,
    playButtons: @Composable () -> Unit,
    collectionButtons: @Composable () -> Unit,
    continueWatching: @Composable () -> Unit,
    statusAndRating: @Composable () -> Unit,
    syncStatus: @Composable () -> Unit,
    description: @Composable () -> Unit,
    tvSeasons: @Composable () -> Unit,
    mediaSources: @Composable () -> Unit,
    cast: @Composable () -> Unit,
    carousels: @Composable () -> Unit,
    comments: @Composable () -> Unit,
    otherContent: @Composable ColumnScope.() -> Unit,
    playerSlot: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    logger.d { "[TARGET_LOG] >>> ENTERED MediaDetailsLayoutWeb (Web/PC Target)" }
    val scrollState = rememberScrollState()
    Box {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .background(Color.Black)
                    .clipToBounds()
            ) {

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            translationY = scrollState.value * 0.5f
                            alpha = 1f - (scrollState.value / 600f).coerceIn(0f, 1f)
                        }
                ) {
                    backdrop()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.3f to Color.Black.copy(alpha = 0.2f),
                                    0.6f to Color.Black.copy(alpha = 0.7f),
                                    1.0f to Color.Black
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, end = 32.dp, bottom = 40.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        title()
                        metadata()
                        criticsRatings()

                        Row(
                            modifier = Modifier.focusGroup(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            playButtons()
                            collectionButtons()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.focusGroup()) { continueWatching() }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.focusGroup()) { mediaSources() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                statusAndRating()
                syncStatus()
            }

            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.focusGroup()) { description() }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.focusGroup()) { cast() }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.focusGroup()) { tvSeasons() }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.focusGroup()) { comments() }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.focusGroup()) { carousels() }
            Spacer(modifier = Modifier.height(24.dp))
            otherContent()
        }
        playerSlot()
    }
}
