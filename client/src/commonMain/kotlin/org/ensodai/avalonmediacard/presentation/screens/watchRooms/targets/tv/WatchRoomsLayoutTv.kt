package org.ensodai.avalonmediacard.presentation.screens.watchRooms.targets.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.watch_rooms_history_empty
import avalonmediacard.client.generated.resources.watch_rooms_history_title
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.action.WatchRoomsActions
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.components.WatchRoomSummaryCard
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.components.WatchRoomSummaryCardSkeleton
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.components.WatchRoomsHeader
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.viewState.WatchRoomsViewState
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/**
 * ТВ-лейаут экрана комнат совместного просмотра:
 * - Шапка скроллится вместе с контентом экрана в единой ленте
 * - 2-колоночная сетка под пропорции 16:9
 * - Мягкий BringIntoViewSpec:
 *   1. На первой линии комнат (и в шапке) зафиксирован minTop под высоту шапки (~26% экрана):
 *      шапка «Смотрим вместе» гарантированно видна на 100% и не выталкивается за экран!
 *   2. При навигации на нижние ряды экран плавно скроллится вниз ровно на размер выхода.
 *   3. При возврате стрелкой «Вверх» на 1-ю линию комнат экран плавно возвращается в нулевую позицию.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun WatchRoomsLayoutTv(
    state: WatchRoomsViewState,
    actions: WatchRoomsActions,
    onRoomClick: (WatchRoomSummaryDto) -> Unit,
    modifier: Modifier = Modifier,
    isPlayerOpen: Boolean = false,
    expectedItemsCount: Int? = null
) {
    val restoreFocusRequester = remember { FocusRequester() }
    val headerButtonFocusRequester = remember { FocusRequester() }
    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    var lastFocusedRoomId by remember { mutableStateOf<Uuid?>(null) }

    LaunchedEffect(state.rooms.size, isPlayerOpen) {
        if (!isPlayerOpen) {
            val success = runCatching {
                if (focusedIndex == -1 || state.rooms.isEmpty()) {
                    headerButtonFocusRequester.requestFocus()
                } else {
                    restoreFocusRequester.requestFocus()
                }
            }.isSuccess

            if (!success) {
                delay(50)
                runCatching {
                    if (focusedIndex == -1 || state.rooms.isEmpty()) {
                        headerButtonFocusRequester.requestFocus()
                    } else {
                        restoreFocusRequester.requestFocus()
                    }
                }
            }
        }
    }

    val tvBringIntoViewSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                // Если мы на первой линии комнат (индексы 0 и 1) или на шапке (-1 / null),
                // верхняя граница равна высоте зоны шапки (~26% экрана), чтобы «Смотрим вместе» оставалась полностью видимой
                val isFirstRow = (focusedIndex ?: 0) < 2
                val minTop = if (isFirstRow) containerSize * 0.26f else 8f
                val bottomThreshold = containerSize * 0.90f

                return when {
                    offset >= minTop && (offset + size) <= bottomThreshold -> 0f
                    offset < minTop -> offset - minTop
                    else -> (offset + size) - bottomThreshold
                }
            }
        }
    }

    val fallbackRequester = if (focusedIndex == -1 || state.rooms.isEmpty()) {
        headerButtonFocusRequester
    } else {
        restoreFocusRequester
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides tvBringIntoViewSpec) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier
                .fillMaxSize()
                .focusRestorer(fallbackRequester)
                .focusGroup(),
            contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ТВ-шапка с кнопкой "Войти по коду" (внутри единого скроллера контента)
            item(span = { GridItemSpan(maxLineSpan) }) {
                WatchRoomsHeader(
                    onJoinByPin = actions.onOpenConnectModal,
                    buttonFocusRequester = headerButtonFocusRequester,
                    titleFontSize = 32.sp,
                    subtitleFontSize = 16.sp,
                    modifier = Modifier.onFocusChanged { if (it.hasFocus) focusedIndex = -1 }
                )
            }

            // 2. Секция "Ваши комнаты"
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.watch_rooms_history_title),
                        fontSize = 22.sp,
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

            // 3. Список комнат (2-колоночная ТВ-сетка 16:9), скелетоны загрузки либо пустое состояние
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
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.watch_rooms_history_empty),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                else -> {
                    itemsIndexed(state.rooms, key = { _, room -> room.id.toString() }) { index, room ->
                        val isTargetForFocus = if (lastFocusedRoomId != null) {
                            room.id == lastFocusedRoomId
                        } else {
                            index == 0
                        }
                        WatchRoomSummaryCard(
                            room = room,
                            onClick = {
                                lastFocusedRoomId = room.id
                                focusedIndex = index
                                onRoomClick(room)
                            },
                            modifier = Modifier
                                .then(if (isTargetForFocus) Modifier.focusRequester(restoreFocusRequester) else Modifier)
                                .onFocusChanged {
                                    if (it.hasFocus) {
                                        focusedIndex = index
                                        lastFocusedRoomId = room.id
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
