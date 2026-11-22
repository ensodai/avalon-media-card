package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder

@Composable
fun WebHeroSection(
    headerData: SlotData.Header?,
    descriptionData: SlotData.Text?,
    playButtons: SlotData.ButtonGroup?,
    collectionButtons: SlotData.ButtonGroup?,
    userActions: SlotData.UserActions?,
    isHeaderLoading: Boolean = false,
    isDescriptionLoading: Boolean = false,
    isPlayButtonsLoading: Boolean = false,
    onAction: (Action) -> Unit,
    scrollOffset: Int = 0,
    heroHeight: Dp = 760.dp,
    onRequestOtherSource: (() -> Unit)? = null,
    onOpenWatchParty: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backdropUrl = headerData?.backgroundUrl ?: headerData?.posterUrl
    val title = headerData?.title ?: ""
    val originalTitle = headerData?.originalTitle?.takeIf { it.isNotBlank() && it != title }
    val description = descriptionData?.content?.takeIf { it.isNotBlank() } ?: headerData?.tagline ?: ""

    val canvas = AvalonTheme.colors.bgCanvas

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(canvas)
            .clipToBounds()
    ) {
        // 1. Full-bleed 100vw Backdrop Image with Smooth Parallax
        if (!backdropUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationY = scrollOffset * 0.45f
                        alpha = (1f - scrollOffset / 750f).coerceIn(0f, 1f)
                    }
            ) {
                ShimmerImage(
                    model = backdropUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Left-edge narrow feather to dissolve image boundary into canvas without darkening the art
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to canvas,
                            0.03f to canvas.copy(alpha = 0.70f),
                            0.08f to Color.Transparent
                        )
                    )
                )
        )

        // 3. Top subtle vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to canvas.copy(alpha = 0.25f),
                            0.08f to Color.Transparent
                        )
                    )
                )
        )

        // 4. Bottom Ease-in cinematic fade: top 45% completely clear, gentle fade down to bgCanvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.65f to canvas.copy(alpha = 0.55f),
                            0.82f to canvas.copy(alpha = 0.92f),
                            1.0f to canvas
                        )
                    )
                )
        )

        // 5. Centered Content Container with Semantic Vertical Rhythm
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 44.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1320.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // 1. Title Block (Tight Gestalt grouping with smooth Crossfade)
                Crossfade(
                    targetState = isHeaderLoading,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) { loading ->
                    if (loading) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(360.dp)
                                    .height(44.dp)
                                    .shimmerPlaceholder(true, RoundedCornerShape(8.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(18.dp)
                                    .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                            )
                        }
                    } else if (title.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AvalonText(
                                text = title,
                                style = AvalonTheme.typography.displayHero,
                                color = AvalonTheme.colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (originalTitle != null) {
                                AvalonText(
                                    text = originalTitle,
                                    style = AvalonTheme.typography.metadata,
                                    color = AvalonTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Rhythm: Gap to Meta (14dp)
                Spacer(modifier = Modifier.height(14.dp))

                // 2. Clean Meta Line (Rating, Year, MediaType, Genres)
                WebMetaLine(
                    headerData = headerData,
                    isLoading = isHeaderLoading,
                    onAction = onAction
                )

                // 3. Compact Description with Generous Line-Height (max-width: 560dp)
                Crossfade(
                    targetState = isDescriptionLoading,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) { loading ->
                    if (loading) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(top = 20.dp)
                                .widthIn(max = 560.dp)
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(14.dp)
                                    .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(14.dp)
                                    .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                            )
                        }
                    } else if (description.isNotBlank()) {
                        Column(modifier = Modifier.padding(top = 20.dp)) {
                            AvalonText(
                                text = description,
                                style = AvalonTheme.typography.bodySynopsis,
                                color = AvalonTheme.colors.textSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 560.dp)
                            )
                        }
                    }
                }

                // Rhythm: Gap to Action Toolbar (28dp)
                Spacer(modifier = Modifier.height(28.dp))

                // 4. Action Toolbar
                WebActionToolbar(
                    playButtons = playButtons,
                    collectionButtons = collectionButtons,
                    userActions = userActions,
                    isLoading = isPlayButtonsLoading,
                    onAction = onAction,
                    onRequestOtherSource = onRequestOtherSource,
                    onOpenWatchParty = onOpenWatchParty
                )
            }
        }
    }
}
