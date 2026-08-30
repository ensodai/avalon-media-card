package org.ensodai.avalonmediacard.presentation.components.mediaGridSlot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.MovieCarouselItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.MediaGridCard
import org.ensodai.avalonmediacard.presentation.core.SlotUiState

@Composable
fun MediaGridSlot(
    state: SlotUiState<SlotData.Grid>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    expectedItemsCount: Int? = null
) {
    if (state.hasError && state.error != null) {
        org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard(
            message = state.error,
            retryAction = state.retryAction,
            onAction = onAction,
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        return
    }
    
    val component = state.data ?: SlotData.Grid(id = "skeleton", title = "", items = emptyList())
    MediaGridSlotInternal(
        component = component,
        isLoading = state.isInitialLoading,
        onAction = onAction,
        modifier = modifier,
        expectedItemsCount = expectedItemsCount
    )
}

@Composable
private fun MediaGridSlotInternal(
    component: SlotData.Grid,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    expectedItemsCount: Int? = null
) {
    val gridState = rememberLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    var hasRequestedFocus by remember { mutableStateOf(false) }

    LaunchedEffect(component.items.isNotEmpty()) {
        if (component.items.isNotEmpty() && !hasRequestedFocus) {
            hasRequestedFocus = true
            runCatching { firstItemFocusRequester.requestFocus() }
        }
    }

    val loadMoreAction = if (isLoading) null else component.loadMoreAction
    if (loadMoreAction != null) {
        val triggeredActions = remember { mutableSetOf<String>() }
        LaunchedEffect(gridState, loadMoreAction) {
            androidx.compose.runtime.snapshotFlow { gridState.layoutInfo }
                .collect { layoutInfo ->
                    val totalItems = layoutInfo.totalItemsCount
                    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (totalItems > 0 && lastVisibleIndex >= totalItems - 12) {
                        val actionKey = "${loadMoreAction.toString()}_$totalItems"
                        if (triggeredActions.add(actionKey)) {
                            onAction(loadMoreAction)
                        }
                    }
                }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 16.dp,
            bottom = 16.dp
        ),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading && component.items.isEmpty()) {
            val count = expectedItemsCount ?: 12
            items(count) {
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
            itemsIndexed(
                items = component.items,
                key = { index, item -> "${item.key.type.name.lowercase()}:${item.key.id}_${component.id}_$index" }
            ) { index, item ->
                MediaGridCard(
                    item = item,
                    isLoading = false,
                    index = index,
                    onAction = onAction,
                    modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                )
            }
        }
    }
}
