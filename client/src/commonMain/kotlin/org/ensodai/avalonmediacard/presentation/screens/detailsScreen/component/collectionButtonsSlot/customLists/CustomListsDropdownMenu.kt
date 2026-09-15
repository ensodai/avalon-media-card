package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot.customLists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.common_cancel
import avalonmediacard.client.generated.resources.common_save
import avalonmediacard.client.generated.resources.details_custom_list_create
import avalonmediacard.client.generated.resources.details_custom_list_name_placeholder
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ButtonItem
import org.ensodai.avalonmediacard.contract.slot.withParameter
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu
import org.jetbrains.compose.resources.stringResource

/**
 * Выпадающее меню кастомных списков для Desktop/Web/Mobile интерфейса (мышь/тач).
 */
@Composable
fun CustomListsDropdownMenu(
    expanded: Boolean,
    button: ButtonItem,
    onDismissRequest: () -> Unit,
    onAction: (Action) -> Unit
) {
    var isCreatingList by remember { mutableStateOf(false) }
    var listName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    val submitNewList = {
        val trimmed = listName.trim()
        if (trimmed.isNotBlank()) {
            val template = button.createListActionTemplate
            if (template != null) {
                onAction(template.withParameter("listName", trimmed))
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

    AvalonDropdownMenu(
        expanded = expanded,
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, with(density) { 56.dp.roundToPx() }),
        onDismissRequest = {
            isCreatingList = false
            listName = ""
            onDismissRequest()
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
                        .clickable { onAction(list.toggleAction) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = list.name.ifBlank { "Список без названия" },
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
                                color = if (isInputFocused) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f),
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
                        tint = if (listName.trim().isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(24.dp)
                            .pointerHoverIcon(if (listName.trim().isNotBlank()) PointerIcon.Hand else PointerIcon.Default)
                            .clickable(enabled = listName.trim().isNotBlank()) { submitNewList() }
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
