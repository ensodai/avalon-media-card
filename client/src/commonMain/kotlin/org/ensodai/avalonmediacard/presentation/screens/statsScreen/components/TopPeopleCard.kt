package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Users
import org.ensodai.avalonmediacard.contract.model.PersonStatDto
import org.ensodai.avalonmediacard.contract.model.TopPeopleStatsDto
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
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
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
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
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.8.sp
        ),
        fontWeight = FontWeight.Bold,
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
            style = MaterialTheme.typography.labelSmall,
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
            style = MaterialTheme.typography.bodySmall,
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

    Surface(
        onClick = onClick,
        shape = shapes.control,
        color = colors.bgElevated
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
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Bold,
                color = if (rank == 1) colors.textPrimary else colors.textSecondary,
                modifier = Modifier.width(18.dp)
            )

            // Аватар персоны 1:1 — строго CircleShape по DESIGN.md
            val photoUrl = person.photoUrl?.let { path ->
                if (path.startsWith("http")) path else "https://image.tmdb.org/t/p/w185$path"
            }
            if (photoUrl != null) {
                ShimmerImage(
                    model = photoUrl,
                    contentDescription = person.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
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
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Имя персоны и количество тайтлов
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = stringResource(Res.string.stats_people_watched_count_fmt, person.watchedCount),
                    style = MaterialTheme.typography.labelSmall.copy(
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
                        style = MaterialTheme.typography.labelSmall.copy(
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
