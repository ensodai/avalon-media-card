package org.ensodai.avalonmediacard.presentation.screens.player.component.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.RotateCw
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandIconButton
import org.jetbrains.compose.resources.stringResource

/**
 * Центральный блок кнопок управления воспроизведением (Play/Pause, Перемотка, Предыдущая/Следующая серия).
 */
@Composable
fun TvPlayerCenterControls(
    controller: PlaybackController,
    hasEpisodesContext: Boolean,
    prevEpisode: MediaStream?,
    nextEpisode: MediaStream?,
    onSelectEpisode: (MediaStream) -> Unit,
    playPauseFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val isPlaying = controller.state.isPlaying
    val currentTime = controller.state.currentTime
    val duration = controller.state.duration

    Row(
        modifier = modifier
            .focusGroup(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Предыдущая серия (только для сериалов)
        if (hasEpisodesContext) {
            PlayerIslandIconButton(
                icon = Lucide.SkipBack,
                contentDescription = stringResource(Res.string.player_controls_prev_episode),
                size = 48.dp,
                iconSize = 22.dp,
                tint = if (prevEpisode != null) Color.White else Color.White.copy(alpha = 0.3f),
                onClick = {
                    prevEpisode?.let { onSelectEpisode(it) }
                }
            )

            Spacer(modifier = Modifier.width(16.dp))
        }

        // 2. Перемотка назад -10 сек
        PlayerIslandIconButton(
            icon = Lucide.RotateCcw,
            contentDescription = stringResource(Res.string.player_controls_prev_10),
            size = 52.dp,
            iconSize = 24.dp,
            onClick = {
                val target = (currentTime - 10.0).coerceAtLeast(0.0)
                controller.seek(target)
            }
        )

        Spacer(modifier = Modifier.width(20.dp))

        // 3. Центральная крупная кнопка Play / Pause (Стартовый фокус пульта)
        PlayerIslandIconButton(
            icon = if (isPlaying) Lucide.Pause else Lucide.Play,
            contentDescription = if (isPlaying) stringResource(Res.string.player_controls_pause) else stringResource(Res.string.player_controls_play),
            size = 68.dp,
            iconSize = 34.dp,
            tint = Color.White,
            onClick = {
                if (isPlaying) controller.pause() else controller.play()
            },
            modifier = Modifier.focusRequester(playPauseFocusRequester)
        )

        Spacer(modifier = Modifier.width(20.dp))

        // 4. Перемотка вперед +10 сек
        PlayerIslandIconButton(
            icon = Lucide.RotateCw,
            contentDescription = stringResource(Res.string.player_controls_next_10),
            size = 52.dp,
            iconSize = 24.dp,
            onClick = {
                val target = (currentTime + 10.0).coerceAtMost(duration)
                controller.seek(target)
            }
        )

        // 5. Следующая серия (только для сериалов)
        if (hasEpisodesContext) {
            Spacer(modifier = Modifier.width(16.dp))

            PlayerIslandIconButton(
                icon = Lucide.SkipForward,
                contentDescription = stringResource(Res.string.player_controls_next_episode),
                size = 48.dp,
                iconSize = 22.dp,
                tint = if (nextEpisode != null) Color.White else Color.White.copy(alpha = 0.3f),
                onClick = {
                    nextEpisode?.let { onSelectEpisode(it) }
                }
            )
        }
    }
}
