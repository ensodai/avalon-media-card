package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import org.ensodai.avalonmediacard.designsystem.components.ButtonDefaults
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun SlotErrorCard(
    message: String,
    retryAction: Action?,
    onAction: ((Action) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val elevation = AvalonTheme.elevation
    val typography = AvalonTheme.typography

    Box(
        modifier = modifier
            .clip(shapes.container)
            .background(colors.statusDanger.copy(alpha = 0.12f))
            .border(elevation.borderWidth, colors.statusDanger.copy(alpha = 0.25f), shapes.container)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AvalonIcon(
                imageVector = Lucide.Info,
                contentDescription = null,
                tint = colors.statusDanger,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                AvalonText(
                    text = stringResource(Res.string.common_error_something_went_wrong),
                    style = typography.titleMedium.copy(fontSize = 15.sp),
                    color = colors.statusDanger
                )
                Spacer(modifier = Modifier.height(2.dp))
                AvalonText(
                    text = message,
                    style = typography.bodyMedium.copy(fontSize = 13.sp),
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )
            }

            if (retryAction != null && onAction != null) {
                AvalonButton(
                    onClick = { onAction(retryAction) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.statusDanger
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    shape = shapes.control
                ) {
                    AvalonText(
                        text = stringResource(Res.string.common_retry),
                        color = colors.textPrimary,
                        style = typography.labelLarge
                    )
                }
            }
        }
    }
}
