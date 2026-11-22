package org.ensodai.avalonmediacard.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.ClickstreamPayload
import org.ensodai.avalonmediacard.contract.model.ClickstreamTargetType
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.MovieCarouselItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.*
import org.ensodai.avalonmediacard.presentation.telemetry.LocalTelemetryTracker
import org.ensodai.avalonmediacard.presentation.telemetry.TrackCarouselImpressions
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

@Composable
fun MovieCarousel(
    state: SlotUiState<SlotData.Carousel>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 32.dp
) {
    if (state.hasError && state.error != null) {
        SlotErrorCard(
            message = state.error,
            retryAction = state.retryAction,
            onAction = onAction,
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        return
    }
    
    val component = state.data ?: SlotData.Carousel(id = "skeleton", title = "", items = emptyList())
    MovieCarouselInternal(
        component = component,
        isLoading = state.isInitialLoading,
        onAction = onAction,
        modifier = modifier,
        horizontalPadding = horizontalPadding
    )
}

@Composable
private fun MovieCarouselInternal(
    component: SlotData.Carousel,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 32.dp
) {
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val canScrollForward by remember { derivedStateOf { scrollState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { scrollState.canScrollBackward } }
    val loadMore = if (isLoading) null else component.loadMoreAction
    val triggeredActions = remember { mutableSetOf<Action>() }

    // Автоматическая подгрузка следующей страницы
    if (loadMore != null) {
        LaunchedEffect(scrollState, loadMore) {
            snapshotFlow { scrollState.layoutInfo }
                .collect { layoutInfo ->
                    val totalItems = layoutInfo.totalItemsCount
                    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (totalItems > 0 && lastVisibleItemIndex >= totalItems - 15) {
                        if (triggeredActions.add(loadMore)) {
                            onAction(loadMore)
                        }
                    }
                }
        }
    }

    // Трекинг показов карточек
    val context = component.telemetryContext ?: ClickstreamContext.CAROUSEL_DISCOVER
    TrackCarouselImpressions(scrollState, context)

    // Кнопка вперед показывается ТОЛЬКО если лента физически может прокручиваться
    val showForwardButton = canScrollForward && !isLoading

    val carouselInteractionSource = remember { MutableInteractionSource() }
    val isCarouselHovered by carouselInteractionSource.collectIsHoveredAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusRestorer()
            .focusGroup()
            .logFocus("MovieCarousel_${component.title}")
            .padding(vertical = 24.dp)
    ) {
        // === ЗАГОЛОВОК КАРУСЕЛИ ===
        CarouselHeader(
            title = component.title,
            titleAction = component.titleAction,
            isLoading = isLoading,
            onAction = onAction,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // === САМА КАРУСЕЛЬ И ЗОНЫ СКРОЛЛА ===
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(carouselInteractionSource),
            contentAlignment = Alignment.Center
        ) {
            val targetVisibleItems = 6.25f
            val horizontalPaddingTotal = horizontalPadding * 2
            val spacingTotal = 16.dp * 6
            val calculatedWidth = (maxWidth - horizontalPaddingTotal - spacingTotal) / targetVisibleItems
            val cardWidth = calculatedWidth.coerceAtLeast(90.dp)

            val deviceTarget = LocalDeviceTarget.current

            val lazyRowContent: @Composable () -> Unit = {
                LazyRow(
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        items(10) { MovieSkeletonCard(cardWidth = cardWidth) }
                    } else {
                        itemsIndexed(
                            items = component.items,
                            key = { index, item -> "${item.key.type.name.lowercase()}:${item.key.id}_$index" }
                        ) { index, item ->
                            MovieCard(
                                item = item,
                                carouselContext = context,
                                positionIndex = index,
                                cardWidth = cardWidth,
                                onAction = onAction
                            )
                        }
                    }
                }
            }

            if (deviceTarget.isTv) {
                TvHorizontalFocusProvider(pivotFraction = 0.5f) {
                    lazyRowContent()
                }
            } else {
                lazyRowContent()
            }

            // Кнопка НАЗАД (плавно проявляется при hover)
            if (canScrollBackward && !isLoading) {
                CarouselNavigationZone(isRight = false, isVisible = isCarouselHovered) {
                    coroutineScope.launch {
                        val layoutInfo = scrollState.layoutInfo
                        val rightOverlayWidthPx = with(density) { 72.dp.toPx() }
                        val visibleRightEdge = layoutInfo.viewportEndOffset - rightOverlayWidthPx
                        val visibleItemsCount = layoutInfo.visibleItemsInfo.count { it.offset < visibleRightEdge }

                        val itemsToScroll = (visibleItemsCount - 1).coerceAtLeast(1)
                        val targetIndex = (scrollState.firstVisibleItemIndex - itemsToScroll).coerceAtLeast(0)
                        scrollState.animateScrollToItem(targetIndex)
                    }
                }
            }

            // Кнопка ВПЕРЕД (плавно проявляется при hover)
            if (showForwardButton) {
                CarouselNavigationZone(isRight = true, isVisible = isCarouselHovered) {
                    coroutineScope.launch {
                        val layoutInfo = scrollState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        if (totalItems == 0) return@launch

                        val rightOverlayWidthPx = with(density) { 72.dp.toPx() }
                        val visibleRightEdge = layoutInfo.viewportEndOffset - rightOverlayWidthPx

                        val targetItem = layoutInfo.visibleItemsInfo.lastOrNull { it.offset < visibleRightEdge }
                        val calculatedIndex = targetItem?.index ?: (scrollState.firstVisibleItemIndex + 1)
                        val targetIndex =
                            maxOf(scrollState.firstVisibleItemIndex + 1, calculatedIndex).coerceAtMost(totalItems - 1)

                        if (loadMore != null && targetIndex >= totalItems - 15) {
                            if (triggeredActions.add(loadMore)) {
                                onAction(loadMore)
                            }
                        }
                        scrollState.animateScrollToItem(targetIndex)
                    }
                }
            }
        }
    }
}

