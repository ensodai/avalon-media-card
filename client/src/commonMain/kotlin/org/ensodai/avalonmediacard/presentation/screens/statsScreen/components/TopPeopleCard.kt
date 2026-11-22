package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonSurface as Surface
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.User
import com.composables.icons.lucide.Users
import org.ensodai.avalonmediacard.contract.model.PersonStatDto
import org.ensodai.avalonmediacard.contract.model.TopPeopleStatsDto
import org.ensodai.avalonmediacard.contract.utils.toProxyImageUrl
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopPeopleCard(
    topPeople: TopPeopleStatsDto,
    onPersonClicked: (personId: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Режиссеры, 1: Актеры

    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 260.dp),
        shape = shapes.container,
        color = colors.bgSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            val isDualColumn = maxWidth >= 680.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                // Заголовок категории и переключатель
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.Users,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.stats_people_title),
                            style = AvalonTheme.typography.sectionTitle,
                            color = colors.textPrimary
                        )
                    }

                    // Табы показываем только если экран узкий и не помещаются 2 колонки
                    if (!isDualColumn) {
                        Surface(
                            shape = shapes.control,
                            color = colors.bgElevated
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                TabPill(
                                    text = stringResource(Res.string.stats_people_directors),
                                    isSelected = selectedTab == 0,
                                    onClick = { selectedTab = 0 }
                                )
                                TabPill(
                                    text = stringResource(Res.string.stats_people_actors),
                                    isSelected = selectedTab == 1,
                                    onClick = { selectedTab = 1 }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isDualColumn) {
                    // Широкий экран: Режиссеры слева, Актеры справа
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader(stringResource(Res.string.stats_people_directors))
                            Spacer(modifier = Modifier.height(12.dp))
                            PeopleList(
                                people = topPeople.directors,
                                onPersonClicked = onPersonClicked,
                                isDirector = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader(stringResource(Res.string.stats_people_actors))
                            Spacer(modifier = Modifier.height(12.dp))
                            PeopleList(
                                people = topPeople.actors,
                                onPersonClicked = onPersonClicked,
                                isDirector = false
                            )
                        }
                    }
                } else {
                    // Узкий экран: список по выбранному табу
                    val people = if (selectedTab == 0) topPeople.directors else topPeople.actors
                    PeopleList(
                        people = people,
                        onPersonClicked = onPersonClicked,
                        isDirector = selectedTab == 0
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AvalonTheme.typography.uiLabel,
        color = AvalonTheme.colors.textSecondary
    )
}

@Composable
private fun TabPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Box(
        modifier = Modifier
            .clip(shapes.control)
            .background(if (isSelected) colors.bgSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = AvalonTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.textPrimary else colors.textSecondary
        )
    }
}

@Composable
private fun PeopleList(
    people: List<PersonStatDto>,
    onPersonClicked: (personId: String, name: String) -> Unit,
    isDirector: Boolean
) {
    val colors = AvalonTheme.colors

    if (people.isEmpty()) {
        val emptyRes = if (isDirector) Res.string.stats_people_empty_directors else Res.string.stats_people_empty_actors
        Text(
            text = stringResource(emptyRes),
            style = AvalonTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            people.take(5).forEachIndexed { index, person ->
                PersonRow(
                    rank = index + 1,
                    person = person,
                    isDirector = isDirector,
                    onClick = { onPersonClicked(person.personId, person.name) }
                )
            }
        }
    }
}

@Composable
private fun PersonRow(
    rank: Int,
    person: PersonStatDto,
    isDirector: Boolean,
    onClick: () -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    var isHovered by remember { mutableStateOf(false) }

    val animatedBg by animateColorAsState(
        if (isHovered) colors.borderSubtle.copy(alpha = 0.5f) else colors.bgElevated
    )
    val animatedBorder by animateColorAsState(
        if (isHovered) colors.borderStrong else Color.Transparent
    )

    Surface(
        onClick = onClick,
        shape = shapes.control,
        color = animatedBg,
        border = BorderStroke(1.dp, animatedBorder),
        modifier = Modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ранг по Тафти
            Text(
                text = "$rank",
                style = AvalonTheme.typography.labelMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Bold,
                color = if (rank == 1) colors.textPrimary else colors.textSecondary,
                modifier = Modifier.width(18.dp)
            )

            // Аватар персоны 1:1 — строго CircleShape по DESIGN.md через безопасный прокси
            val photoUrl = person.photoUrl?.toProxyImageUrl("w185") ?: person.photoUrl
            if (!photoUrl.isNullOrBlank()) {
                ShimmerImage(
                    model = photoUrl,
                    contentDescription = person.name,
                    contentScale = ContentScale.Crop,
                    shape = CircleShape,
                    errorIcon = Lucide.User,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.chartTrack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = person.name.firstOrNull()?.uppercase() ?: "?",
                        style = AvalonTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Имя персоны и количество тайтлов с грамотной русской грамматикой
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = AvalonTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = formatWatchedWorks(person.watchedCount),
                    style = AvalonTheme.typography.labelSmall.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    color = colors.textSecondary
                )
            }

            // Средний балл (золото accentGold)
            val avg = person.averageRating
            if (avg != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = null,
                        tint = colors.accentGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ((avg * 10).toInt() / 10.0).toString(),
                        style = AvalonTheme.typography.labelSmall.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = colors.accentGold
                    )
                }
            }
        }
    }
}

private fun formatWatchedWorks(count: Int): String {
    val rem10 = count % 10
    val rem100 = count % 100
    val word = when {
        rem100 in 11..19 -> "работ"
        rem10 == 1 -> "работа"
        rem10 in 2..4 -> "работы"
        else -> "работ"
    }
    return "$count $word просмотрено"
}
