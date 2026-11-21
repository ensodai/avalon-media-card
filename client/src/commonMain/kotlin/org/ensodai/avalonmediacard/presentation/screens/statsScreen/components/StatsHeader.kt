package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
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
            Text(
                text = stringResource(Res.string.stats_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = stringResource(Res.string.stats_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        IconButton(
            onClick = onRefresh,
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Lucide.RefreshCw,
                contentDescription = stringResource(Res.string.stats_refresh),
                tint = if (isLoading) colors.textSecondary.copy(alpha = 0.4f) else colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
