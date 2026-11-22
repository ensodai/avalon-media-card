package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.designsystem.components.AvalonDialog
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvEpisodeRatingPopup(
    currentRating: Int?,
    maxRating: Int = 10,
    callerFocusRequester: FocusRequester? = null,
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit
) {
    val colors = AvalonTheme.colors
    val initialRating = currentRating ?: maxRating
    var focusedRating by remember { mutableStateOf(initialRating) }
    val focusRequesters = remember { List(maxRating) { FocusRequester() } }
    val indexToFocus = (initialRating - 1).coerceIn(0, maxRating - 1)

    AvalonDialog(
        isOpen = true,
        onDismissRequest = onDismiss,
        callerFocusRequester = callerFocusRequester,
        initialFocusRequester = focusRequesters[indexToFocus]
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            // Заголовок и текущий рейтинг
            AvalonText(
                text = stringResource(Res.string.details_rating_my_title),
                color = colors.textSecondary,
                style = AvalonTheme.typography.sectionTitle,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AvalonText(
                text = "$focusedRating / $maxRating",
                style = AvalonTheme.typography.kpiHero,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accentGold,
                modifier = Modifier.padding(bottom = 28.dp)
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
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..maxRating) {
                    key(i) {
                        val starIndex = i - 1
                        val isActive = i <= focusedRating
                        val starColor by animateColorAsState(
                            if (isActive) colors.accentGold else colors.textMuted.copy(alpha = 0.25f)
                        )

                        Box(
                            modifier = Modifier
                                .focusRequester(focusRequesters[starIndex])
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.3f,
                                    activeBorderWidth = 0.dp,
                                    shape = CircleShape,
                                    tiltEnabled = false,
                                    onClick = { onRate(i) },
                                    onStateChange = { active -> if (active) focusedRating = i }
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AvalonIcon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = starColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Подсказка для пользователя
            AvalonText(
                text = stringResource(Res.string.details_rating_tv_hint),
                color = colors.textMuted,
                style = AvalonTheme.typography.microCaption
            )
        }
    }
}
