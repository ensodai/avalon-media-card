package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton as Button
import org.ensodai.avalonmediacard.designsystem.components.ButtonDefaults

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.slot.Action
import org.jetbrains.compose.resources.stringResource

@Composable
fun SlotErrorCard(
    message: String,
    retryAction: Action?,
    onAction: ((Action) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AvalonTheme.colors.errorContainer.copy(alpha = 0.12f))
            .border(1.dp, AvalonTheme.colors.error.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Lucide.Info,
                contentDescription = null,
                tint = AvalonTheme.colors.error,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.common_error_something_went_wrong),
                    style = AvalonTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = AvalonTheme.colors.error
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = AvalonTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = AvalonTheme.colors.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            if (retryAction != null && onAction != null) {
                Button(
                    onClick = { onAction(retryAction) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AvalonTheme.colors.error
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(Res.string.common_retry), color = Color.White, style = AvalonTheme.typography.labelLarge)
                }
            }
        }
    }
}
