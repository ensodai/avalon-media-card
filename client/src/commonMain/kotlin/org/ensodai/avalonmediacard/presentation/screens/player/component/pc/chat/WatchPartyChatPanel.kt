package org.ensodai.avalonmediacard.presentation.screens.player.component.pc.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquare
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerChatActions
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.WatchPartyChatViewState
import org.jetbrains.compose.resources.stringResource

/**
 * Внутриплеерная панель чата совместного просмотра (левый оверлей).
 * Имеет единый со списком участников дизайн: сворачивается и разворачивается по клику на шапку.
 */
@Composable
fun WatchPartyChatPanel(
    chatState: WatchPartyChatViewState,
    actions: PlayerChatActions,
    episodeTitle: String?,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isExpanded = chatState.isVisible

    LaunchedEffect(chatState.messages.size, isExpanded) {
        if (isExpanded && chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    val panelShape = if (isExpanded) RoundedCornerShape(20.dp) else RoundedCornerShape(22.dp)
    val panelBg = if (isExpanded) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.65f)
    val panelBorderColor = if (isExpanded) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.08f)

    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = 320.dp)
            .animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
            .clip(panelShape)
            .background(panelBg)
            .border(1.dp, panelBorderColor, panelShape)
    ) {
        val maxAvailableHeight = maxHeight
        // Динамический потолок для высоты сообщений: занимает всю доступную высоту до нижнего бара плеера
        val maxListHeight = (maxAvailableHeight - 110.dp).coerceAtLeast(80.dp)

        Column(
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            val headerShape = if (isExpanded) {
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            } else {
                RoundedCornerShape(22.dp)
            }

            // Кликабельная шапка: в свернутом виде компактная таблетка [💬 63], в развернутом - полная шапка
            Row(
                modifier = Modifier
                    .then(
                        if (isExpanded) Modifier.width(320.dp) else Modifier.wrapContentWidth()
                    )
                    .tvAndWebHoverEffect(
                        scaleTarget = if (isExpanded) 1.0f else 1.08f,
                        shape = headerShape,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        activeBorderWidth = if (isExpanded) 1.5.dp else 2.dp,
                        defaultBorderColor = Color.Transparent,
                        defaultBorderWidth = 0.dp,
                        onClick = { actions.onToggleChatVisibility() }
                    )
                    .pointerHoverIcon(PointerIcon.Hand, overrideDescendants = true)
                    .padding(
                        horizontal = 14.dp,
                        vertical = if (isExpanded) 10.dp else 9.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = if (isExpanded) Modifier.weight(1f, fill = false) else Modifier
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64B5F6).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.MessageSquare,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (isExpanded) {
                        Text(
                            text = stringResource(Res.string.watch_party_chat_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // Бейдж счетчика сообщений: если есть непрочитанные - красная плашка, иначе общее число
                    if (chatState.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE53935).copy(alpha = 0.30f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "+${chatState.unreadCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A80)
                            )
                        }
                    } else if (chatState.messages.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = chatState.messages.size.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // В развернутом виде показываем шеврон сворачивания
                if (isExpanded) {
                    Icon(
                        imageVector = Lucide.ChevronUp,
                        contentDescription = stringResource(Res.string.common_collapse),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.width(320.dp)) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        thickness = 1.dp
                    )

                    if (!episodeTitle.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = episodeTitle,
                                color = Color.White.copy(alpha = 0.40f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Список сообщений: растет динамически по мере наполнения сообщениями до maxListHeight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxListHeight)
                            .padding(horizontal = 12.dp)
                    ) {
                        if (chatState.messages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.MessageSquare,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.20f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.watch_party_chat_empty),
                                        color = Color.White.copy(alpha = 0.40f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(vertical = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = maxListHeight)
                            ) {
                                items(chatState.messages, key = { it.id.toString() }) { message ->
                                    WatchPartyChatMessageItem(
                                        message = message,
                                        onTimecodeClicked = actions.onTimecodeClicked
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    // Поле ввода сообщения
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        WatchPartyChatInput(
                            text = chatState.inputText,
                            onTextChanged = actions.onInputTextChanged,
                            onInputFocusChanged = actions.onInputFocusChanged,
                            onSendMessage = actions.onSendMessage
                        )
                    }
                }
            }
        }
    }
}
