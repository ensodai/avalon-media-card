package org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.MovieCarouselItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.CarouselNavigationZone
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvHorizontalFocusProvider
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

@Composable
fun BackdropsCarouselWidget(
    state: SlotUiState<SlotData.CarouselBackdrops>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.data == null) {
        Column(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 16.dp)
                    .width(160.dp)
                    .height(24.dp)
                    .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 32.dp)
            ) {
                items(4) {
                    Box(
                        modifier = Modifier
                            .width(280.dp)
                            .height(158.dp)
                            .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(12.dp))
                    )
                }
            }
        }
        return
    }

    val data = state.data ?: return

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val canScrollForward by remember { derivedStateOf { scrollState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { scrollState.canScrollBackward } }

    val carouselInteractionSource = remember { MutableInteractionSource() }
    val isCarouselHovered by carouselInteractionSource.collectIsHoveredAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = data.title,
            color = AvalonTheme.colors.textPrimary,
            style = AvalonTheme.typography.sectionTitle,
            modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 16.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(carouselInteractionSource)
        ) {
            val targetVisibleItems = 3.5f
            val horizontalPadding = 64.dp // 32.dp * 2
            val spacingTotal = 16.dp * (targetVisibleItems.toInt())
            val calculatedWidth = (maxWidth - horizontalPadding - spacingTotal) / targetVisibleItems
            val cardWidth = calculatedWidth.coerceAtLeast(220.dp)
            val cardHeight = cardWidth / (16f / 9f)

            val deviceTarget = LocalDeviceTarget.current

            val lazyRowContent: @Composable () -> Unit = {
                LazyRow(
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 4.dp)
                ) {
                    itemsIndexed(data.items, key = { index, item -> "${item.key.id}_$index" }) { _, item ->
                        BackdropCard(
                            item = item,
                            cardWidth = cardWidth,
                            cardHeight = cardHeight,
                            onAction = onAction
                        )
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
            if (canScrollBackward && !state.isLoading) {
                CarouselNavigationZone(isRight = false, isVisible = isCarouselHovered) {
                    coroutineScope.launch {
                        val layoutInfo = scrollState.layoutInfo
                        val overlayWidthPx = with(density) { 72.dp.toPx() }
                        val visibleRightEdge = layoutInfo.viewportEndOffset - overlayWidthPx
                        val visibleItemsCount = layoutInfo.visibleItemsInfo.count { it.offset < visibleRightEdge }
                        val itemsToScroll = (visibleItemsCount - 1).coerceAtLeast(1)
                        val targetIndex = (scrollState.firstVisibleItemIndex - itemsToScroll).coerceAtLeast(0)
                        scrollState.animateScrollToItem(targetIndex)
                    }
                }
            }

            // Кнопка ВПЕРЕД (плавно проявляется при hover)
            if (canScrollForward && !state.isLoading) {
                CarouselNavigationZone(isRight = true, isVisible = isCarouselHovered) {
                    coroutineScope.launch {
                        val layoutInfo = scrollState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        if (totalItems == 0) return@launch
                        val overlayWidthPx = with(density) { 72.dp.toPx() }
                        val visibleRightEdge = layoutInfo.viewportEndOffset - overlayWidthPx
                        val targetItem = layoutInfo.visibleItemsInfo.lastOrNull { it.offset < visibleRightEdge }
                        val calculatedIndex = targetItem?.index ?: (scrollState.firstVisibleItemIndex + 1)
                        val targetIndex = maxOf(scrollState.firstVisibleItemIndex + 1, calculatedIndex).coerceAtMost(totalItems - 1)
                        scrollState.animateScrollToItem(targetIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackdropCard(
    item: MovieCarouselItem,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    var isActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .tvAndWebHoverEffect(
                scaleTarget = 1.05f,
                activeBorderColor = AvalonTheme.colors.accentPrimary,
                activeBorderWidth = 2.dp,
                shape = RoundedCornerShape(12.dp),
                onStateChange = { isActive = it },
                onClick = {
                    onAction(ActionNavigate(Screen.Details(item.key)))
                }
            )
    ) {
        ShimmerImage(
            model = item.backdropUrl ?: item.posterUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxSize()
        )

        // Внутренний волосковый контур 10% белого
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = isActive,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.5f to Color.Black.copy(alpha = 0.4f),
                                1.0f to Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isActive,
            enter = androidx.compose.animation.slideInVertically { it / 2 } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { it / 2 } + androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = null,
                    tint = AvalonTheme.colors.accentPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = item.title,
                    color = Color.White,
                    style = AvalonTheme.typography.cardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(AvalonTheme.colors.accentPrimary)
            )
        }
    }
}
