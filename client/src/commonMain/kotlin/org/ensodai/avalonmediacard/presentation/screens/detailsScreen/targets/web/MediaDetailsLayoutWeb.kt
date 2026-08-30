package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.carouselsSlot.CarouselsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.commentsSlot.CommentsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.continueWatchingSlot.ContinueWatchingSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot.MediaSourcesSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playerSlot.PlayerSectionSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components.WebCastSection
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components.WebHeroSection
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components.WebTvSeasonsSection
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.viewState.DetailsViewState

@Composable
fun MediaDetailsLayoutWeb(
    state: DetailsViewState,
    onAction: (Action) -> Unit,
    onClosePlayer: (() -> Unit)? = null,
    onRequestOtherSource: (() -> Unit)? = null,
    onCloseSources: (() -> Unit)? = null,
    onSelectSource: ((providerId: String, sourceId: String, seasonNumber: Int?, episodeNumber: Int?, onComplete: () -> Unit) -> Unit)? = null,
    onRefreshSources: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isPlayerOpen = state.playerState !is DetailsViewState.PlayerState.Idle
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenHeight = maxHeight
        val heroHeight = (screenHeight * 0.88f).coerceIn(720.dp, 920.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 1. Full-bleed 100vw Hero Section (Cineby style) with Parallax
            WebHeroSection(
                headerData = state.header?.state?.data,
                descriptionData = state.description?.state?.data,
                playButtons = state.playButtons?.state?.data,
                collectionButtons = state.collectionButtons?.state?.data,
                userActions = state.userActions?.state?.data,
                isHeaderLoading = state.header?.state?.isLoading == true,
                isDescriptionLoading = state.description?.state?.isLoading == true,
                isPlayButtonsLoading = state.playButtons?.state?.isLoading == true,
                onAction = onAction,
                scrollOffset = scrollState.value,
                heroHeight = heroHeight,
                onRequestOtherSource = onRequestOtherSource
            )

            // 2. Centered Content Body (max-width: 1320dp)
            Column(
                modifier = Modifier
                    .widthIn(max = 1320.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Continue Watching Card
                ContinueWatchingSlot(
                    state = state.continueWatching?.state,
                    onAction = onAction
                )

                // Media Sources & Torrent Inspector (if expanded)
                MediaSourcesSlot(
                    isExpanded = state.isSourcesExpanded,
                    mediaSourcesList = state.mediaSourcesList,
                    torrentInspectorState = state.torrentInspector?.state,
                    onClose = { onCloseSources?.invoke() },
                    onSelectSource = onSelectSource,
                    onRefreshSources = onRefreshSources,
                    onAction = onAction
                )

                // TV Seasons & Episodes (if show)
                WebTvSeasonsSection(
                    state = state.tvSeasons?.state,
                    onAction = onAction
                )

                // Cast Section
                WebCastSection(
                    castData = state.cast?.state?.data,
                    isLoading = state.cast?.state?.isLoading == true,
                    onAction = onAction
                )

                // Comments Section
                CommentsSlot(
                    state = state.comments?.state,
                    onAction = onAction
                )

                // Recommendations & Similar Carousels
                CarouselsSlot(
                    state = state.carousels,
                    onAction = onAction
                )
            }
        }

        // 3. Player Section Overlay (if playing)
        if (isPlayerOpen) {
            PlayerSectionSlot(
                mediaKey = state.mediaKey,
                seriesTitle = state.header?.state?.data?.title ?: "",
                playerState = state.playerState,
                onClose = { onClosePlayer?.invoke() },
                onRequestOtherSource = onRequestOtherSource,
                onAction = onAction
            )
        }
    }
}
