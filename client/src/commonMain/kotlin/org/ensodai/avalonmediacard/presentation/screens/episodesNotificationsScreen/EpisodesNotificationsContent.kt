package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AdaptiveLayout
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.EpisodesNotificationsLayoutTv
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.web.EpisodesNotificationsLayoutWeb
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodeFilter
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodesNotificationsViewState

@Composable
fun EpisodesNotificationsContent(
    state: EpisodesNotificationsViewState,
    onAction: (Action) -> Unit,
    onFilterSelected: (EpisodeFilter) -> Unit,
    expectedItemsCount: Int? = null,
    modifier: Modifier = Modifier
) {
    AdaptiveLayout(
        tv = {
            EpisodesNotificationsLayoutTv(
                state = state,
                onAction = onAction,
                onFilterSelected = onFilterSelected,
                expectedItemsCount = expectedItemsCount,
                modifier = modifier
            )
        },
        default = {
            EpisodesNotificationsLayoutWeb(
                state = state,
                onAction = onAction,
                onFilterSelected = onFilterSelected,
                expectedItemsCount = expectedItemsCount,
                modifier = modifier
            )
        }
    )
}
