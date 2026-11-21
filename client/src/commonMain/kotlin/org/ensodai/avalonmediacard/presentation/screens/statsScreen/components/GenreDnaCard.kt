package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lucide
import kotlin.math.roundToInt
import org.ensodai.avalonmediacard.contract.model.GenreStatDto
import org.ensodai.avalonmediacard.contract.model.UserMediaFilter
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.navigation.LocalNavigation
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun GenreDnaCard(
    genres: List<GenreStatDto>,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val navigation = LocalNavigation.current

    val genresTitle = stringResource(Res.string.stats_genres_title)

    val totalHits = if (genres.isNotEmpty()) {
        val first = genres.first()
        if (first.percentage > 0f) {
            val estimated = ((first.watchedCount * 100f) / first.percentage).roundToInt()
            maxOf(genres.sumOf { it.watchedCount }, estimated)
        } else {
            genres.sumOf { it.watchedCount }
        }
    } else {
        0
    }

    var hoveredGenreId by remember { mutableStateOf<Int?>(null) }

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 260.dp),
        shape = shapes.container,
        color = colors.bgSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            // Заголовок категории с отступом 24.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Flame,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = genresTitle,
                        style = AvalonTheme.typography.sectionTitle,
                        color = colors.textPrimary
                    )
                }

                if (totalHits > 0) {
                    Text(
                        text = stringResource(Res.string.stats_genres_total_fmt, totalHits),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (genres.isEmpty()) {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = stringResource(Res.string.stats_genres_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            } else {
                val topGenres = genres.take(7)
                val maxPercentage = topGenres.firstOrNull()?.percentage?.coerceAtLeast(1f) ?: 1f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    topGenres.forEachIndexed { index, genre ->
                        val isLeader = index == 0
                        val isHovered = hoveredGenreId == genre.genreId
                        val isAnyHovered = hoveredGenreId != null

                        val formattedName = genre.genreName.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                        val screenTitle = stringResource(
                            Res.string.stats_library_list_title_fmt,
                            genresTitle,
                            formattedName
                        )

                        GenreRow(
                            name = formattedName,
                            count = genre.watchedCount,
                            percentage = genre.percentage,
                            fraction = (genre.percentage / maxPercentage).coerceIn(0.05f, 1f),
                            isLeader = isLeader,
                            isHovered = isHovered,
                            isAnyHovered = isAnyHovered,
                            genreId = genre.genreId,
                            hoveredGenreId = hoveredGenreId,
                            onHoverChange = { id -> hoveredGenreId = id },
                            onClick = {
                                navigation.navigateTo(
                                    Screen.UserMediaList(
                                        filter = UserMediaFilter.ByGenre(genre.genreId),
                                        title = screenTitle
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreRow(
    name: String,
    count: Int,
    percentage: Float,
    fraction: Float,
    isLeader: Boolean,
    isHovered: Boolean,
    isAnyHovered: Boolean,
    genreId: Int,
    hoveredGenreId: Int?,
    onHoverChange: (Int?) -> Unit,
    onClick: () -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val currentHovered by rememberUpdatedState(hoveredGenreId)

    val animatedBg by animateColorAsState(
        if (isHovered) colors.bgElevated else Color.Transparent
    )
    val animatedNameColor by animateColorAsState(
        if (isHovered) colors.textPrimary
        else if (isAnyHovered) colors.textSecondary.copy(alpha = 0.5f)
        else if (isLeader) colors.textPrimary
        else colors.textSecondary
    )
    val animatedCountColor by animateColorAsState(
        if (isHovered) colors.textPrimary
        else if (isAnyHovered) colors.textSecondary.copy(alpha = 0.5f)
        else colors.textSecondary
    )
    val animatedPercentColor by animateColorAsState(
        if (isHovered) colors.chartBarPeak
        else if (isAnyHovered) colors.textSecondary.copy(alpha = 0.5f)
        else if (isLeader) colors.chartBarPeak
        else colors.textSecondary
    )
    val animatedTrackColor by animateColorAsState(
        if (isHovered) colors.borderStrong else colors.chartTrack
    )
    val animatedBarColor by animateColorAsState(
        if (isHovered) colors.chartBarPeak
        else if (isAnyHovered) colors.chartBarDefault.copy(alpha = 0.35f)
        else if (isLeader) colors.chartBarPeak
        else colors.chartBarDefault
    )
    val animatedBarHeight by animateDpAsState(
        if (isHovered) 6.dp else 5.dp
    )

    Surface(
        onClick = onClick,
        shape = shapes.control,
        color = animatedBg,
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(genreId) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> onHoverChange(genreId)
                            PointerEventType.Exit -> {
                                if (currentHovered == genreId) onHoverChange(null)
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isHovered || isLeader) FontWeight.SemiBold else FontWeight.Medium,
                    color = animatedNameColor
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.stats_genres_count_fmt, count),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = animatedCountColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${percentage.roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = animatedPercentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Полоса прогресса по Тафти: трек chartTrack, заливка: лидер/ховер - chartBarPeak, остальные - chartBarDefault
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedBarHeight)
                    .clip(shapes.micro)
                    .background(animatedTrackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(shapes.micro)
                        .background(animatedBarColor)
                )
            }
        }
    }
}
