package org.ensodai.avalonmediacard.presentation.screens.player.component.reactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smile
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.player.model.WATCH_PARTY_EMOJIS

/**
 * Кнопка-таблетка со смайликом и выезжающей горизонтальной плашкой эмодзи-реакций.
 * Размещается в оверлее рядом с кнопкой чата в режиме совместного просмотра (Watch Party).
 */
@Composable
fun WatchPartyReactionsControl(
    onSendReaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else Color.Black.copy(alpha = 0.65f)
                )
                .border(
                    width = 1.dp,
                    color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.60f)
                    else Color.White.copy(alpha = 0.10f),
                    shape = CircleShape
                )
                .tvAndWebHoverEffect(
                    scaleTarget = 1.08f,
                    shape = CircleShape,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    activeBorderWidth = 1.5.dp,
                    defaultBorderColor = Color.Transparent,
                    defaultBorderWidth = 0.dp,
                    onClick = { isExpanded = !isExpanded }
                )
                .pointerHoverIcon(PointerIcon.Hand, overrideDescendants = true),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Smile,
                contentDescription = "Reactions",
                tint = if (isExpanded) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        WatchPartyReactionsBar(
            isVisible = isExpanded,
            onSendReaction = onSendReaction
        )
    }
}

/**
 * Всплывающая плашка быстрого выбора эмодзи-реакций (Glassmorphism Pill).
 *
 * Рассчитана на многократные быстрые нажатия (спам реакций) без закрытия меню.
 */
@Composable
fun WatchPartyReactionsBar(
    isVisible: Boolean,
    onSendReaction: (String) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    onDismissRequest: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .focusGroup()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.key == Key.Back || event.key == Key.Escape || event.key == Key.DirectionDown) {
                            onDismissRequest?.invoke()
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
        ) {
            WATCH_PARTY_EMOJIS.forEachIndexed { index, emoji ->
                val itemRequester = if (index == 0) firstItemFocusRequester else null
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .then(if (itemRequester != null) Modifier.focusRequester(itemRequester) else Modifier)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.30f,
                            shape = CircleShape,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                            activeBorderWidth = 2.dp,
                            onClick = { onSendReaction(emoji) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
