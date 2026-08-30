package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.plugins.MediaStream

@Composable
fun BoxScope.PlayerRightPanelOverlay(
    visible: Boolean,
    seasonEpisodes: Map<Int, List<MediaStream>>,
    currentStreamId: String = "",
    url: String?,
    currentEpisode: MediaStream? = null,
    isLoadingEpisodes: Boolean,
    listState: LazyListState,
    tabState: LazyListState,
    onEpisodeClick: ((MediaStream) -> Unit)?
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(top = 24.dp, bottom = 110.dp, end = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
        ) {
            if (isLoadingEpisodes) {
                EpisodesShimmer()
            } else {
                EpisodeListPanel(
                    seasonEpisodes = seasonEpisodes,
                    currentStreamId = currentStreamId,
                    currentUrl = url,
                    currentEpisode = currentEpisode,
                    listState = listState,
                    tabState = tabState,
                    onEpisodeClick = onEpisodeClick
                )
            }
        }
    }
}
