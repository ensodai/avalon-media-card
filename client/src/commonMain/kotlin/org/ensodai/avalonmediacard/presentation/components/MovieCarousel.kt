package org.ensodai.avalonmediacard.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.ClickstreamPayload
import org.ensodai.avalonmediacard.contract.model.ClickstreamTargetType
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.MovieCarouselItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.*
import org.ensodai.avalonmediacard.presentation.telemetry.LocalTelemetryTracker
import org.ensodai.avalonmediacard.presentation.telemetry.TrackCarouselImpressions

@Composable
fun MovieCarousel(
    state: SlotUiState<SlotData.Carousel>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
    )
}

@Composable
private fun MovieCarouselInternal(
    component: SlotData.Carousel,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val canScrollForward by remember { derivedStateOf { scrollState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { scrollState.canScrollBackward } }
    val loadMore = if (isLoading) null else component.loadMoreAction
    val triggeredActions = remember { mutableSetOf<Action>() }

    // Автоматическая подгрузка (как в отзывах)
    if (loadMore != null) {
        LaunchedEffect(scrollState, loadMore) {
            snapshotFlow { scrollState.layoutInfo }
                .collect { layoutInfo ->
                    val totalItems = layoutInfo.totalItemsCount
                    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    // Загружаем следующую страницу заранее, когда остается 15 элементов (почти целый экран)
                    if (totalItems > 0 && lastVisibleItemIndex >= totalItems - 15) {
                        if (triggeredActions.add(loadMore)) {
                            onAction(loadMore)
                        }
                    }
                }
        }
    }

    // Добавляем трекинг показов карточек
    val context = component.telemetryContext ?: ClickstreamContext.CAROUSEL_DISCOVER
    TrackCarouselImpressions(scrollState, context)

    val showForwardButton = canScrollForward || loadMore != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusGroup()
            .padding(vertical = 24.dp) // Даем больше воздуха сверху и снизу между каруселями
    ) {
        // === ЗАГОЛОВОК КАРУСЕЛИ ===
        CarouselHeader(
            title = component.title,
            titleAction = component.titleAction,
            isLoading = isLoading,
            onAction = onAction,
            // Паддинг 88dp, чтобы заголовок визуально начинался ровно там же,
            // где начинается первый постер (из-за боковых зон)
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // === САМА КАРУСЕЛЬ И ЗОНЫ СКРОЛЛА ===
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val targetVisibleItems = 6.25f
            val horizontalPadding = 80.dp // 40.dp * 2
            val spacingTotal = 16.dp * 6
            val calculatedWidth = (maxWidth - horizontalPadding - spacingTotal) / targetVisibleItems
            val cardWidth = calculatedWidth.coerceAtLeast(90.dp)

            val deviceTarget = LocalDeviceTarget.current

            val lazyRowContent: @Composable () -> Unit = {
                LazyRow(
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 40.dp), // Отступы для навигационных зон!
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

            // Кнопка НАЗАД
            if (canScrollBackward && !isLoading) {
                CarouselNavigationZone(isRight = false) {
                    coroutineScope.launch {
                        val layoutInfo = scrollState.layoutInfo
                        val rightOverlayWidthPx = with(density) { 80.dp.toPx() }
                        val visibleRightEdge = layoutInfo.viewportEndOffset - rightOverlayWidthPx
                        val visibleItemsCount = layoutInfo.visibleItemsInfo.count { it.offset < visibleRightEdge }

                        val itemsToScroll = (visibleItemsCount - 1).coerceAtLeast(1)
                        val targetIndex = (scrollState.firstVisibleItemIndex - itemsToScroll).coerceAtLeast(0)
                        scrollState.animateScrollToItem(targetIndex)
                    }
                }
            }

            // Кнопка ВПЕРЕД
            if (showForwardButton && !isLoading) {
                CarouselNavigationZone(isRight = true) {
                    coroutineScope.launch {
                        val layoutInfo = scrollState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        if (totalItems == 0) return@launch

                        val rightOverlayWidthPx = with(density) { 80.dp.toPx() }
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
        val headerColor by animateColorAsState(targetValue = if (isHeaderActive) Color.White else Color.White.copy(alpha = 0.6f))
        val arrowOffset by animateDpAsState(targetValue = if (isHeaderActive) 6.dp else 0.dp)

        Row(
            modifier = modifier
                .wrapContentSize()
                .tvAndWebHoverEffect(
                    shape = RoundedCornerShape(8.dp),
                    activeBorderColor = Color.Transparent,
                    onStateChange = { isHeaderActive = it }
                ,
    onClick = { onAction(titleAction) })
                ,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = headerColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = headerColor,
                modifier = Modifier.size(24.dp).offset(x = arrowOffset)
            )
        }
    } else {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.8f),
            modifier = modifier.shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
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
    Box(
        modifier = Modifier
            .width(cardWidth)
            .aspectRatio(2f / 3f)
            .tvAndWebHoverEffect(shape = RoundedCornerShape(12.dp),
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
            })
            ,
        contentAlignment = Alignment.Center
    ) {
        ShimmerImage(
            model = item.posterUrl.takeIf { !it.isNullOrEmpty() && it != "placeholder" },
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp)
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
            .shimmerPlaceholder(true, RoundedCornerShape(12.dp))
    )
}