package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.tvSeasonsSlot

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.contract.slot.SeasonItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.EpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvSeasonsSlotContent(
    component: SlotData.TvSeasons,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    // Если глобальная загрузка завершена, а сезонов нет — скрываем блок
    if (component.seasons.isEmpty() && !isLoading) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // === ЗАГОЛОВОК И КНОПКА "ОТМЕТИТЬ СЕЗОН" ===

        Text(
            modifier = Modifier
                .shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            text = stringResource(Res.string.details_seasons_header),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) Color.Transparent else Color.White,
        )


        Spacer(modifier = Modifier.height(16.dp))

        // === СЕЛЕКТОР СЕЗОНОВ ===
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            if (isLoading) {
                repeat(3) { SeasonTabSkeleton() }
            } else {
                component.seasons.forEach { season ->
                    val isSelected = season.seasonNumber == component.selectedSeasonNumber
                    SeasonTab(
                        season = season,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                val seasonAction = season.selectAction
                                if (seasonAction != null) {
                                    onAction(seasonAction)
                                }
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        MarkSeasonAsWatchedButton(
            isLoading = isLoading,
            component = component,
            onAction = onAction
        )

        Spacer(modifier = Modifier.height(24.dp))

        // === СПИСОК СЕРИЙ ===
        // Достаем контент текущего выбранного сезона
        // Считаем, что идет локальная загрузка, если явно стоит флаг isLoading,
        // либо если данных для этого сезона еще вообще нет в Map
        val activeSeasonContent = component.seasonContents[component.selectedSeasonNumber]
        val isEpisodesLoading = activeSeasonContent?.isLoading == true || (activeSeasonContent == null && !isLoading)

        // Стейт для разворачивания списка (сбрасывается при смене сезона)
        var isListExpanded by remember(component.selectedSeasonNumber) { mutableStateOf(false) }
        val maxVisibleItems = 5

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .zIndex(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading || isEpisodesLoading) {
                repeat(4) {
                    EpisodeListItemSkeleton(
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                val episodes = activeSeasonContent?.episodes
                if (!episodes.isNullOrEmpty()) {

                    // Обрезаем список, если он не развернут
                    val visibleEpisodes = if (isListExpanded) episodes else episodes.take(maxVisibleItems)

                    visibleEpisodes.forEach { episode ->
                        key(episode.id) {
                            EpisodeListItem(
                                modifier = Modifier.padding(horizontal = 32.dp),
                                episode = episode,
                                isWatched = episode.isWatched,
                                userRating = episode.userRating,
                                onClick = {
                                    val playAction = episode.playAction
                                    if (playAction != null) {
                                        onAction(playAction)
                                    }
                                },
                                onToggleWatch = {
                                    val toggleAction = episode.toggleWatchedAction
                                    if (toggleAction != null) {
                                        onAction(toggleAction)
                                    }
                                },
                                onRate = { newRating ->
                                    // rating not supported on individual episodes yet
                                }
                            )
                        }
                    }

                    // КНОПКА "ПОКАЗАТЬ ВСЕ"
                    if (episodes.size > maxVisibleItems && !isListExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp)
                                .tvAndWebHoverEffect(scaleTarget = 1.02f, shape = RoundedCornerShape(8.dp),
    onClick = { isListExpanded = true })
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(Res.string.details_seasons_show_all_fmt, episodes.size),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Lucide.ChevronDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                } else {
                    Text(
                        stringResource(Res.string.details_seasons_not_available),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// ВНУТРЕННИЕ КОМПОНЕНТЫ
// ============================================================================

@Composable
fun EpisodeListItem(
    modifier: Modifier,
    episode: EpisodeItem,
    isWatched: Boolean = false,
    userRating: Int? = null,
    onClick: () -> Unit,
    onToggleWatch: () -> Unit,
    onRate: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // ВАЖНО: Внешний отступ защищает нас от обрезания анимации родительским Column!
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // === 1. ЛЕВАЯ КОЛОНКА (Чеклист: Статус и Оценка) ===
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(56.dp) // Фиксированная ширина колонки чекбокса
        ) {
            // Кнопка: Отметить просмотренным
            val checkBgColor = if (isWatched) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
            val checkIconColor = if (isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f)
            val checkIcon = if (isWatched) Lucide.Check else Lucide.Eye

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .tvAndWebHoverEffect(scaleTarget = 1.15f, shape = CircleShape,
    onClick = { onToggleWatch() })
                    .background(checkBgColor, CircleShape)
                    .border(
                        1.dp,
                        if (isWatched) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
                    ,
                contentAlignment = Alignment.Center
            ) {
                Icon(checkIcon, contentDescription = null, tint = checkIconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка: Оценить серию
            var isRatingPopupExpanded by remember { mutableStateOf(false) }
            val starColor = if (userRating != null) Color(0xFFFFC107) else Color.White.copy(alpha = 0.2f)

            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = RoundedCornerShape(8.dp),
    onClick = { isRatingPopupExpanded = true })
                        .clip(RoundedCornerShape(8.dp))
                        
                        .padding(4.dp)
                ) {
                    Icon(Lucide.Star, contentDescription = null, tint = starColor, modifier = Modifier.size(20.dp))
                    if (userRating != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userRating.toString(),
                            color = Color(0xFFFFC107),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Вызов нашего нового попапа
                if (isRatingPopupExpanded) {
                    EpisodeRatingPopup(
                        currentRating = userRating,
                        onDismiss = { isRatingPopupExpanded = false },
                        onRate = {
                            onRate(it)
                            isRatingPopupExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // === 2. ПРАВАЯ ЧАСТЬ (КАРТОЧКА СЕРИИ) ===
        var isActive by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .weight(1f)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.02f, // Мягкий скейл
                    activeBorderWidth = 1.dp,
                    activeBorderColor = Color.White.copy(alpha = 0.2f),
                    defaultBorderWidth = 1.dp,
                    defaultBorderColor = Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    onStateChange = { isActive = it }
                ,
    onClick = { onClick() })
                .background(
                    if (isActive) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                
                .padding(12.dp), // Внутренний воздух увеличен! Теперь текст не будет прилипать к краям.
            verticalAlignment = Alignment.Top
        ) {
            // Картинка серии
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                if (!episode.stillUrl.isNullOrEmpty() && episode.stillUrl != "placeholder") {
                    ShimmerImage(
                        model = episode.stillUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Lucide.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(visible = isActive, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Информация о серии
            Column(modifier = Modifier.weight(1f)) {
                val titleColor by animateColorAsState(if (isActive) Color.White else Color.White.copy(alpha = 0.9f))
                val descColor by animateColorAsState(
                    if (isActive) Color.White.copy(alpha = 0.8f) else Color.White.copy(
                        alpha = 0.6f
                    )
                )

                // 1. ТОЛЬКО НАЗВАНИЕ
                Text(
                    text = "${episode.episodeNumber}. ${episode.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. МЕТАДАННЫЕ: Дата • Время • Рейтинг
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Дата
                    if (!episode.airDate.isNullOrEmpty()) {
                        Text(
                            text = episode.airDate.toString(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Время
                    if (episode.runtime != null && episode.runtime?.let { it > 0 } == true) {
                        if (!episode.airDate.isNullOrEmpty()) {
                            Text(
                                text = " • ",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        Text(text = stringResource(Res.string.player_duration_mins_single_fmt, episode.runtime ?: 0), fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }

                    // Рейтинг (ПЕРЕНЕСЕН СЮДА)
                    val rating = episode.voteAverage
                    if (rating != null && rating > 0.0) {
                        val formattedRating = ((rating * 10).toInt() / 10.0).toString()

                        // Добавляем разделитель " • ", если перед рейтингом есть дата или время
                        if (!episode.airDate.isNullOrEmpty() || (episode.runtime != null && episode.runtime?.let { it > 0 } == true)) {
                            Text(
                                text = " • ",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        Icon(
                            Lucide.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedRating,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. ОПИСАНИЕ
                val overviewText = episode.overview.takeIf { !it.isNullOrEmpty() } ?: stringResource(Res.string.details_seasons_no_desc)
                Text(
                    text = overviewText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = descColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MarkSeasonAsWatchedButton(isLoading: Boolean, component: SlotData.TvSeasons, onAction: (Action) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Заголовок слева
        // Кнопка справа (Показываем только если загрузилось)
        if (!isLoading && component.seasons.isNotEmpty()) {
            val activeSeason = component.seasons.find { it.seasonNumber == component.selectedSeasonNumber }

            // TODO: В будущем здесь можно проверять статус сезона (например: activeSeason.isWatched).
            // Если просмотрен - менять текст на "Снять отметку" и убирать зеленый цвет.
            val isSeasonWatched = activeSeason?.isFullyWatched ?: false

            val buttonBgColor =
                if (isSeasonWatched) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
            val buttonIconColor = if (isSeasonWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f)
            val buttonTextColor = if (isSeasonWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.8f)
            val borderColor =
                if (isSeasonWatched) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)

            Row(
                modifier = Modifier
                    .tvAndWebHoverEffect(scaleTarget = 1.05f, shape = RoundedCornerShape(8.dp),
    onClick = {
                        val markAction =
                            component.seasons.find { it.seasonNumber == component.selectedSeasonNumber }?.markWatchedAction
                        if (markAction != null) {
                            onAction(markAction)
                        }
                    })
                    .clip(RoundedCornerShape(8.dp))
                    .background(buttonBgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.CheckCheck,
                    contentDescription = stringResource(Res.string.details_seasons_mark_season),
                    tint = buttonIconColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSeasonWatched) stringResource(Res.string.player_watched) else stringResource(Res.string.details_seasons_mark_season),
                    color = buttonTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SeasonTab(
    season: SeasonItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.05f)
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f)
    )

    Box(
        modifier = Modifier
            .tvAndWebHoverEffect(scaleTarget = 1.05f, shape = RoundedCornerShape(8.dp),
    onClick = { onClick() })
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (season.isFullyWatched) {
                Icon(
                    imageVector = Lucide.CheckCheck,
                    contentDescription = stringResource(Res.string.details_seasons_completed),
                    tint = Color(0xFF4CAF50), // Green for fully watched
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (season.isWatching) {
                Icon(
                    imageVector = Lucide.Eye,
                    contentDescription = stringResource(Res.string.details_seasons_watching),
                    tint = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = season.name,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (season.episodeCount > 0 && !isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${season.episodeCount}",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SeasonTabSkeleton() {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(40.dp)
            .shimmerPlaceholder(true, RoundedCornerShape(8.dp))
    )
}


@Composable
private fun EpisodeListItemSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // === ЛЕВАЯ КОЛОНКА (Чеклист: Статус и Оценка) ===
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(56.dp)
        ) {
            // Скелетон круглой кнопки (Отметить просмотренным)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shimmerPlaceholder(true, CircleShape)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Скелетон иконки оценки (Звездочка)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // === ПРАВАЯ ЧАСТЬ (Карточка серии) ===
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp), // Внутренний отступ, как в реальной карточке
            verticalAlignment = Alignment.Top
        ) {
            // Картинка 16:9
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(16f / 9f)
                    .shimmerPlaceholder(true, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Текстовый блок
            Column(modifier = Modifier.weight(1f)) {
                // Заголовок (длинная строка)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(18.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Метаданные (короткая строка)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Описание (3 строки)
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).shimmerPlaceholder(true, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).shimmerPlaceholder(true, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(0.8f).height(12.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}