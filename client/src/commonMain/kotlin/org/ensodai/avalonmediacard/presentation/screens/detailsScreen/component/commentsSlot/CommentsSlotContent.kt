package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.commentsSlot

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.CommentItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.CarouselNavigationZone
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun CommentsSlotContent(
    component: SlotData.Comments,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    if (component.comments.isEmpty() && !isLoading) return

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Проверяем, можно ли скроллить
    val canScrollForward by remember { derivedStateOf { scrollState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { scrollState.canScrollBackward } }

    val loadMore = component.loadMoreAction

    // Автоматическая фоновая подгрузка при достижении конца списка
    if (loadMore != null) {
        val triggeredActions = remember { mutableSetOf<Action>() }
        LaunchedEffect(scrollState, loadMore) {
            snapshotFlow { scrollState.layoutInfo }
                .collect { layoutInfo ->
                    val totalItems = layoutInfo.totalItemsCount
                    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

                    if (totalItems >= 5 && lastVisibleItemIndex >= totalItems - 3) {
                        if (triggeredActions.add(loadMore)) {
                            onAction(loadMore)
                        }
                    }
                }
        }
    }

    val showForwardButton = canScrollForward || loadMore != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
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

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 40.dp)
                ) {
                    items(3) {
                        CommentSkeletonCard()
                    }
                }
            } else {
                LazyRow(
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 40.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(component.comments, key = { it.id }) { comment ->
                        CommentCard(comment = comment)
                    }
                }

                // Кнопка НАЗАД (Слева)
                if (canScrollBackward) {
                    CarouselNavigationZone(isRight = false) {
                        coroutineScope.launch {
                            val layoutInfo = scrollState.layoutInfo

                            // 1. Вычисляем точную физическую координату начала правого градиента
                            val rightOverlayWidthPx = with(density) { 80.dp.toPx() }
                            val visibleRightEdge = layoutInfo.viewportEndOffset - rightOverlayWidthPx

                            // 2. Считаем, сколько карточек сейчас РЕАЛЬНО помещается в зону видимости
                            val visibleItemsCount = layoutInfo.visibleItemsInfo.count { it.offset < visibleRightEdge }

                            val itemsToScroll = (visibleItemsCount - 1).coerceAtLeast(1)
                            val targetIndex = (scrollState.firstVisibleItemIndex - itemsToScroll).coerceAtLeast(0)
                            scrollState.animateScrollToItem(targetIndex)
                        }
                    }
                }

                // Кнопка ВПЕРЕД (Справа)
                if (showForwardButton) {
                    CarouselNavigationZone(isRight = true) {
                        coroutineScope.launch {
                            val layoutInfo = scrollState.layoutInfo
                            val totalItems = layoutInfo.totalItemsCount

                            if (totalItems == 0) return@launch

                            // 1. Вычисляем границу видимой зоны (там, где начинается 80dp градиент)
                            val rightOverlayWidthPx = with(density) { 80.dp.toPx() }
                            val visibleRightEdge = layoutInfo.viewportEndOffset - rightOverlayWidthPx

                            // 2. Ищем ПОСЛЕДНИЙ элемент, левый край которого левее градиента
                            // (именно его пользователь видит обрезанным)
                            val targetItem = layoutInfo.visibleItemsInfo.lastOrNull { it.offset < visibleRightEdge }

                            // 3. Защита от багов: гарантируем, что список сдвинется хотя бы на 1 шаг
                            val calculatedIndex = targetItem?.index ?: (scrollState.firstVisibleItemIndex + 1)
                            val targetIndex = maxOf(
                                scrollState.firstVisibleItemIndex + 1,
                                calculatedIndex
                            ).coerceAtMost(totalItems - 1)

                            if (loadMore != null && targetIndex >= totalItems - 3) {
                                onAction(loadMore)
                            }

                            scrollState.animateScrollToItem(targetIndex)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CommentSkeletonCard() {
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(200.dp)
            .shimmerPlaceholder(true, RoundedCornerShape(12.dp))
    )
}

@Composable
private fun CommentCard(comment: CommentItem) {
    var showSpoiler by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var hasVisualOverflow by remember { mutableStateOf(false) }

    // НОВОЕ: Локальное состояние активности (наведен/в фокусе)
    var isActive by remember { mutableStateOf(false) }

    val isClickable = (comment.isSpoiler && !showSpoiler) || hasVisualOverflow
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    // Запускаем эффект, когда меняется состояние expanded или showSpoiler
    LaunchedEffect(expanded, showSpoiler) {
        if (expanded || showSpoiler) {
            // 1. Вычисляем целевую ширину карточки (в dp)
            val targetWidthDp = if (expanded) 480.dp else 300.dp

            // 2. Добавляем "виртуальный отступ" (60.dp), чтобы компенсировать
            // разницу между градиентом (80dp) и паддингом LazyRow (40dp) + дать немного воздуха
            val extraMarginDp = 60.dp

            // 3. Переводим всё в пиксели
            val leftPx = with(density) { (-extraMarginDp).toPx() } // Сдвигаем левый край левее нуля
            val rightPx = with(density) { (targetWidthDp + extraMarginDp).toPx() } // Сдвигаем правый край
            val heightPx = with(density) { 200.dp.toPx() } // Высота примерно

            // 4. Просим Compose проскроллить под наш кастомный виртуальный размер,
            // не дожидаясь окончания анимации самой карточки!
            bringIntoViewRequester.bringIntoView(
                Rect(left = leftPx, top = 0f, right = rightPx, bottom = heightPx)
            )
        }
    }

    val defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    // НОВОЕ: Динамические цвета для текста! Если карточка активна - текст белеет.
    val authorTextColor by animateColorAsState(
        targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
    )
    val commentTextColor by animateColorAsState(
        targetValue = if (isActive) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
    )

    // НОВОЕ: Решаем, какой цвет рамки будет при активности.
    // Если кликабельно - белая рамка. Если нет - оставляем дефолтную!
    val activeBorderColor = if (isClickable) Color.White.copy(alpha = 0.8f) else defaultBorderColor


    Column(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoViewRequester)

            // 1. ХОВЕР И МАСШТАБ (Должен быть первым!)
            // Он обернет и увеличит ВСЁ, что написано ниже, не обрезая края.
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f,
                activeBorderWidth = 1.dp,
                activeBorderColor = activeBorderColor,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = defaultBorderColor,
                shape = RoundedCornerShape(12.dp),
                clickEnabled = isClickable,
                onStateChange = { isActive = it }
            )

            // 2. АНИМАЦИЯ РАЗМЕРА
            .animateContentSize()

            // 3. ФИЗИЧЕСКИЕ РАЗМЕРЫ КАРТОЧКИ
            .width(if (expanded) 480.dp else 300.dp)
            .then(
                if (expanded) Modifier.wrapContentHeight() else Modifier.height(200.dp)
            )

            // 4. ФОН
            // Задаем форму скругления явно, чтобы фон ложился идеально
            .background(backgroundColor, RoundedCornerShape(12.dp))

            // 5. ЛОГИКА КЛИКА И ВНУТРЕННИЙ ОТСТУП
            .then(
                if (isClickable) {
                    Modifier.clickable {
                        if (comment.isSpoiler && !showSpoiler) {
                            showSpoiler = true
                        } else {
                            expanded = !expanded
                        }
                    }
                } else Modifier
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Аватарка
            ShimmerImage(
                model = comment.authorAvatarUrl.takeIf { !it.isNullOrEmpty() },
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                errorIcon = Lucide.User,
                errorIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Имя и дата
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = comment.authorName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = authorTextColor, // ПРИМЕНИЛИ АНИМИРОВАННЫЙ ЦВЕТ
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val date = comment.dateText
                if (!date.isNullOrEmpty()) {
                    Text(
                        text = date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // Дату можно не высветлять, чтобы был контраст
                    )
                }
            }

            // Оценка пользователя (userRating)
            if (comment.userRating != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                    // Убрали background, border и padding
                ) {
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = "User Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = comment.userRating.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = authorTextColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Лайки
            if (comment.likesCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Heart,
                        contentDescription = "Likes",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = comment.likesCount.toString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Содержимое отзыва
        if (comment.isSpoiler && !showSpoiler) {
            // Спойлер-заглушка
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.EyeOff,
                    contentDescription = "Spoiler warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.details_comments_spoilers),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            // Обычный текст отзыва с авто-вычислением переполнения
            val textModifier = if (expanded) Modifier else Modifier.weight(1f, fill = false)
            Text(
                text = comment.commentText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = commentTextColor,
                maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    if (!expanded) {
                        hasVisualOverflow = textLayoutResult.hasVisualOverflow
                    }
                },
                modifier = textModifier
            )
        }
    }
}