// === ВЫНЕСЕННЫЙ ЗАГОЛОВОК ===
@Composable
private fun CarouselHeader(
    title: String,
    titleAction: Action?,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    if (titleAction != null && !isLoading) {
        var isHeaderActive by remember { mutableStateOf(false) }
        val headerColor by animateColorAsState(
            targetValue = if (isHeaderActive) AvalonTheme.colors.textPrimary else AvalonTheme.colors.textPrimary.copy(alpha = 0.85f),
            label = "header_color"
        )
        val arrowOffset by animateDpAsState(
            targetValue = if (isHeaderActive) 4.dp else 0.dp,
            label = "arrow_offset"
        )

        Row(
            modifier = modifier
                .wrapContentSize()
                .tvAndWebHoverEffect(
                    scaleTarget = 1.0f,
                    activeBorderColor = Color.Transparent,
                    shape = AvalonTheme.shapes.control,
                    onStateChange = { isHeaderActive = it },
                    onClick = { onAction(titleAction) }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvalonText(
                text = title,
                style = AvalonTheme.typography.sectionTitle,
                color = headerColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            AvalonIcon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = AvalonTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp).offset(x = arrowOffset)
            )
        }
    } else {
        AvalonText(
            text = title,
            style = AvalonTheme.typography.sectionTitle,
            color = if (isLoading) Color.Transparent else AvalonTheme.colors.textPrimary,
            modifier = modifier.shimmerPlaceholder(isLoading, AvalonTheme.shapes.micro)
        )
    }
}

// === ВЫНЕСЕННАЯ КАРТОЧКА ФИЛЬМА ===
@Composable
private fun MovieCard(
    item: MovieCarouselItem,
    carouselContext: ClickstreamContext,
    positionIndex: Int,
    cardWidth: androidx.compose.ui.unit.Dp,
    onAction: (Action) -> Unit
) {
    val telemetry = LocalTelemetryTracker.current
    var isHoveredOrFocused by remember { mutableStateOf(false) }
    val veilAlpha by animateFloatAsState(
        targetValue = if (isHoveredOrFocused) 0.0f else 0.06f,
        label = "veil_alpha"
    )

    Box(
        modifier = Modifier
            .width(cardWidth)
            .aspectRatio(2f / 3f)
            .tvAndWebHoverEffect(
                shape = AvalonTheme.shapes.media,
                activeBorderColor = AvalonTheme.colors.borderFocus,
                activeBorderWidth = AvalonTheme.elevation.focusBorderWidth,
                onStateChange = { isHoveredOrFocused = it },
                onClick = {
                    telemetry.logClick(
                        targetType = when (item.key.type) {
                            EntityType.PERSON -> ClickstreamTargetType.PERSON
                            EntityType.TV -> ClickstreamTargetType.MEDIA_TV
                            else -> ClickstreamTargetType.MEDIA_MOVIE
                        },
                        targetId = item.key.id,
                        context = carouselContext,
                        payload = ClickstreamPayload.CarouselInteraction(positionIndex)
                    )

                    val action = if (item.key.type == EntityType.PERSON) {
                        ActionNavigate(Screen.Person(key = item.key, personName = item.title))
                    } else {
                        ActionNavigate(Screen.Details(key = item.key))
                    }
                    onAction(action)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        ShimmerImage(
            model = item.posterUrl.takeIf { !it.isNullOrEmpty() && it != "placeholder" },
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            shape = AvalonTheme.shapes.media
        )

        // 6% затемняющая вуаль в покое для выравнивания контраста (снимается при hover/focus)
        if (veilAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(AvalonTheme.shapes.media)
                    .background(AvalonTheme.colors.mediaRestingVeil.copy(alpha = veilAlpha))
            )
        }

        // Внутренний волосковый контур (Закон 5 Design Craft)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = AvalonTheme.elevation.borderWidth,
                    color = AvalonTheme.colors.mediaHairline,
                    shape = AvalonTheme.shapes.media
                )
        )
    }
}

@Composable
private fun MovieSkeletonCard(
    cardWidth: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .width(cardWidth)
            .aspectRatio(2f / 3f)
            .shimmerPlaceholder(true, AvalonTheme.shapes.media)
    )
}
