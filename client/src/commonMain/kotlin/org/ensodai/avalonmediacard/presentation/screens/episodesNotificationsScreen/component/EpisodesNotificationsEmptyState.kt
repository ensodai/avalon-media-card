package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton as Button
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.episodes_notifications_empty_desc
import avalonmediacard.client.generated.resources.episodes_notifications_empty_title
import avalonmediacard.client.generated.resources.episodes_notifications_go_to_tv_shows
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tv
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.jetbrains.compose.resources.stringResource

@Composable
fun EpisodesNotificationsEmptyState(
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AvalonTheme.colors.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.CheckCheck,
                    contentDescription = null,
                    tint = AvalonTheme.colors.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(Res.string.episodes_notifications_empty_title),
                style = AvalonTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AvalonTheme.colors.onSurface
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.episodes_notifications_empty_desc),
                style = AvalonTheme.typography.bodyMedium.copy(
                    color = AvalonTheme.colors.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onAction(ActionNavigate(Screen.TvShows)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(
                    imageVector = Lucide.Tv,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(Res.string.episodes_notifications_go_to_tv_shows),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
