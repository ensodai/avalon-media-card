package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ButtonItem
import org.ensodai.avalonmediacard.contract.slot.withParameter
import org.ensodai.avalonmediacard.presentation.components.IconManager
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalTvDrawerState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTvDrawerItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ArrowLeft
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private enum class ListDrawerMenu { NONE, LISTS, CREATE }

@Composable
fun CustomListDropdownButton(
    button: ButtonItem,
    isLoading: Boolean = false,
    onAction: (Action) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isCreatingList by remember { mutableStateOf(false) }
    var listName by remember { mutableStateOf("") }
    var currentTvMenu by remember { mutableStateOf(ListDrawerMenu.NONE) }

    val focusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }
    var wasTvMenuOpen by remember { mutableStateOf(false) }

    val deviceTarget = LocalDeviceTarget.current

    val submitNewList = {
        if (listName.isNotBlank()) {
            val template = button.createListActionTemplate
            if (template != null) {
                onAction(template.withParameter("listName", listName))
            }
            isCreatingList = false
            listName = ""
        }
    }

    LaunchedEffect(isCreatingList) {
        if (isCreatingList) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(currentTvMenu) {
        if (currentTvMenu != ListDrawerMenu.NONE) {
            wasTvMenuOpen = true
        } else if (wasTvMenuOpen) {
            wasTvMenuOpen = false
            runCatching { buttonFocusRequester.requestFocus() }
        }
    }

    Box {
        SecondaryActionButton(
            icon = IconManager.getIcon(button.icon),
            tint = Color.White,
            modifier = Modifier.focusRequester(buttonFocusRequester),
            onClick = {
                if (deviceTarget.isTv) {
                    currentTvMenu = ListDrawerMenu.LISTS
                } else {
                    expanded = true
                }
            }
        )

        if (currentTvMenu != ListDrawerMenu.NONE) {
            org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect(
                title = stringResource(Res.string.details_custom_lists),
                icon = IconManager.getIcon(button.icon),
                onDismiss = { currentTvMenu = ListDrawerMenu.NONE }
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (button.customLists != null) {
                        items(
                            items = button.customLists!!,
                            key = { it.id }
                        ) { list ->
                            AvalonTvDrawerItem(
                                title = list.name,
                                isSelected = list.isAdded,
                                onClick = { onAction(list.toggleAction) }
                            )
                        }
                    }
                    if (button.createListActionTemplate != null) {
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.details_custom_list_create),
                                icon = Lucide.Plus,
                                onClick = { currentTvMenu = ListDrawerMenu.CREATE }
                            )
                        }
                    }
                }
            }
            
            if (currentTvMenu == ListDrawerMenu.CREATE) {
                org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect(
                    title = stringResource(Res.string.details_custom_list_creation),
                    icon = Lucide.Plus,
                    onDismiss = { currentTvMenu = ListDrawerMenu.LISTS }
                ) {
                    val inputFocusRequester = remember { FocusRequester() }
                    var listNameTv by remember { mutableStateOf("") }
                    val keyboardController = LocalSoftwareKeyboardController.current

                    val submitNewListTv = {
                        if (listNameTv.isNotBlank()) {
                            val template = button.createListActionTemplate
                            if (template != null) {
                                onAction(template.withParameter("listName", listNameTv))
                            }
                            currentTvMenu = ListDrawerMenu.LISTS
                        }
                    }

                    LaunchedEffect(Unit) {
                        runCatching { inputFocusRequester.requestFocus() }
                        keyboardController?.show()
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.player_btn_back),
                                icon = Lucide.ArrowLeft,
                                onClick = { currentTvMenu = ListDrawerMenu.LISTS }
                            )
                        }
                        item {
                            var isInputFocused by remember { mutableStateOf(false) }
                            val placeholderText = stringResource(Res.string.details_custom_list_name_placeholder)
                            
                            BasicTextField(
                                value = listNameTv,
                                onValueChange = { listNameTv = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { submitNewListTv() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .focusRequester(inputFocusRequester)
                                    .onFocusChanged { isInputFocused = it.isFocused }
                                    .onPreviewKeyEvent { event ->
                                        if ((event.key == Key.Escape || event.key == Key.Back) && event.type == KeyEventType.KeyDown) {
                                            currentTvMenu = ListDrawerMenu.LISTS
                                            true
                                        } else if ((event.key == Key.DirectionCenter || event.key == Key.Enter) && event.type == KeyEventType.KeyDown) {
                                            keyboardController?.show()
                                            false
                                        } else {
                                            false
                                        }
                                    }
                                    .background(
                                        color = if (isInputFocused) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isInputFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                decorationBox = { innerTextField ->
                                    if (listNameTv.isEmpty()) {
                                        Text(
                                            placeholderText,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }
            }
        }

        if (expanded && !deviceTarget.isTv) {
            val density = LocalDensity.current
            org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu(
                expanded = expanded,
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(density) { 56.dp.roundToPx() }),
                onDismissRequest = {
                    expanded = false
                    isCreatingList = false
                    listName = ""
                },
                width = 300.dp
            ) {

                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    button.customLists?.forEach { list ->

                        var isItemHovered by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerHoverIcon(PointerIcon.Hand)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Enter -> isItemHovered = true
                                                PointerEventType.Exit -> isItemHovered = false
                                            }
                                        }
                                    }
                                }
                                .background(if (isItemHovered) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                .clickable {
                                    onAction(list.toggleAction)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = list.name,
                                color = if (list.isAdded) Color.White else Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = if (list.isAdded) FontWeight.Bold else FontWeight.Normal
                            )
                            if (list.isAdded) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = "Added",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (button.createListActionTemplate != null) {
                    if (button.customLists?.isNotEmpty() == true) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(vertical = 4.dp)
                        )
                    }

                    if (isCreatingList) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            var isInputFocused by remember { mutableStateOf(false) }

                            BasicTextField(
                                value = listName,
                                onValueChange = { listName = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { submitNewList() }
                                ),

                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { isInputFocused = it.isFocused }
                                    .onPreviewKeyEvent { event ->
                                        if ((event.key == Key.Enter || event.key == Key.NumPadEnter) && event.type == KeyEventType.KeyDown) {
                                            submitNewList()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .background(
                                        color = if (isInputFocused) Color.White.copy(alpha = 0.12f) else Color.White.copy(
                                            alpha = 0.08f
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isInputFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                decorationBox = { innerTextField ->
                                    if (listName.isEmpty()) {
                                        Text(
                                            stringResource(Res.string.details_custom_list_name_placeholder),
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = stringResource(Res.string.common_save),
                                tint = if (listName.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(
                                    alpha = 0.3f
                                ), // Подсветим галочку цветом темы
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerHoverIcon(if (listName.isNotBlank()) PointerIcon.Hand else PointerIcon.Default)
                                    .clickable(enabled = listName.isNotBlank()) { submitNewList() }
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = stringResource(Res.string.common_cancel),
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable {
                                        isCreatingList = false
                                        listName = ""
                                    }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { isCreatingList = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.Plus,
                                contentDescription = stringResource(Res.string.details_custom_list_create),
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(Res.string.details_custom_list_create),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}