package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.model.UserStatsDto

@Composable
fun StatsBentoGrid(
    stats: UserStatsDto,
    onShareArchetype: () -> Unit,
    onPersonClicked: (personId: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 840.dp

        Column(
            modifier = Modifier
                .widthIn(max = 1240.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isWide) {
                // 1. Hero-разворот: Экранное время (60%) + Киноархетип (40%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeroTimeCard(
                        timeStats = stats.timeOverview,
                        modifier = Modifier.weight(1.15f)
                    )
                    ArchetypeCard(
                        archetype = stats.archetype,
                        onShare = onShareArchetype,
                        modifier = Modifier.weight(0.85f)
                    )
                }

                // 2. Библиотека и бэклог: Панель счетчиков на всю ширину сетки
                LibraryCountersCard(
                    libraryStats = stats.libraryOverview,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Аналитика вкуса: Кривая оценок (50%) + ДНК жанров (50%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RatingDistributionCard(
                        ratingDistribution = stats.ratingDistribution,
                        modifier = Modifier.weight(1f)
                    )
                    GenreDnaCard(
                        genres = stats.genreBreakdown,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Люди и Эпохи: Лидерборд персон (50%) + Машина времени (50%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TopPeopleCard(
                        topPeople = stats.topPeople,
                        onPersonClicked = onPersonClicked,
                        modifier = Modifier.weight(1f)
                    )
                    DecadesCard(
                        decades = stats.decadesBreakdown,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Мобильный / узкий экран: Вертикальный адаптивный стек
                HeroTimeCard(timeStats = stats.timeOverview)
                ArchetypeCard(archetype = stats.archetype, onShare = onShareArchetype)
                LibraryCountersCard(libraryStats = stats.libraryOverview)
                RatingDistributionCard(ratingDistribution = stats.ratingDistribution)
                GenreDnaCard(genres = stats.genreBreakdown)
                TopPeopleCard(topPeople = stats.topPeople, onPersonClicked = onPersonClicked)
                DecadesCard(decades = stats.decadesBreakdown)
            }
        }
    }
}
