package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.details_custom_lists
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ButtonItem
import org.ensodai.avalonmediacard.presentation.components.IconManager
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.LocalTvDrawerNavigator
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot.customLists.CustomListsDropdownMenu
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot.customLists.CustomListsSelectionScreen
import org.jetbrains.compose.resources.stringResource

/**
 * Кнопка управления пользовательскими списками карточки.
 * На ТВ открывает выезжающую правую ТВ-шторку через [LocalTvDrawerNavigator],
 * на Desktop/Web/Mobile открывает выпадающее меню [CustomListsDropdownMenu].
 */
@Composable
fun CustomListDropdownButton(
    button: ButtonItem,
    isLoading: Boolean = false,
    onAction: (Action) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val buttonFocusRequester = remember { FocusRequester() }

    val deviceTarget = LocalDeviceTarget.current
    val drawerNavigator = LocalTvDrawerNavigator.current
    val currentButton by rememberUpdatedState(button)
    val drawerTitle = stringResource(Res.string.details_custom_lists)

    Box {
        SecondaryActionButton(
            icon = IconManager.getIcon(button.icon),
            tint = Color.White,
            isLoading = isLoading,
            modifier = Modifier.focusRequester(buttonFocusRequester),
            onClick = {
                if (deviceTarget.isTv) {
                    drawerNavigator.open(
                        screen = CustomListsSelectionScreen(
                            title = drawerTitle,
                            buttonProvider = { currentButton },
                            callerFocusRequester = buttonFocusRequester,
                            onAction = onAction
                        ),
                        caller = buttonFocusRequester
                    )
                } else {
                    expanded = true
                }
            }
        )

        if (expanded && !deviceTarget.isTv) {
            CustomListsDropdownMenu(
                expanded = expanded,
                button = button,
                onDismissRequest = { expanded = false },
                onAction = onAction
            )
        }
    }
}