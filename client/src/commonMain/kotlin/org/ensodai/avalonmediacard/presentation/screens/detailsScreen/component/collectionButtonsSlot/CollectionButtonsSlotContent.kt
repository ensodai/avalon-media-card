package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.IconType
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.IconManager

@Composable
fun CollectionButtonsSlotContent(
    data: SlotData.ButtonGroup,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isLoading) {
            SecondaryActionButton(icon = Lucide.Heart, tint = Color.Transparent, isLoading = true, onClick = {})
            SecondaryActionButton(icon = Lucide.Heart, tint = Color.Transparent, isLoading = true, onClick = {})
        } else {
            data.buttons.forEach { btn ->
                key(btn.icon?.name ?: btn.label) {
                    if (!btn.customLists.isNullOrEmpty() || btn.createListActionTemplate != null) {
                        CustomListDropdownButton(button = btn, onAction = onAction)
                    } else {
                        SecondaryActionButton(
                            icon = IconManager.getIcon(btn.icon),
                            tint = if (btn.icon == IconType.HEART_FILLED) Color(0xFFE91E63) else Color.White,
                            isLoading = false,
                            onClick = { btn.action?.let { onAction(it) } }
                        )
                    }
                }
            }
        }
    }
}