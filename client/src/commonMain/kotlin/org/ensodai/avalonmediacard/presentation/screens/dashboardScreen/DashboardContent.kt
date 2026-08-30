package org.ensodai.avalonmediacard.presentation.screens.dashboardScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.components.MovieCarousel
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.component.BackdropsCarouselWidget
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.component.ExplorationWidget
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.component.HeroBannerWidget
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.viewState.DashboardViewState
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.viewState.FeedItem
import org.ensodai.avalonmediacard.presentation.telemetry.TrackScrollDepth

@Composable
fun DashboardContent(
    title: String,
    state: DashboardViewState,
    onAction: (Action) -> Unit
) {
    val listState = rememberLazyListState()
    val listFocusRequester = remember { FocusRequester() }

    TrackScrollDepth(lazyListState = listState, context = ClickstreamContext.HOME_PAGE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 40.dp)
        ) {
            items(
                count = state.feedItems.size,
                key = { index -> "${state.feedItems[index].nodeId}_$index" }
            ) { index ->
                val item = state.feedItems[index]

                Box(modifier = Modifier
                    .padding(bottom = 32.dp)
                    .then(if (index == 0) Modifier.focusRequester(listFocusRequester) else Modifier)
                    .focusGroup()
                ) {
                    if (index == 0) {
                        LaunchedEffect(Unit) {
                            runCatching { listFocusRequester.requestFocus() }
                        }
                    }

                    when (item) {
                        is FeedItem.HeroBanner -> {
                            HeroBannerWidget(
                                state = item.state,
                                scrollOffsetProvider = {
                                    if (listState.firstVisibleItemIndex == index) {
                                        listState.firstVisibleItemScrollOffset.toFloat()
                                    } else 0f
                                },
                                onAction = onAction,
                                modifier = Modifier
                            )
                        }

                        is FeedItem.Backdrops -> {
                            BackdropsCarouselWidget(
                                state = item.state,
                                onAction = onAction,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        is FeedItem.Exploration -> {
                            ExplorationWidget(
                                state = item.state,
                                onAction = onAction,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        is FeedItem.Carousel -> {
                            MovieCarousel(
                                state = item.state,
                                onAction = onAction,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
