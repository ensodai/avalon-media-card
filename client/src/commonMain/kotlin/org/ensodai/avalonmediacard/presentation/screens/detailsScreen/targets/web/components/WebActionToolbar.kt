package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ButtonItem
import org.ensodai.avalonmediacard.contract.slot.IconType
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.withParameter
import org.ensodai.avalonmediacard.presentation.components.IconManager
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenuItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.EpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot.SecondaryActionButton
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.statusAndRatingSlot.DefaultStatusOptionList
import org.jetbrains.compose.resources.stringResource

@Composable
fun WebActionToolbar(
    playButtons: SlotData.ButtonGroup?,
    collectionButtons: SlotData.ButtonGroup?,
    userActions: SlotData.UserActions?,
    isLoading: Boolean = false,
    onAction: (Action) -> Unit,
    onRequestOtherSource: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = isLoading,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        modifier = modifier
    ) { loading ->
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Play button skeleton
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(44.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(22.dp))
                )
                // Custom list button skeleton
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(44.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(22.dp))
                )
                // Favorite heart skeleton
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shimmerPlaceholder(true, CircleShape)
                )
                // Divider
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
                // Status dropdown skeleton
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(40.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(20.dp))
                )
                // Rating dropdown skeleton
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(40.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(20.dp))
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Primary Play Button
                val primaryBtn = playButtons?.buttons?.firstOrNull()
                val playLabel = primaryBtn?.label?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.details_btn_watch_online)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(44.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.04f,
                            shape = RoundedCornerShape(22.dp),
                            onClick = { primaryBtn?.action?.let { onAction(it) } }
                        )
                        .background(Color.White, RoundedCornerShape(22.dp))
                        .padding(horizontal = 22.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Play,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = playLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // 2. Custom List Dropdown Button (+ В списки ∨)
                val customListBtn = collectionButtons?.buttons?.find { it.customLists != null || it.createListActionTemplate != null }
                if (customListBtn != null) {
                    WebCustomListButton(
                        button = customListBtn,
                        onAction = onAction
                    )
                }

                // 3. Favorite / Secondary Action Buttons
                val favoriteBtn = collectionButtons?.buttons?.find { it.customLists == null && it.createListActionTemplate == null }
                if (favoriteBtn != null) {
                    val isFavorite = favoriteBtn.icon == IconType.HEART_FILLED
                    val icon = IconManager.getIcon(favoriteBtn.icon)
                    val tint = if (isFavorite) Color(0xFFE91E63) else Color.White
                    SecondaryActionButton(
                        icon = icon,
                        tint = tint,
                        onClick = { favoriteBtn.action?.let { onAction(it) } }
                    )
                }

                // Divider between main player actions and tracking actions
                if (userActions != null || onRequestOtherSource != null) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                }

                // 4. Status Dropdown (👁 Смотрю ∨)
                if (userActions != null) {
                    WebStatusDropdown(
                        userActions = userActions,
                        onAction = onAction
                    )

                    // 5. Rating Dropdown (★ Оценка ∨)
                    WebRatingDropdown(
                        userActions = userActions,
                        onAction = onAction
                    )
                }

                // 6. Optional Sources Button
                if (onRequestOtherSource != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(40.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.04f,
                                defaultBorderWidth = 1.dp,
                                defaultBorderColor = Color.White.copy(alpha = 0.12f),
                                activeBorderWidth = 1.dp,
                                activeBorderColor = Color.White.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(20.dp),
                                onClick = onRequestOtherSource
                            )
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.HardDrive,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.details_sources_select),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebCustomListButton(
    button: ButtonItem,
    onAction: (Action) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isCreatingList by remember { mutableStateOf(false) }
    var listName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

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

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(44.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.04f,
                    defaultBorderWidth = 1.dp,
                    defaultBorderColor = Color.White.copy(alpha = 0.15f),
                    activeBorderWidth = 1.dp,
                    activeBorderColor = Color.White,
                    shape = RoundedCornerShape(22.dp),
                    onClick = { expanded = true }
                )
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.details_custom_lists),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = Color.White.copy(alpha = 0.6f)
            )
        }

        if (expanded) {
            val density = LocalDensity.current
            AvalonDropdownMenu(
                expanded = expanded,
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 50.dp.roundToPx() }),
                onDismissRequest = {
                    expanded = false
                    isCreatingList = false
                    listName = ""
                },
                width = 300.dp
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 260.dp)
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
                                .background(if (isItemHovered) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { onAction(list.toggleAction) }
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
                                keyboardActions = KeyboardActions(onDone = { submitNewList() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { isInputFocused = it.isFocused }
                                    .onPreviewKeyEvent { event ->
                                        if ((event.key == Key.Enter || event.key == Key.NumPadEnter) && event.type == KeyEventType.KeyDown) {
                                            submitNewList()
                                            true
                                        } else false
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
                                tint = if (listName.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
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
                                contentDescription = null,
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

@Composable
private fun WebStatusDropdown(
    userActions: SlotData.UserActions,
    onAction: (Action) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentStatusOption = DefaultStatusOptionList.find { it.status == userActions.currentStatus }
    val label = if (currentStatusOption != null) {
        stringResource(currentStatusOption.labelRes)
    } else {
        stringResource(Res.string.details_status_add)
    }
    val iconTint = if (currentStatusOption != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(40.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.04f,
                    defaultBorderWidth = 1.dp,
                    defaultBorderColor = Color.White.copy(alpha = 0.12f),
                    activeBorderWidth = 1.dp,
                    activeBorderColor = Color.White.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { expanded = true }
                )
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp)
        ) {
            Icon(
                Lucide.Eye,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Lucide.ChevronDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }

        if (expanded) {
            val density = LocalDensity.current
            AvalonDropdownMenu(
                expanded = expanded,
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 46.dp.roundToPx() }),
                onDismissRequest = { expanded = false }
            ) {
                DefaultStatusOptionList.forEach { option ->
                    val isSelected = userActions.currentStatus == option.status
                    val optionLabel = stringResource(option.labelRes)
                    AvalonDropdownMenuItem(
                        text = optionLabel,
                        icon = if (isSelected) Lucide.Check else null,
                        textColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        iconColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        onClick = {
                            val newStatus = if (isSelected) MediaStatus.NONE else option.status
                            val action = userActions.statusOptions[newStatus]
                            if (action != null) onAction(action)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WebRatingDropdown(
    userActions: SlotData.UserActions,
    onAction: (Action) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentRating = userActions.currentRating?.takeIf { it > 0 }
    val label = if (currentRating != null) {
        stringResource(Res.string.details_rating_my, currentRating)
    } else {
        stringResource(Res.string.details_rating_rate)
    }
    val iconTint = if (currentRating != null) Color(0xFFFFC107) else Color.White.copy(alpha = 0.7f)

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(40.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.04f,
                    defaultBorderWidth = 1.dp,
                    defaultBorderColor = Color.White.copy(alpha = 0.12f),
                    activeBorderWidth = 1.dp,
                    activeBorderColor = Color.White.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { expanded = true }
                )
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp)
        ) {
            Icon(
                Lucide.Star,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Lucide.ChevronDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }

        if (expanded) {
            val density = LocalDensity.current
            EpisodeRatingPopup(
                currentRating = currentRating,
                maxRating = userActions.maxRating,
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 46.dp.roundToPx() }),
                onDismiss = { expanded = false },
                onRate = { rating ->
                    val action = userActions.ratingOptions[rating]
                    if (action != null) onAction(action)
                    expanded = false
                }
            )
        }
    }
}
