package org.ensodai.avalonmediacard.presentation.screens.player.component.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

/**
 * Индикатор-островок "Все серии" / "Скрыть серии".
 * Оформлен в едином стиле темных парящих островков (Floating Island) с поддержкой D-Pad фокуса и тача.
 */
@Composable
fun TvEpisodeShelfExpander(
    isExpanded: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .tvAndWebHoverEffect(
                    scaleTarget = 1.06f,
                    shape = shape,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    activeBorderWidth = 2.dp,
                    defaultBorderColor = Color.White.copy(alpha = 0.08f),
                    defaultBorderWidth = 1.dp,
                    onClick = { onClick?.invoke() }
                )
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isExpanded) stringResource(Res.string.player_episodes_hide) else stringResource(Res.string.player_episodes_all),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = stringResource(Res.string.player_episodes_title),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}
