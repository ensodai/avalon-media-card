package org.ensodai.avalonmediacard.presentation.screens.watchRooms.targets.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.watch_rooms_history_empty
import avalonmediacard.client.generated.resources.watch_rooms_history_title
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.action.WatchRoomsActions
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.components.WatchRoomSummaryCard
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.components.WatchRoomSummaryCardSkeleton
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.components.WatchRoomsHeader
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.viewState.WatchRoomsViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchRoomsLayoutWeb(
    state: WatchRoomsViewState,
    actions: WatchRoomsActions,
    onRoomClick: (WatchRoomSummaryDto) -> Unit,
    modifier: Modifier = Modifier,
    expectedItemsCount: Int? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header with compact action toolbar (full width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                WatchRoomsHeader(
                    onJoinByPin = actions.onOpenConnectModal
                )
            }

            // 2. User Rooms Section Title (full width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.watch_rooms_history_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 3. User Rooms List, Skeletons or Empty State
            when {
                state.isLoading && state.rooms.isEmpty() -> {
                    val skeletonCount = (expectedItemsCount ?: 4).coerceAtLeast(1)
                    items(skeletonCount) {
                        WatchRoomSummaryCardSkeleton()
                    }
                }
                state.rooms.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.watch_rooms_history_empty),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                else -> {
                    items(state.rooms, key = { it.id.toString() }) { room ->
                        WatchRoomSummaryCard(
                            room = room,
                            onClick = { onRoomClick(room) }
                        )
                    }
                }
            }
        }
    }
}
