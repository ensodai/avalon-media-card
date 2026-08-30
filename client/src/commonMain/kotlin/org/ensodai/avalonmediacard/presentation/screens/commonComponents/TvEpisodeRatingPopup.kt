package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvEpisodeRatingPopup(
    currentRating: Int?,
    maxRating: Int = 10,
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit
) {
    // Popup на весь экран для перехвата фокуса и показа оверлея
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && (event.key == Key.Back || event.key == Key.Escape)) {
                        onDismiss()
                        true
                    } else false
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                var focusedRating by remember { mutableStateOf(currentRating ?: maxRating) }
                val focusRequesters = remember { List(maxRating) { FocusRequester() } }

                // При запуске переводим фокус на текущую оценку (или на максимум)
                LaunchedEffect(Unit) {
                    delay(50) // Небольшая задержка, чтобы Popup успел появиться
                    runCatching {
                        val indexToFocus = (focusedRating - 1).coerceIn(0, maxRating - 1)
                        focusRequesters[indexToFocus].requestFocus()
                    }
                }

                // Заголовок и текущий рейтинг
                Text(
                    text = stringResource(Res.string.details_rating_my_title),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "$focusedRating / $maxRating",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Ряд звезд
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..maxRating) {
                        key(i) {
                            val isActive = i <= focusedRating
                            var isItemFocused by remember { mutableStateOf(false) }

                            val starColor by animateColorAsState(
                                if (isActive) Color(0xFFFFD700) else Color.White.copy(alpha = 0.15f)
                            )
                            val scale by animateFloatAsState(if (isItemFocused) 1.5f else if (isActive) 1.1f else 0.9f)
                            val elevation by animateFloatAsState(if (isItemFocused) 8f else 0f)

                            Box(
                                modifier = Modifier
                                    .focusRequester(focusRequesters[i - 1])
                                    .onFocusChanged { state ->
                                        isItemFocused = state.isFocused
                                        if (state.isFocused) {
                                            focusedRating = i
                                        }
                                    }
                                    .focusable()
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)) {
                                            onRate(i)
                                            true
                                        } else false
                                    }
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        shadowElevation = elevation
                                        shape = RoundedCornerShape(50)
                                        clip = false
                                    }
                                    .background(
                                        if (isItemFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(50)
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Star,
                                    contentDescription = null,
                                    tint = starColor,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
                
                // Подсказка для пользователя
                Text(
                    text = stringResource(Res.string.details_rating_tv_hint),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }
        }
    }
}
