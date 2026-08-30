package org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HeroBannerWidget(
    state: SlotUiState<SlotData.Hero>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    scrollOffsetProvider: () -> Float = { 0f }
) {
    val data = state.data
    val items = data?.items ?: emptyList()
    val isReady = !state.isLoading && data != null && items.isNotEmpty()

    val initialPage = 0
    val pagerState = rememberPagerState(initialPage = initialPage) { 
        if (items.isNotEmpty()) Int.MAX_VALUE else 0 
    }
    val scope = rememberCoroutineScope()

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isUserInteracting = isHovered || isFocused

    LaunchedEffect(pagerState, isUserInteracting, isReady) {
        if (isReady && !isUserInteracting && items.size > 1) {
            while (true) {
                delay(5000L.milliseconds)
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(520.dp)
            .clipToBounds()
            .onKeyEvent { event ->
                if (isReady && event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionRight -> {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            true
                        }
                        Key.DirectionLeft -> {
                            if (pagerState.currentPage > 0) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else false
            }
            .tvAndWebHoverEffect(
                scaleTarget = 1.0f,
                activeBorderWidth = 0.dp,
                activeBorderColor = Color.Transparent,
                shape = RoundedCornerShape(0.dp),
                onClick = {
                    if (isReady) {
                        val currentItem = items[pagerState.currentPage % items.size]
                        onAction(ActionNavigate(Screen.Details(currentItem.key)))
                    }
                }
            )
            .shimmerPlaceholder(isLoading = !isReady, shape = RoundedCornerShape(0.dp))
    ) {
        if (!isReady) return@Box
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (items.isEmpty()) return@HorizontalPager
            val actualIndex = page % items.size
            val currentItem = items[actualIndex]

            val itemInteractionSource = remember { MutableInteractionSource() }
            val isItemHovered by itemInteractionSource.collectIsHoveredAsState()
            val isItemFocused by itemInteractionSource.collectIsFocusedAsState()
            val isItemHighlighted = isItemHovered || isItemFocused

            val imageScale by animateFloatAsState(
                targetValue = if (isItemHighlighted) 1.04f else 1.0f,
                label = "Hero Image Scale"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                ShimmerImage(
                    model = currentItem.backdropUrl ?: currentItem.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(720.dp)
                        .offset(y = (-100).dp)
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                            translationY = scrollOffsetProvider() * 0.15f
                        }
                )

                // Темный градиент снизу для читаемости текста
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.3f to Color.Black.copy(alpha = 0.2f),
                                    0.6f to Color.Black.copy(alpha = 0.7f),
                                    0.85f to Color.Black,
                                    1.0f to Color.Black
                                )
                            )
                        )
                )

                // Информационный блок текущего фильма
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 32.dp, end = 120.dp, bottom = 40.dp)
                        .fillMaxWidth(0.75f)
                ) {
                    Text(
                        text = data?.title?.uppercase() ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentItem.title,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 46.sp
                    )
                    data?.subtitle?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // Кнопка навигации ВЛЕВО (<)
        if (items.size > 1 && pagerState.currentPage > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .tvAndWebHoverEffect(
                        scaleTarget = 1.15f,
                        activeBorderWidth = 1.dp,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = stringResource(Res.string.dashboard_hero_prev_slide),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Кнопка навигации ВПРАВО (>)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .tvAndWebHoverEffect(
                        scaleTarget = 1.15f,
                        activeBorderWidth = 1.dp,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = stringResource(Res.string.dashboard_hero_next_slide),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Счетчик и индикаторы страниц (например, "1 / 5" и точки)
        if (items.size > 1) {
            val currentActualIndex = pagerState.currentPage % items.size
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Текстовый счетчик 1 / 5
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${currentActualIndex + 1} / ${items.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Индикаторы-точки
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(items.size) { index ->
                        val isSelected = index == currentActualIndex
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 18.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    }
}
