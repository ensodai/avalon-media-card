package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.statusAndRatingSlot

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenuItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.EpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvEpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class StatusOptionRes(val status: MediaStatus, val labelRes: StringResource)

val DefaultStatusOptionList = listOf(
    StatusOptionRes(MediaStatus.WATCHING, Res.string.details_status_watching),
    StatusOptionRes(MediaStatus.PLANNED, Res.string.details_status_planned),
    StatusOptionRes(MediaStatus.COMPLETED, Res.string.details_status_completed),
    StatusOptionRes(MediaStatus.DROPPED, Res.string.details_status_dropped)
)

@Composable
fun StatusAndRatingSlotContent(
    component: SlotData.UserActions,
    isLoading: Boolean = false,
    onAction: (Action) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDropdownButton(component, isLoading, onAction)

        // Тонкий вертикальный разделитель
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(if (isLoading) Color.Transparent else AvalonTheme.colors.borderSubtle)
        )

        RatingDropdownButton(component, isLoading, onAction)
    }
}

@Composable
fun StatusDropdownButton(
    component: SlotData.UserActions,
    isLoading: Boolean,
    onAction: (Action) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val buttonFocusRequester = remember { FocusRequester() }
    val currentStatusOption = DefaultStatusOptionList.find { it.status == component.currentStatus }
    val label = if (currentStatusOption != null) {
        stringResource(currentStatusOption.labelRes)
    } else {
        stringResource(Res.string.details_status_add)
    }
    val iconTint = if (currentStatusOption != null) AvalonTheme.colors.accentPrimary else AvalonTheme.colors.textSecondary

    fun safeDismiss() {
        expanded = false
        runCatching { buttonFocusRequester.requestFocus() }
    }

    Box {
        // Сама кнопка
        Row(
            modifier = Modifier
                .focusRequester(buttonFocusRequester)
                .shimmerPlaceholder(isLoading, AvalonTheme.shapes.control)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.04f,
                    shape = AvalonTheme.shapes.control,
                    clickEnabled = !isLoading,
                    onClick = { expanded = true }
                )
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isTvTarget = LocalDeviceTarget.current.isTv
            AvalonIcon(
                Lucide.Eye,
                contentDescription = null,
                tint = if (isLoading) Color.Transparent else iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            AvalonText(
                text = label,
                color = if (isLoading) Color.Transparent else AvalonTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (!isTvTarget) {
                Spacer(modifier = Modifier.width(4.dp))
                AvalonIcon(
                    Lucide.ChevronDown,
                    contentDescription = null,
                    tint = if (isLoading) Color.Transparent else AvalonTheme.colors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Выпадающее меню или Шторка
        if (expanded) {
            val isTvTarget = LocalDeviceTarget.current.isTv
            if (isTvTarget) {
                TvDrawerEffect(
                    title = stringResource(Res.string.details_status_my),
                    icon = Lucide.Eye,
                    callerFocusRequester = buttonFocusRequester,
                    onDismiss = { safeDismiss() }
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.details_status_remove),
                                icon = Lucide.X,
                                isSelected = component.currentStatus == MediaStatus.NONE,
                                onClick = {
                                    val action = component.statusOptions[MediaStatus.NONE]
                                    if (action != null) onAction(action)
                                    safeDismiss()
                                }
                            )
                        }
                        items(DefaultStatusOptionList) { option ->
                            val isSelected = component.currentStatus == option.status
                            AvalonTvDrawerItem(
                                title = stringResource(option.labelRes),
                                isSelected = isSelected,
                                onClick = {
                                    val action = component.statusOptions[option.status]
                                    if (action != null) onAction(action)
                                    safeDismiss()
                                }
                            )
                        }
                    }
                }
            } else {
                val density = LocalDensity.current
                AvalonDropdownMenu(
                    expanded = expanded,
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, with(density) { 40.dp.roundToPx() }),
                    onDismissRequest = { safeDismiss() }
                ) {
                    DefaultStatusOptionList.forEach { option ->
                        val isSelected = component.currentStatus == option.status
                        val optionLabel = stringResource(option.labelRes)
                        AvalonDropdownMenuItem(
                            text = optionLabel,
                            icon = if (isSelected) Lucide.Check else null,
                            textColor = if (isSelected) AvalonTheme.colors.accentPrimary else AvalonTheme.colors.textPrimary,
                            iconColor = if (isSelected) AvalonTheme.colors.accentPrimary else AvalonTheme.colors.textPrimary,
                            onClick = {
                                val newStatus = if (isSelected) MediaStatus.NONE else option.status
                                val action = component.statusOptions[newStatus]
                                if (action != null) {
                                    onAction(action)
                                }
                                safeDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDropdownButton(
    component: SlotData.UserActions,
    isLoading: Boolean,
    onAction: (Action) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val buttonFocusRequester = remember { FocusRequester() }
    val currentRating = component.currentRating
    val label = if (currentRating != null) {
        stringResource(Res.string.details_rating_my, currentRating)
    } else {
        stringResource(Res.string.details_rating_rate)
    }
    val iconTint = if (currentRating != null) AvalonTheme.colors.accentGold else AvalonTheme.colors.textSecondary

    fun safeDismiss() {
        expanded = false
        runCatching { buttonFocusRequester.requestFocus() }
    }

    Box {
        // Сама кнопка
        Row(
            modifier = Modifier
                .focusRequester(buttonFocusRequester)
                .shimmerPlaceholder(isLoading, AvalonTheme.shapes.control)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.04f,
                    shape = AvalonTheme.shapes.control,
                    clickEnabled = !isLoading,
                    onClick = { expanded = true }
                )
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isTvTarget = LocalDeviceTarget.current.isTv
            AvalonIcon(
                Lucide.Star,
                contentDescription = null,
                tint = if (isLoading) Color.Transparent else iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            AvalonText(
                text = label,
                color = if (isLoading) Color.Transparent else AvalonTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (!isTvTarget) {
                Spacer(modifier = Modifier.width(4.dp))
                AvalonIcon(
                    Lucide.ChevronDown,
                    contentDescription = null,
                    tint = if (isLoading) Color.Transparent else AvalonTheme.colors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Выпадающее меню с логикой звезд
        if (expanded) {
            val isTvTarget = LocalDeviceTarget.current.isTv
            if (isTvTarget) {
                TvEpisodeRatingPopup(
                    currentRating = currentRating,
                    maxRating = component.maxRating,
                    callerFocusRequester = buttonFocusRequester,
                    onDismiss = { safeDismiss() },
                    onRate = { rating ->
                        val action = component.ratingOptions[rating]
                        if (action != null) {
                            onAction(action)
                        }
                        safeDismiss()
                    }
                )
            } else {
                val density = LocalDensity.current
                EpisodeRatingPopup(
                    currentRating = currentRating,
                    maxRating = component.maxRating,
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, with(density) { 40.dp.roundToPx() }),
                    onDismiss = { safeDismiss() },
                    onRate = { rating ->
                        val action = component.ratingOptions[rating]
                        if (action != null) {
                            onAction(action)
                        }
                        safeDismiss()
                    }
                )
            }
        }
    }
}