package org.ensodai.avalonmediacard.presentation.screens.mediaScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.MovieCarouselItem
import org.ensodai.avalonmediacard.presentation.components.MediaGridCard
import org.ensodai.avalonmediacard.presentation.screens.mediaScreen.viewState.MediaListViewState

@Composable
fun MediaListContent(
    title: String,
    state: MediaListViewState,
    onAction: (Action) -> Unit
) {
    val grids = state.grids.mapNotNull { it.state.data }
    val gridState = rememberLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    var hasRequestedFocus by remember { mutableStateOf(false) }

    LaunchedEffect(grids) {
        if (grids.any { it.items.isNotEmpty() } && !hasRequestedFocus) {
            hasRequestedFocus = true
            runCatching { firstItemFocusRequester.requestFocus() }
        }
    }

    val isLoading = state.grids.isEmpty() || state.grids.any { it.state.isInitialLoading || it.state.isLoading }

    // Pagination logic
    val loadMoreAction = grids.firstNotNullOfOrNull { if (isLoading) null else it.loadMoreAction }
    if (loadMoreAction != null) {
        val triggeredActions = remember { mutableSetOf<String>() }
        LaunchedEffect(gridState, loadMoreAction) {
            snapshotFlow { gridState.layoutInfo }
                .collect { layoutInfo ->
                    val totalItems = layoutInfo.totalItemsCount
                    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

                    if (totalItems > 0 && lastVisibleIndex >= totalItems - 12) {
                        val actionKey = "${loadMoreAction}_$totalItems"
                        if (triggeredActions.add(actionKey)) {
                            onAction(loadMoreAction)
                        }
                    }
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            state = gridState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Заголовок экрана (на всю ширину)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
            }

            // Данные
            if (isLoading && grids.isEmpty()) {
                items(12) {
                    MediaGridCard(
                        item = MovieCarouselItem(
                            key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "mock"),
                            title = ""
                        ),
                        isLoading = true,
                        onAction = {}
                    )
                }
            } else {
                grids.forEachIndexed { gridIndex, grid ->
                    itemsIndexed(
                        items = grid.items,
                        key = { index, item -> "${item.key.type.name.lowercase()}:${item.key.id}_${grid.id}_$index" }
                    ) { index, item ->
                        MediaGridCard(
                            item = item,
                            isLoading = false,
                            onAction = onAction,
                            modifier = if (gridIndex == 0 && index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                        )
                    }
                }
            }
        }
    }
}
