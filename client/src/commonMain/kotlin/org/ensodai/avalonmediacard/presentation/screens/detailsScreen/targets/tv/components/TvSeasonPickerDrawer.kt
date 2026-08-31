package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SeasonItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TvSeasonPickerDrawer(
    isOpen: Boolean,
    seasons: List<SeasonItem>,
    selectedSeasonNumber: Int,
    onDismiss: () -> Unit,
    onAction: (Action) -> Unit
) {
    if (!isOpen || seasons.isEmpty()) return

    TvDrawerEffect(
        title = stringResource(Res.string.details_seasons_select_season_title),
        icon = Lucide.Film,
        onDismiss = onDismiss
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(
                items = seasons,
                key = { it.id.ifBlank { "season_${it.seasonNumber}" } }
            ) { season ->
                val isSelected = season.seasonNumber == selectedSeasonNumber
                val epCountText = stringResource(getPluralEpisodeRes(season.episodeCount), season.episodeCount)
                AvalonTvDrawerItem(
                    title = getSeasonDisplayName(season),
                    subtitle = epCountText,
                    isSelected = isSelected,
                    icon = if (isSelected) Lucide.Check else null,
                    onClick = {
                        season.selectAction?.let(onAction)
                        onDismiss()
                    }
                )
            }
        }
    }
}
