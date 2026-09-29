package org.ensodai.avalonmediacard.presentation.screens.player.component.pc.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Send
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

/**
 * Строка ввода сообщения в чат совместного просмотра.
 */
@Composable
fun WatchPartyChatInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onInputFocusChanged: (Boolean) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                Text(
                    text = stringResource(Res.string.watch_party_chat_input_placeholder),
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 13.sp
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChanged,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 13.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        onInputFocusChanged(focusState.isFocused)
                    }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            if (keyEvent.key == Key.Enter) {
                                onSendMessage()
                                true
                            } else if (keyEvent.key == Key.Escape) {
                                focusManager.clearFocus()
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Кнопка отправки с TV/Web hover эффектом
        val canSend = text.isNotBlank()
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (canSend) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
                )
                .tvAndWebHoverEffect(
                    scaleTarget = 1.15f,
                    shape = CircleShape,
                    activeBorderColor = if (canSend) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.20f),
                    activeBorderWidth = 1.5.dp,
                    defaultBorderColor = Color.Transparent,
                    defaultBorderWidth = 0.dp,
                    clickEnabled = canSend,
                    onClick = { onSendMessage() }
                )
                .pointerHoverIcon(
                    if (canSend) PointerIcon.Hand else PointerIcon.Default,
                    overrideDescendants = true
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Send,
                contentDescription = stringResource(Res.string.watch_party_chat_send),
                tint = if (canSend) Color.Black else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
