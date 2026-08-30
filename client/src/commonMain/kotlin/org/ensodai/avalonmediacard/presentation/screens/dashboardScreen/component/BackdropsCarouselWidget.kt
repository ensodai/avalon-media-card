package org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.MovieCarouselItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvHorizontalFocusProvider
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

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
                    .padding(start = 24.dp, bottom = 16.dp)
                    .width(160.dp)
                    .height(24.dp)
                    .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp)
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

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = data.title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val targetVisibleItems = 3.5f
            val horizontalPadding = 48.dp // 24.dp * 2
            val spacingTotal = 16.dp * (targetVisibleItems.toInt())
            val calculatedWidth = (maxWidth - horizontalPadding - spacingTotal) / targetVisibleItems
            val cardWidth = calculatedWidth.coerceAtLeast(220.dp)
            val cardHeight = cardWidth / (16f / 9f)

            val deviceTarget = LocalDeviceTarget.current

            val lazyRowContent: @Composable () -> Unit = {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 8.dp)
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

    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
        label = "Backdrop Card Title Color"
    )

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .tvAndWebHoverEffect(
                scaleTarget = 1.05f,
                activeBorderColor = MaterialTheme.colorScheme.primary,
                activeBorderWidth = 2.dp,
                shape = RoundedCornerShape(12.dp),
                onStateChange = { isActive = it }
            ,
    onClick = {
                onAction(ActionNavigate(Screen.Details(item.key)))
            })
            
    ) {
        ShimmerImage(
            model = item.backdropUrl ?: item.posterUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
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
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
