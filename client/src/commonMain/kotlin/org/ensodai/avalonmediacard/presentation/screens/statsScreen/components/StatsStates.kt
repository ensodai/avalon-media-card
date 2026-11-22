package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonCircularProgressIndicator
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatsLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp),
        contentAlignment = Alignment.Center
    ) {
        AvalonCircularProgressIndicator(
            color = AvalonTheme.colors.primary,
            modifier = Modifier.size(44.dp)
        )
    }
}

@Composable
fun StatsErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AvalonText(
                text = errorMessage,
                style = AvalonTheme.typography.bodyLarge,
                color = AvalonTheme.colors.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            AvalonButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp)
            ) {
                AvalonText(stringResource(Res.string.stats_retry))
            }
        }
    }
}
