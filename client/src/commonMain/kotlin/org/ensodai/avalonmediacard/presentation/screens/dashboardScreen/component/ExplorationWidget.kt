package org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

@Composable
fun ExplorationWidget(
    state: SlotUiState<SlotData.Exploration>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.data == null) {
        Column(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .width(180.dp)
                    .height(24.dp)
                    .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
                    .padding(bottom = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                items(6) {
                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
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
            color = AvalonTheme.colors.textPrimary,
            style = AvalonTheme.typography.sectionTitle,
            modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 16.dp)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val targetVisibleItems = 5.5f
            val horizontalPadding = 64.dp // 32.dp * 2
            val spacingTotal = 16.dp * (targetVisibleItems.toInt())
            val calculatedSize = (maxWidth - horizontalPadding - spacingTotal) / targetVisibleItems
            val tileSize = calculatedSize.coerceAtLeast(110.dp)
            val gridHeight = (tileSize * 2) + 16.dp

            val deviceTarget = LocalDeviceTarget.current

            val gridContent: @Composable () -> Unit = {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                ) {
                    itemsIndexed(data.items, key = { index, item -> "${item.key.id}_$index" }) { _, item ->
                        ExplorationCard(
                            item = item,
                            tileSize = tileSize,
                            onAction = onAction
                        )
                    }
                }
            }

            if (deviceTarget.isTv) {
                TvHorizontalFocusProvider(pivotFraction = 0.5f) {
                    gridContent()
                }
            } else {
                gridContent()
            }
        }
    }
}

@Composable
private fun ExplorationCard(
    item: MovieCarouselItem,
    tileSize: androidx.compose.ui.unit.Dp,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    var isActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(tileSize)
            .tvAndWebHoverEffect(
                scaleTarget = 1.05f,
                activeBorderColor = AvalonTheme.colors.accentPrimary,
                activeBorderWidth = 2.dp,
                shape = RoundedCornerShape(16.dp),
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
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxSize()
        )

        // Внутренний волосковый контур 10% белого
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.4f),
                            1.0f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Text(
            text = item.title,
            color = if (isActive) AvalonTheme.colors.textPrimary else AvalonTheme.colors.textPrimary.copy(alpha = 0.9f),
            style = AvalonTheme.typography.cardTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(8.dp)
        )
    }
}
