package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot.customLists

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.details_custom_list_create
import avalonmediacard.client.generated.resources.details_custom_list_creation
import avalonmediacard.client.generated.resources.details_custom_list_name_placeholder
import avalonmediacard.client.generated.resources.player_btn_back
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ButtonItem
import org.ensodai.avalonmediacard.contract.slot.withParameter
import org.ensodai.avalonmediacard.presentation.components.IconManager
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTextField
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerNavigator
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerScreen
import org.jetbrains.compose.resources.stringResource

/**
 * Экран шторки со списком пользовательских списков.
 */
class CustomListsSelectionScreen(
    override val title: String,
    private val buttonProvider: () -> ButtonItem,
    override val callerFocusRequester: FocusRequester? = null,
    private val onAction: (Action) -> Unit
) : TvDrawerScreen {
    override val key: String = "custom_lists_selection"

    override val icon: ImageVector?
        get() = IconManager.getIcon(buttonProvider().icon)

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        val button = buttonProvider()
        val lists = button.customLists.orEmpty()
        val createTemplate = button.createListActionTemplate
        val createTitle = stringResource(Res.string.details_custom_list_creation)

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(
                items = lists,
                key = { it.id }
            ) { list ->
                AvalonTvDrawerItem(
                    title = list.name.ifBlank { "Список без названия" },
                    isSelected = list.isAdded,
                    onClick = { onAction(list.toggleAction) }
                )
            }

            if (createTemplate != null) {
                item(key = "create_new_list_button") {
                    AvalonTvDrawerItem(
                        title = stringResource(Res.string.details_custom_list_create),
                        icon = Lucide.Plus,
                        onClick = {
                            navigator.push(
                                CreateCustomListDrawerScreen(
                                    title = createTitle,
                                    createTemplate = createTemplate,
                                    onAction = onAction
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Экран создания нового пользовательского списка.
 */
class CreateCustomListDrawerScreen(
    override val title: String,
    private val createTemplate: Action,
    private val onAction: (Action) -> Unit
) : TvDrawerScreen {
    override val key: String = "create_custom_list"

    override val icon: ImageVector = Lucide.Plus

    override val initialFocusRequester: FocusRequester = FocusRequester()

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        var listNameTv by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current

        val submitNewListTv = {
            val trimmed = listNameTv.trim()
            if (trimmed.isNotBlank()) {
                onAction(createTemplate.withParameter("listName", trimmed))
                navigator.pop()
            }
        }

        LaunchedEffect(Unit) {
            keyboardController?.show()
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "back_button") {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.player_btn_back),
                    icon = Lucide.ArrowLeft,
                    onClick = { navigator.pop() }
                )
            }

            item(key = "list_name_input") {
                AvalonTextField(
                    value = listNameTv,
                    onValueChange = { listNameTv = it },
                    focusRequester = initialFocusRequester,
                    initialFocus = true,
                    placeholder = stringResource(Res.string.details_custom_list_name_placeholder),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitNewListTv() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .onPreviewKeyEvent { event ->
                            if ((event.key == Key.Escape || event.key == Key.Back) && event.type == KeyEventType.KeyDown) {
                                navigator.pop()
                                true
                            } else if ((event.key == Key.DirectionCenter || event.key == Key.Enter) && event.type == KeyEventType.KeyDown) {
                                if (listNameTv.trim().isNotBlank()) {
                                    submitNewListTv()
                                    true
                                } else {
                                    keyboardController?.show()
                                    false
                                }
                            } else {
                                false
                            }
                        }
                )
            }

            if (listNameTv.trim().isNotBlank()) {
                item(key = "submit_button") {
                    AvalonTvDrawerItem(
                        title = stringResource(Res.string.details_custom_list_create),
                        icon = Lucide.Check,
                        onClick = { submitNewListTv() }
                    )
                }
            }
        }
    }
}
