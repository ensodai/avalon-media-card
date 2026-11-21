package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    onMoviesClicked: () -> Unit = {},
    onShowsClicked: () -> Unit = {},
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
                // 1. Уровень Hero (2x1 · 6 кол + 2x1 · 6 кол): Киноархетип + Экранное время со сплитом
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ArchetypeCard(
                        archetype = stats.archetype,
                        onShare = onShareArchetype,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    HeroTimeCard(
                        timeStats = stats.timeOverview,
                        onMoviesClicked = onMoviesClicked,
                        onShowsClicked = onShowsClicked,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // 2. Уровень медиатеки и вкуса: Библиотека / бэклог + ДНК жанров
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LibraryCountersCard(
                        libraryStats = stats.libraryOverview,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    GenreDnaCard(
                        genres = stats.genreBreakdown,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // 3. Аналитические шкалы: Кривая оценок (50%) + Эпохи (50%)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RatingDistributionCard(
                        ratingDistribution = stats.ratingDistribution,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    DecadesCard(
                        decades = stats.decadesBreakdown,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // 4. Годовой ритм: Полноширинная хронология (4x1 · 12 кол.) — Тепловая карта года (52 недели)
                ActivityHeatmapCard(
                    heatmap = stats.activityHeatmap,
                    modifier = Modifier.fillMaxWidth()
                )

                // 5. Зал славы: Лидерборд персон (Режиссеры слева, Актеры справа)
                TopPeopleCard(
                    topPeople = stats.topPeople,
                    onPersonClicked = onPersonClicked,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Мобильный адаптивный стек: последовательность перевернутой пирамиды
                ArchetypeCard(archetype = stats.archetype, onShare = onShareArchetype)
                HeroTimeCard(
                    timeStats = stats.timeOverview,
                    onMoviesClicked = onMoviesClicked,
                    onShowsClicked = onShowsClicked
                )
                LibraryCountersCard(libraryStats = stats.libraryOverview)
                GenreDnaCard(genres = stats.genreBreakdown)
                RatingDistributionCard(ratingDistribution = stats.ratingDistribution)
                DecadesCard(decades = stats.decadesBreakdown)
                ActivityHeatmapCard(heatmap = stats.activityHeatmap)
                TopPeopleCard(topPeople = stats.topPeople, onPersonClicked = onPersonClicked)
            }
        }
    }
}
