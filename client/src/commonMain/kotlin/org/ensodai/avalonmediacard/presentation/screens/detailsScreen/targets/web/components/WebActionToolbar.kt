package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.components

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

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
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonSize
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonVariant
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonIconButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
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
    modifier: Modifier = Modifier,
    playButtons: SlotData.ButtonGroup?,
    collectionButtons: SlotData.ButtonGroup?,
    userActions: SlotData.UserActions?,
    isLoading: Boolean = false,
    onAction: (Action) -> Unit,
    onRequestOtherSource: (() -> Unit)? = null,
    onOpenWatchParty: (() -> Unit)? = null
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
                        .shimmerPlaceholder(true, AvalonTheme.shapes.control)
                )
                // Custom list button skeleton
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(40.dp)
                        .shimmerPlaceholder(true, AvalonTheme.shapes.control)
                )
                // Favorite heart skeleton
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shimmerPlaceholder(true, AvalonTheme.shapes.control)
                )
                // Divider
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp)
                        .height(20.dp)
                        .background(AvalonTheme.colors.borderSubtle)
                )
                // Status dropdown skeleton
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(40.dp)
                        .shimmerPlaceholder(true, AvalonTheme.shapes.control)
                )
                // Rating dropdown skeleton
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(40.dp)
                        .shimmerPlaceholder(true, AvalonTheme.shapes.control)
                )
                // Settings button skeleton
                if (onOpenWatchParty != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shimmerPlaceholder(true, AvalonTheme.shapes.control)
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Primary Play Button
                val primaryBtn = playButtons?.buttons?.firstOrNull()
                val playLabel = primaryBtn?.label?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.details_btn_watch_online)
                
                AvalonButton(
                    text = playLabel,
                    onClick = { primaryBtn?.action?.let { onAction(it) } },
                    variant = AvalonButtonVariant.Primary,
                    size = AvalonButtonSize.Hero,
                    leadingIcon = {
                        AvalonIcon(
                            imageVector = Lucide.Play,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

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
                    val tint = if (isFavorite) Color(0xFFE91E63) else AvalonTheme.colors.textPrimary
                    AvalonIconButton(
                        onClick = { favoriteBtn.action?.let { onAction(it) } },
                        contentDescription = "Favorite",
                        variant = AvalonButtonVariant.Secondary,
                        size = AvalonButtonSize.Standard
                    ) {
                        AvalonIcon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                    AvalonButton(
                        text = stringResource(Res.string.details_sources_select),
                        onClick = onRequestOtherSource,
                        variant = AvalonButtonVariant.Secondary,
                        size = AvalonButtonSize.Standard,
                        leadingIcon = {
                            AvalonIcon(
                                imageVector = Lucide.HardDrive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                // 7. Settings Dropdown (Шестеренка: совместный просмотр и др.)
                if (onOpenWatchParty != null) {
                    WebSettingsMenuButton(
                        onOpenWatchParty = onOpenWatchParty
                    )
                }
            }
        }
    }
}

@Composable
private fun WebSettingsMenuButton(
    onOpenWatchParty: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box {
        AvalonIconButton(
            onClick = { expanded = !expanded },
            contentDescription = stringResource(Res.string.settings_title),
            variant = AvalonButtonVariant.Secondary,
            size = AvalonButtonSize.Standard
        ) {
            AvalonIcon(
                imageVector = Lucide.Settings,
                contentDescription = null,
                tint = if (expanded) AvalonTheme.colors.textPrimary else AvalonTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (expanded) {
            AvalonDropdownMenu(
                expanded = expanded,
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 46.dp.roundToPx() }),
                onDismissRequest = { expanded = false }
            ) {
                AvalonDropdownMenuItem(
                    text = stringResource(Res.string.watch_party_title),
                    icon = Lucide.Users,
                    textColor = Color.White,
                    iconColor = AvalonTheme.colors.accentPrimary,
                    onClick = {
                        expanded = false
                        onOpenWatchParty()
                    }
                )
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
        AvalonButton(
            onClick = { expanded = true },
            text = stringResource(Res.string.details_custom_lists),
            leadingIcon = {
                AvalonIcon(
                    imageVector = Lucide.Plus,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                AvalonIcon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AvalonTheme.colors.textMuted
                )
            },
            variant = AvalonButtonVariant.Secondary,
            size = AvalonButtonSize.Standard
        )

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
                            AvalonText(
                                text = list.name,
                                color = if (list.isAdded) Color.White else Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = if (list.isAdded) FontWeight.Bold else FontWeight.Normal
                            )
                            if (list.isAdded) {
                                AvalonIcon(
                                    imageVector = Lucide.Check,
                                    contentDescription = "Added",
                                    tint = AvalonTheme.colors.accentPrimary,
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
                                cursorBrush = SolidColor(AvalonTheme.colors.accentPrimary),
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
                                        color = if (isInputFocused) AvalonTheme.colors.accentPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                decorationBox = { innerTextField ->
                                    if (listName.isEmpty()) {
                                        AvalonText(
                                            stringResource(Res.string.details_custom_list_name_placeholder),
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AvalonIcon(
                                imageVector = Lucide.Check,
                                contentDescription = stringResource(Res.string.common_save),
                                tint = if (listName.isNotBlank()) AvalonTheme.colors.accentPrimary else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerHoverIcon(if (listName.isNotBlank()) PointerIcon.Hand else PointerIcon.Default)
                                    .clickable(enabled = listName.isNotBlank()) { submitNewList() }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AvalonIcon(
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
                            AvalonIcon(
                                imageVector = Lucide.Plus,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            AvalonText(
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
    val iconTint = if (currentStatusOption != null) AvalonTheme.colors.accentPrimary else Color.White.copy(alpha = 0.7f)

    Box {
        AvalonButton(
            onClick = { expanded = true },
            text = label,
            leadingIcon = {
                AvalonIcon(
                    imageVector = Lucide.Eye,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            },
            trailingIcon = {
                AvalonIcon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = null,
                    tint = AvalonTheme.colors.textMuted,
                    modifier = Modifier.size(13.dp)
                )
            },
            variant = AvalonButtonVariant.Secondary,
            size = AvalonButtonSize.Standard
        )

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
                        textColor = if (isSelected) AvalonTheme.colors.accentPrimary else Color.White,
                        iconColor = if (isSelected) AvalonTheme.colors.accentPrimary else Color.White,
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
    val iconTint = if (currentRating != null) AvalonTheme.colors.accentGold else AvalonTheme.colors.textMuted

    Box {
        AvalonButton(
            onClick = { expanded = true },
            text = label,
            leadingIcon = {
                AvalonIcon(
                    imageVector = Lucide.Star,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            },
            trailingIcon = {
                AvalonIcon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = null,
                    tint = AvalonTheme.colors.textMuted,
                    modifier = Modifier.size(13.dp)
                )
            },
            variant = AvalonButtonVariant.Secondary,
            size = AvalonButtonSize.Standard
        )

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
