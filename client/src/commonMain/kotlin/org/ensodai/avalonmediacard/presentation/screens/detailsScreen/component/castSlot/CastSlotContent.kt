package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.castSlot

import androidx.compose.animation.animateColorAsState
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
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.CastMemberItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.CarouselNavigationZone
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvHorizontalFocusProvider
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun CastSlotContent(
    component: SlotData.Cast,
    isLoading: Boolean,
    onAction: (Action) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (component.members.isEmpty() && !isLoading) return

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val canScrollForward by remember { derivedStateOf { scrollState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { scrollState.canScrollBackward } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        // Заголовок (Отступы 40.dp, чтобы выровнять с каруселью фильмов и комментариями)
        Text(
            text = component.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isLoading) Color.Transparent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 40.dp, vertical = 4.dp)
                .shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Зона скролла и карточки
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val deviceTarget = LocalDeviceTarget.current

            val lazyRowContent: @Composable () -> Unit = {
                LazyRow(
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 40.dp), // Отступы для градиентов
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        items(6) { ActorSkeletonCard() }
                    } else {
                        items(component.members, key = { it.key.id }) { member ->
                            ActorCard(
                                member = member,
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
            if (canScrollForward && !isLoading) { // Тут используем canScrollForward
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

                        scrollState.animateScrollToItem(targetIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActorCard(
    member: CastMemberItem, // Замените на ваш класс модели
    onAction: (Action) -> Unit
) {
    // Состояние, которое сообщит нам, наведен ли фокус на фото
    var isActive by remember { mutableStateOf(false) }

    // Анимированные цвета текста, зависящие от фокуса на фотографии!
    val nameColor by animateColorAsState(
        targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
    )
    val roleColor by animateColorAsState(
        targetValue = if (isActive) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(
        modifier = Modifier.width(110.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // === ИНТЕРАКТИВНАЯ ЗОНА (ФОТО) ===
        // Фокус, рамка и кликабельность вешаются именно на фото,
        // чтобы на ТВ выделялась картинка, а не весь текстовый блок.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                // ПРИМЕНЯЕМ НАШ ЭКСТЕНШЕН
                .tvAndWebHoverEffect(
                    scaleTarget = 1.06f, // Для актеров 1.06 отлично подходит
                    activeBorderWidth = 2.dp,
                    activeBorderColor = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    onStateChange = { isActive = it } // Передаем статус активности наружу
                ,
    onClick = {
                    onAction(ActionNavigate(Screen.Person(key = member.key, personName = member.name)))
                })
                ,
            contentAlignment = Alignment.Center
        ) {
            ShimmerImage(
                model = member.profileUrl.takeIf { !it.isNullOrEmpty() && it != "placeholder" },
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                errorIcon = Lucide.User,
                errorIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // === ТЕКСТОВАЯ ЗОНА ===
        Text(
            text = member.name,
            fontSize = 13.sp,
            maxLines = 2,
            fontWeight = FontWeight.Bold,
            color = nameColor, // Используем анимированный цвет!
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
                color = roleColor, // Используем анимированный цвет!
                lineHeight = 14.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ActorSkeletonCard() {
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