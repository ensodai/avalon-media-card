package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    showEpisodes: Boolean,
    seasonEpisodes: Map<Int, List<MediaStream>> = emptyMap(),
    currentStreamId: String = "",
    url: String? = null,
    currentEpisode: MediaStream? = null,
    isLoadingEpisodes: Boolean = false,
    listState: LazyListState,
    tabState: LazyListState,
    onEpisodeClick: ((MediaStream) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 24.dp, bottom = 110.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Панель списка серий
        AnimatedVisibility(
            visible = showEpisodes,
            enter = fadeIn(),
            exit = fadeOut()
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
}

