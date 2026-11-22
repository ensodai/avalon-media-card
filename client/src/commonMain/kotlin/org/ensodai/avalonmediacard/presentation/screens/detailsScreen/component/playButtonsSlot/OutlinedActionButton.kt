package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playButtonsSlot

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonSize
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonVariant
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon

@Deprecated("Migrated to AvalonButton in :core:designsystem")
@Composable
fun OutlinedActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    AvalonButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = AvalonButtonVariant.Secondary,
        size = AvalonButtonSize.Standard,
        enabled = !isLoading,
        leadingIcon = {
            AvalonIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}
