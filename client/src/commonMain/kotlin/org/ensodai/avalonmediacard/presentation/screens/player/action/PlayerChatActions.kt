package org.ensodai.avalonmediacard.presentation.screens.player.action

/**
 * Набор действий пользовательского интерфейса чата комнаты совместного просмотра.
 */
data class PlayerChatActions(
    val onToggleChatVisibility: () -> Unit = {},
    val onInputTextChanged: (String) -> Unit = {},
    val onInputFocusChanged: (Boolean) -> Unit = {},
    val onSendMessage: () -> Unit = {},
    val onTimecodeClicked: (Long) -> Unit = {}
)
