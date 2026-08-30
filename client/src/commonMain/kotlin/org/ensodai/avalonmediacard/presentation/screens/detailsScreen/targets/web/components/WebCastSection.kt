package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.CastMemberItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.CarouselNavigationZone
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun WebCastSection(
    castData: SlotData.Cast?,
    isLoading: Boolean = false,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    if (castData == null || (castData.members.isEmpty() && !isLoading)) return

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val canScrollForward by remember { derivedStateOf { scrollState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { scrollState.canScrollBackward } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Section Title with Cineby style accent
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
            Text(
                text = castData.title.ifBlank { "В главных ролях" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                state = scrollState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    items(6) { WebActorSkeletonCard() }
                } else {
                    items(castData.members, key = { it.key.id }) { member ->
                        WebActorCard(
                            member = member,
                            onAction = onAction
                        )
                    }
                }
            }

            // Scroll arrows
            if (canScrollBackward && !isLoading) {
                CarouselNavigationZone(isRight = false) {
                    coroutineScope.launch {
                        val layoutInfo = scrollState.layoutInfo
                        val visibleItemsCount = layoutInfo.visibleItemsInfo.count()
                        val itemsToScroll = (visibleItemsCount - 1).coerceAtLeast(1)
                        val targetIndex = (scrollState.firstVisibleItemIndex - itemsToScroll).coerceAtLeast(0)
                        scrollState.animateScrollToItem(targetIndex)
                    }
                }
            }

            if (canScrollForward && !isLoading) {
                CarouselNavigationZone(isRight = true) {
                    coroutineScope.launch {
                        val layoutInfo = scrollState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        if (totalItems == 0) return@launch
                        val targetIndex = minOf(scrollState.firstVisibleItemIndex + 3, totalItems - 1)
                        scrollState.animateScrollToItem(targetIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun WebActorCard(
    member: CastMemberItem,
    onAction: (Action) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val nameColor by animateColorAsState(
        targetValue = if (isHovered) Color.White else Color.White.copy(alpha = 0.9f)
    )
    val roleColor by animateColorAsState(
        targetValue = if (isHovered) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f)
    )

    Column(
        modifier = Modifier.width(110.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.05f,
                    activeBorderWidth = 2.dp,
                    activeBorderColor = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    onStateChange = { isHovered = it },
                    onClick = {
                        onAction(ActionNavigate(Screen.Person(key = member.key, personName = member.name)))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            ShimmerImage(
                model = member.profileUrl.takeIf { !it.isNullOrEmpty() && it != "placeholder" },
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                errorIcon = Lucide.User,
                errorIconTint = Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = member.name,
            fontSize = 13.sp,
            maxLines = 2,
            fontWeight = FontWeight.SemiBold,
            color = nameColor,
            lineHeight = 16.sp,
            textAlign = TextAlign.Start
        )

        val role = member.character
        if (!role.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = role,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = roleColor,
                lineHeight = 14.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun WebActorSkeletonCard() {
    Column(modifier = Modifier.width(110.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .shimmerPlaceholder(true, RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
        )
    }
}
