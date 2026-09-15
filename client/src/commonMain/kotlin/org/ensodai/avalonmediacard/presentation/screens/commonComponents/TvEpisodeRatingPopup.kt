package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.presentation.overlay.TvModalSurface
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvEpisodeRatingPopup(
    currentRating: Int?,
    maxRating: Int = 10,
    callerFocusRequester: FocusRequester? = null,
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit
) {
    val initialRating = currentRating ?: maxRating
    var focusedRating by remember { mutableStateOf(initialRating) }
    val focusRequesters = remember { List(maxRating) { FocusRequester() } }
    val indexToFocus = (initialRating - 1).coerceIn(0, maxRating - 1)

    TvModalSurface(
        isOpen = true,
        onDismissRequest = onDismiss,
        callerFocusRequester = callerFocusRequester,
        initialFocusRequester = focusRequesters[indexToFocus],
        scrimColor = Color.Black.copy(alpha = 0.85f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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

            // Ряд звезд с навигацией Влево/Вправо
            Row(
                modifier = Modifier
                    .focusProperties {
                        up = FocusRequester.Cancel
                        down = FocusRequester.Cancel
                    }
                    .focusRestorer(focusRequesters[indexToFocus])
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..maxRating) {
                    key(i) {
                        val starIndex = i - 1
                        val isActive = i <= focusedRating
                        val starColor by animateColorAsState(
                            if (isActive) Color(0xFFFFD700) else Color.White.copy(alpha = 0.15f)
                        )

                        Box(
                            modifier = Modifier
                                .focusRequester(focusRequesters[starIndex])
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.35f,
                                    activeBorderWidth = 0.dp,
                                    shape = CircleShape,
                                    tiltEnabled = false,
                                    onClick = { onRate(i) },
                                    onStateChange = { active -> if (active) focusedRating = i }
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = starColor,
                                modifier = Modifier.size(44.dp)
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
