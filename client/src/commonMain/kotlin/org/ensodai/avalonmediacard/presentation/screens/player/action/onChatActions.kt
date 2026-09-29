package org.ensodai.avalonmediacard.presentation.screens.player.action

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerChatUiMessage
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Создание связки действий чата с методами PlayerViewModel.
 */
fun PlayerViewModel.createPlayerChatActions(): PlayerChatActions = PlayerChatActions(
    onToggleChatVisibility = { onToggleChatVisibility() },
    onInputTextChanged = { onChatInputTextChanged(it) },
    onInputFocusChanged = { onChatInputFocusChanged(it) },
    onSendMessage = { onSendChatMessage() },
    onTimecodeClicked = { onChatTimecodeClicked(it) }
)

fun PlayerViewModel.onToggleChatVisibility() {
    updateViewState { state ->
        val newVisible = !state.chatState.isVisible
        val newUnread = if (newVisible) 0 else state.chatState.unreadCount
        state.copy(
            chatState = state.chatState.copy(
                isVisible = newVisible,
                unreadCount = newUnread
            )
        )
    }
}

fun PlayerViewModel.onChatInputTextChanged(newText: String) {
    updateViewState { state ->
        state.copy(
            chatState = state.chatState.copy(inputText = newText)
        )
    }
}

fun PlayerViewModel.onChatInputFocusChanged(isFocused: Boolean) {
    updateViewState { state ->
        val newUnread = if (isFocused) 0 else state.chatState.unreadCount
        state.copy(
            chatState = state.chatState.copy(
                isInputFocused = isFocused,
                unreadCount = newUnread
            )
        )
    }
}

fun PlayerViewModel.onSendChatMessage() {
    val currentChatState = viewState.value.chatState
    val text = currentChatState.inputText.trim()
    if (text.isBlank()) return

    val roomId = viewState.value.watchRoomId ?: return
    val currentUid = viewState.value.currentUserId ?: ""
    val myUserUuid = runCatching { Uuid.parse(currentUid) }.getOrDefault(Uuid.NIL)
    val myUsername = viewState.value.watchRoomParticipants.find { it.userId.toString() == currentUid }?.username ?: ""

    val posMs = ((activeController?.state?.currentTime ?: viewState.value.currentTime) * 1000.0).toLong().coerceAtLeast(0L)
    val formattedPos = formatChatTimecode(posMs)

    val tempId = Uuid.random()
    val optimisticMessage = PlayerChatUiMessage(
        id = tempId,
        senderUserId = myUserUuid,
        senderUsername = myUsername,
        senderAvatarUrl = null,
        text = text,
        formattedPosition = formattedPos,
        rawPositionMs = posMs,
        isFromMe = true,
        isHost = viewState.value.isEffectiveHost,
        isSending = true,
        createdAt = Clock.System.now()
    )

    updateViewState { state ->
        state.copy(
            chatState = state.chatState.copy(
                inputText = "",
                messages = state.chatState.messages + optimisticMessage
            )
        )
    }

    viewModelScope.launch {
        try {
            val success = rpcService.sendChatMessage(
                roomId = roomId,
                text = text,
                playbackPositionMs = posMs
            )
            if (!success) {
                // Если сервер вернул false, помечаем неуспешной
                updateViewState { state ->
                    state.copy(
                        chatState = state.chatState.copy(
                            messages = state.chatState.messages.filter { it.id != tempId }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            updateViewState { state ->
                state.copy(
                    chatState = state.chatState.copy(
                        messages = state.chatState.messages.filter { it.id != tempId }
                    )
                )
            }
        }
    }
}

fun PlayerViewModel.onChatTimecodeClicked(rawPositionMs: Long) {
    val targetSeconds = (rawPositionMs / 1000.0).coerceAtLeast(0.0)
    onSeek(targetSeconds)
}

fun formatChatTimecode(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0) {
        val hStr = hours.toString()
        val mStr = if (minutes < 10) "0$minutes" else minutes.toString()
        val sStr = if (seconds < 10) "0$seconds" else seconds.toString()
        "$hStr:$mStr:$sStr"
    } else {
        val mStr = if (minutes < 10) "0$minutes" else minutes.toString()
        val sStr = if (seconds < 10) "0$seconds" else seconds.toString()
        "$mStr:$sStr"
    }
}
