package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonIconButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatsHeader(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            AvalonText(
                text = stringResource(Res.string.stats_screen_title),
                style = AvalonTheme.typography.pageTitle,
                color = colors.textPrimary
            )
            AvalonText(
                text = stringResource(Res.string.stats_screen_subtitle),
                style = AvalonTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        AvalonIconButton(
            onClick = onRefresh,
            enabled = !isLoading
        ) {
            AvalonIcon(
                imageVector = Lucide.RefreshCw,
                contentDescription = stringResource(Res.string.stats_refresh),
                tint = if (isLoading) colors.textSecondary.copy(alpha = 0.4f) else colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
