package org.ensodai.avalonmediacard.presentation.screens.player.viewState

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * UI-модель сообщения чата для отображения в плеере совместного просмотра.
 */
data class PlayerChatUiMessage(
    val id: Uuid,
    val senderUserId: Uuid,
    val senderUsername: String,
    val senderAvatarUrl: String? = null,
    val text: String,
    val formattedPosition: String, // например, "04:52" или "01:23:45"
    val rawPositionMs: Long,
    val isFromMe: Boolean,
    val isHost: Boolean,
    val isSending: Boolean = false,
    val createdAt: Instant
)

/**
 * Состояние компонента внутриплеерного чата совместного просмотра.
 */
data class WatchPartyChatViewState(
    val isVisible: Boolean = true,
    val inputText: String = "",
    val isInputFocused: Boolean = false,
    val unreadCount: Int = 0,
    val messages: List<PlayerChatUiMessage> = emptyList()
)
