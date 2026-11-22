package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.CastMemberItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.designsystem.components.AvalonPersonCard
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.CarouselNavigationZone

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

    val canScrollForward by remember { derivedStateOf { scrollState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { scrollState.canScrollBackward } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Section Title: clean typography without decorative bar accents
        Text(
            text = castData.title.ifBlank { "В главных ролях" },
            style = AvalonTheme.typography.sectionTitle.copy(color = AvalonTheme.colors.textPrimary),
            modifier = Modifier.padding(bottom = AvalonTheme.spacing.space16)
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                state = scrollState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    items(7) { WebActorSkeletonCard() }
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
    val photo = member.profileUrl?.takeIf { it.isNotBlank() && it != "placeholder" }

    AvalonPersonCard(
        name = member.name,
        role = member.character ?: "",
        avatarSize = 64.dp,
        onClick = {
            onAction(ActionNavigate(Screen.Person(key = member.key, personName = member.name)))
        },
        avatarContent = if (photo != null) {
            {
                ShimmerImage(
                    model = photo,
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    errorIcon = Lucide.User,
                    errorIconTint = AvalonTheme.colors.textMuted
                )
            }
        } else null
    )
}

@Composable
private fun WebActorSkeletonCard(avatarSize: Dp = 64.dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(avatarSize + 48.dp)
            .padding(AvalonTheme.spacing.space8)
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .shimmerPlaceholder(true, CircleShape)
        )
        Spacer(Modifier.height(AvalonTheme.spacing.space8))
        Box(
            modifier = Modifier
                .width(avatarSize + 16.dp)
                .height(14.dp)
                .shimmerPlaceholder(true, AvalonTheme.shapes.micro)
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(avatarSize)
                .height(12.dp)
                .shimmerPlaceholder(true, AvalonTheme.shapes.micro)
        )
    }
}
