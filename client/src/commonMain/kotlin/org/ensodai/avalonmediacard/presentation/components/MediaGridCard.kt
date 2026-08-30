package org.ensodai.avalonmediacard.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.MovieCarouselItem
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MediaGridCard(
    item: MovieCarouselItem,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0
) {
    var isVisible by rememberSaveable { mutableStateOf(isLoading) }
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(((index % 20) * 40L).milliseconds)
            isVisible = true
        }
    }
    val animAlpha by animateFloatAsState(
        targetValue = if (isVisible || isLoading) 1f else 0f,
        animationSpec = tween(500)
    )
    val animOffsetY by animateDpAsState(
        targetValue = if (isVisible || isLoading) 0.dp else 20.dp,
        animationSpec = tween(500)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = animAlpha
                this.translationY = animOffsetY.toPx()
            }
            .padding(4.dp)
            .tvAndWebHoverEffect(
                scaleTarget = 1.05f,
                activeBorderWidth = 2.dp,
                activeBorderColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                clickEnabled = !isLoading,
                onClick = {
                    val action = if (item.key.type == EntityType.PERSON) {
                        ActionNavigate(
                            Screen.Person(
                                key = item.key,
                                personName = item.title
                            )
                        )
                    } else {
                        ActionNavigate(Screen.Details(key = item.key))
                    }
                    onAction(action)
                })

            .padding(8.dp) // Внутренний отступ от рамки до контента
    ) {
        Box {
            val posterModifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .shimmerPlaceholder(isLoading, RoundedCornerShape(8.dp))

            if (!isLoading) {
                ShimmerImage(
                    model = item.posterUrl.takeIf { !it.isNullOrEmpty() && it != "placeholder" },
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = posterModifier,
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                Box(
                    modifier = posterModifier,
                    contentAlignment = Alignment.Center
                ) {}
            }

            // Badges overlay
            if (!isLoading && item.badges.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.badges.forEach { badge ->
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(4.dp))
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFE50914), Color(0xFFB00710))),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLoading) "" else item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
        )
    }
}
