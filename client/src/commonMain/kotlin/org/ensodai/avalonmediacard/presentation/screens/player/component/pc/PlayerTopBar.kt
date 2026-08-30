package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.EpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandActionButton
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandContainer
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandDynamicButton
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandIconButton
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandTitle
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayerTopBar(
    state: PlayerViewState,
    actions: PlayerActions,
    rightPadding: Dp,
    modifier: Modifier = Modifier
) {
    val titleData = state.displayTitleData
    val topText = titleData.topText
    val bottomText = titleData.bottomText
    val currentEpisode = state.currentEpisode

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 24.dp, end = rightPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Левая группа (Остров 1 и Остров 2)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            // Остров 1: Кнопка закрытия
            PlayerIslandIconButton(
                icon = Lucide.X,
                contentDescription = stringResource(Res.string.player_btn_close),
                onClick = { actions.onCloseClicked() }
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Остров 2: Заголовок
            PlayerIslandTitle(
                topText = topText,
                bottomText = bottomText
            )
        }

        // Остров 3 и 4: Правая часть (Действия)
        if (currentEpisode != null) {
            var showRatingPopup by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Островок 3: Статус просмотра
                PlayerIslandDynamicButton(
                    icon = if (currentEpisode.isWatched) Lucide.Check else Lucide.Eye,
                    text = if (currentEpisode.isWatched) stringResource(Res.string.player_watched) else null,
                    contentDescription = if (currentEpisode.isWatched) stringResource(Res.string.player_watched) else stringResource(Res.string.player_mark_watched),
                    iconTint = if (currentEpisode.isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.85f),
                    textColor = if (currentEpisode.isWatched) Color(0xFF4CAF50) else Color.White,
                    onClick = { actions.onToggleEpisodeWatched(currentEpisode) }
                )

                // Островок 4: Оценка
                Box {
                    PlayerIslandDynamicButton(
                        icon = Lucide.Star,
                        text = currentEpisode.userRating?.toString(),
                        contentDescription = if (currentEpisode.userRating != null) stringResource(Res.string.player_rating_val, currentEpisode.userRating.toString()) else stringResource(Res.string.player_rate),
                        iconTint = if (currentEpisode.userRating != null) Color(0xFFFFC107) else Color.White.copy(alpha = 0.85f),
                        textColor = if (currentEpisode.userRating != null) Color(0xFFFFC107) else Color.White,
                        onClick = { showRatingPopup = true }
                    )

                    if (showRatingPopup) {
                        val density = LocalDensity.current
                        EpisodeRatingPopup(
                            currentRating = currentEpisode.userRating ?: 0,
                            maxRating = 10,
                            alignment = Alignment.TopEnd,
                            offset = IntOffset(0, with(density) { 44.dp.roundToPx() }),
                            onDismiss = { showRatingPopup = false },
                            onRate = { newRating ->
                                actions.onRateEpisode(currentEpisode, newRating)
                                showRatingPopup = false
                            }
                        )
                    }
                }
            }
        }
    }
}
