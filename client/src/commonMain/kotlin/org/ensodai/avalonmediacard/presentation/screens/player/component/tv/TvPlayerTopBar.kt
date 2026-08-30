package org.ensodai.avalonmediacard.presentation.screens.player.component.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandActionButton
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandDynamicButton
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandIconButton
import org.ensodai.avalonmediacard.presentation.screens.player.component.PlayerIslandTitle
import org.jetbrains.compose.resources.stringResource

/**
 * Верхний ТВ-бар плеера на независимых парящих островках (Floating Islands).
 */
@Composable
fun TvPlayerTopBar(
    topText: String,
    bottomText: String,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    hasCustomAudioOrSubtitle: Boolean = false,
    currentEpisode: MediaStream? = null,
    onToggleEpisodeWatched: (() -> Unit)? = null,
    onRateEpisode: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Левая группа: Кнопка Назад + Островок Названия
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Островок 1: Кнопка Назад с поддержкой D-Pad фокуса на ТВ
            PlayerIslandIconButton(
                icon = Lucide.ArrowLeft,
                contentDescription = stringResource(Res.string.player_btn_close),
                onClick = onClose
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Островок 2: Название и подзаголовок
            PlayerIslandTitle(
                topText = topText,
                bottomText = bottomText
            )
        }

        // Правая группа: Островки действий (Отметить, Оценка) + Островок Настроек
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentEpisode != null) {
                val isWatched = currentEpisode.isWatched
                val userRating = currentEpisode.userRating

                // Островок 3: Статус просмотра (стабильный фокус)
                if (onToggleEpisodeWatched != null) {
                    PlayerIslandDynamicButton(
                        icon = if (isWatched) Lucide.Check else Lucide.Eye,
                        text = if (isWatched) stringResource(Res.string.player_watched) else null,
                        contentDescription = if (isWatched) stringResource(Res.string.player_watched) else stringResource(Res.string.player_mark_watched),
                        iconTint = if (isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.85f),
                        textColor = if (isWatched) Color(0xFF4CAF50) else Color.White,
                        onClick = onToggleEpisodeWatched
                    )
                }

                // Островок 4: Оценка (стабильный фокус)
                PlayerIslandDynamicButton(
                    icon = Lucide.Star,
                    text = userRating?.toString(),
                    contentDescription = if (userRating != null) stringResource(Res.string.player_rating_val, userRating.toString()) else stringResource(Res.string.player_rate),
                    iconTint = if (userRating != null) Color(0xFFFFC107) else Color.White.copy(alpha = 0.85f),
                    textColor = if (userRating != null) Color(0xFFFFC107) else Color.White,
                    onClick = { onRateEpisode?.invoke() }
                )
            }

            // Островок 5: Настройки (Озвучки, Субтитры, Плеер, Другой источник)
            PlayerIslandIconButton(
                icon = Lucide.Settings,
                contentDescription = stringResource(Res.string.player_settings_title),
                tint = if (hasCustomAudioOrSubtitle) Color(0xFF4CAF50) else Color.White,
                onClick = onOpenSettings
            )
        }
    }
}
