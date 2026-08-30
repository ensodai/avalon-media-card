package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.backdropImageSlot.BackdropImageSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.carouselsSlot.CarouselsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.castSlot.CastSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot.CollectionButtonsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.commentsSlot.CommentsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.continueWatchingSlot.ContinueWatchingSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.criticsRatingsSlot.CriticsRatingsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaDescriptionSlot.MediaDescriptionSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot.MediaSourcesSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.metadataSlot.MetadataSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playButtonsSlot.PlayButtonsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playerSlot.PlayerSectionSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.posterImageSlot.PosterImageSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.statusAndRatingSlot.StatusAndRatingSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.syncStatusSlot.SyncStatusSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.titleSlot.TitleSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.tvSeasonsSlot.TvSeasonsSlot
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.viewState.DetailsViewState

@Composable
fun MediaDetailsLayoutMobile(
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
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            // Mobile Hero Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                BackdropImageSlot(state = state.header?.state)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.4f to backgroundColor.copy(alpha = 0.3f),
                                    0.75f to backgroundColor.copy(alpha = 0.8f),
                                    1.0f to backgroundColor
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TitleSlot(state = state.header?.state)
                        MetadataSlot(state = state.header?.state, onAction = onAction)
                        CriticsRatingsSlot(state = state.header?.state)
                    }
                }
            }

            // Play & Collection buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayButtonsSlot(state = state.playButtons?.state, onAction = onAction)
                CollectionButtonsSlot(state = state.collectionButtons?.state, onAction = onAction)
            }

            // Continue Watching
            ContinueWatchingSlot(state = state.continueWatching?.state, onAction = onAction)

            // Sources
            MediaSourcesSlot(
                isExpanded = state.isSourcesExpanded,
                mediaSourcesList = state.mediaSourcesList,
                torrentInspectorState = state.torrentInspector?.state,
                onClose = { onCloseSources?.invoke() },
                onSelectSource = onSelectSource,
                onRefreshSources = onRefreshSources,
                onAction = onAction
            )

            // User Actions: Status & Rating
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusAndRatingSlot(state = state.userActions?.state, onAction = onAction)
                SyncStatusSlot(state = state.syncStatus?.state)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                MediaDescriptionSlot(state = state.description?.state)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cast
            CastSlot(state = state.cast?.state, onAction = onAction)

            Spacer(modifier = Modifier.height(16.dp))

            // TV Seasons
            TvSeasonsSlot(state = state.tvSeasons?.state, onAction = onAction)

            Spacer(modifier = Modifier.height(16.dp))

            // Comments
            CommentsSlot(state = state.comments?.state, onAction = onAction)

            Spacer(modifier = Modifier.height(16.dp))

            // Carousels
            CarouselsSlot(state = state.carousels, onAction = onAction)
        }

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
