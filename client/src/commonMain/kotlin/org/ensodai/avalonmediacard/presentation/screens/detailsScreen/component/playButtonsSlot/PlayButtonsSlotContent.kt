package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playButtonsSlot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ButtonItem
import org.ensodai.avalonmediacard.contract.slot.IconType
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonSize
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonVariant
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayButtonsSlotContent(
    data: SlotData.ButtonGroup,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val watchOnlineLabel = stringResource(Res.string.details_btn_watch_online)
    val trailerLabel = stringResource(Res.string.details_btn_trailer)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val buttonsToRender = if (isLoading && data.buttons.isEmpty()) {
            listOf(
                ButtonItem(label = watchOnlineLabel, action = null, icon = IconType.PLAY),
                ButtonItem(label = trailerLabel, action = null, icon = IconType.PLAY)
            )
        } else {
            data.buttons
        }

        buttonsToRender.forEachIndexed { index, btn ->
            AvalonButton(
                text = btn.label,
                onClick = { btn.action?.let { onAction(it) } },
                variant = if (index == 0) AvalonButtonVariant.Primary else AvalonButtonVariant.Secondary,
                size = AvalonButtonSize.Standard,
                enabled = !isLoading,
                leadingIcon = {
                    AvalonIcon(
                        imageVector = Lucide.Play,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}
