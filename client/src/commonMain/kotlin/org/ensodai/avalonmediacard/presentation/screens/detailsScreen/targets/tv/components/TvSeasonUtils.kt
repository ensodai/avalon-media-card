package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import androidx.compose.runtime.Composable
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.slot.SeasonItem
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun getPluralSeasonRes(count: Int): StringResource {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> Res.string.details_seasons_count_many_fmt
        mod10 == 1 -> Res.string.details_seasons_count_single_fmt
        mod10 in 2..4 -> Res.string.details_seasons_count_few_fmt
        else -> Res.string.details_seasons_count_many_fmt
    }
}

internal fun getPluralEpisodeRes(count: Int): StringResource {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> Res.string.details_episodes_count_many_fmt
        mod10 == 1 -> Res.string.details_episodes_count_single_fmt
        mod10 in 2..4 -> Res.string.details_episodes_count_few_fmt
        else -> Res.string.details_episodes_count_many_fmt
    }
}

@Composable
internal fun getSeasonDisplayName(season: SeasonItem): String {
    return if (season.seasonNumber == 0) {
        stringResource(Res.string.details_seasons_specials)
    } else {
        stringResource(Res.string.player_season_fmt, season.seasonNumber)
    }
}
