package org.ensodai.avalonmediacard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.model.SidebarItemType
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.data.selectAndUploadPlugin
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenuItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTvDrawerItem
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.navigation.AvalonBackHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.ExperimentalComposeUiApi
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val logger = AppLogging.logger("Sidebar")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Sidebar(
    sidebarItems: List<SidebarItem>,
    selectedItem: SidebarItem?,
    onSelected: (SidebarItem) -> Unit,
    uploadStatus: String,
    onUploadFinished: (success: Boolean, message: String) -> Unit,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onLogout: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onIntegrationsClick: (() -> Unit)? = null,
    onAdminClick: (() -> Unit)? = null,
    userRole: org.ensodai.avalonmediacard.contract.model.UserRole? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    var isProfileMenuExpanded by remember { mutableStateOf(false) }
    val selectedItemFocusRequester = remember { FocusRequester() }

    val deviceTarget = LocalDeviceTarget.current
    var hasFocus by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }
    val isExpanded = hasFocus || isHovered

    LaunchedEffect(isExpanded) {
        onExpandedChange(isExpanded)
    }

    val width by animateDpAsState(targetValue = if (isExpanded) 260.dp else 80.dp, label = "sidebarWidth")
    val contentAlpha by animateFloatAsState(targetValue = if (isExpanded) 1f else 0f, label = "contentAlpha")

    val focusManager = LocalFocusManager.current
    AvalonBackHandler(enabled = isExpanded) {
        focusManager.moveFocus(FocusDirection.Right)
        isHovered = false
    }

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .onFocusChanged {
                hasFocus = it.hasFocus
                if (it.hasFocus) {
                    logger.d { "[FOCUS_DEBUG] Sidebar GOT FOCUS! (isFocused=${it.isFocused})" }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            }
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = outlineColor,
                    start = Offset(size.width - strokeWidth / 2, 0f),
                    end = Offset(size.width - strokeWidth / 2, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .padding(16.dp)
    ) {
        // Scrollable Menu
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRestorer(selectedItemFocusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box {
                    SidebarActionItem(
                        title = stringResource(Res.string.nav_profile),
                        icon = Lucide.User,
                        isExpanded = isExpanded,
                        isFocusable = isExpanded || selectedItem == null,
                        onClick = { isProfileMenuExpanded = true }
                    )

                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val deviceTarget = LocalDeviceTarget.current

                    if (deviceTarget.isDesktop) {
                        AvalonDropdownMenu(
                            expanded = isProfileMenuExpanded,
                            onDismissRequest = { isProfileMenuExpanded = false },
                            alignment = Alignment.TopStart,
                            offset = androidx.compose.ui.unit.IntOffset(0, with(density) { 8.dp.roundToPx() })
                        ) {
                            AvalonDropdownMenuItem(
                                text = stringResource(Res.string.nav_settings),
                                icon = Lucide.Settings,
                                onClick = {
                                    isProfileMenuExpanded = false
                                    onSettingsClick?.invoke()
                                    focusManager.moveFocus(FocusDirection.Right)
                                }
                            )
                            AvalonDropdownMenuItem(
                                text = stringResource(Res.string.nav_external_integrations),
                                icon = Lucide.Wrench,
                                onClick = {
                                    isProfileMenuExpanded = false
                                    onIntegrationsClick?.invoke()
                                    focusManager.moveFocus(FocusDirection.Right)
                                }
                            )
                            if (userRole == org.ensodai.avalonmediacard.contract.model.UserRole.ADMIN) {
                                AvalonDropdownMenuItem(
                                    text = stringResource(Res.string.nav_admin_panel),
                                    icon = Lucide.Shield,
                                    onClick = {
                                        isProfileMenuExpanded = false
                                        onAdminClick?.invoke()
                                        focusManager.moveFocus(FocusDirection.Right)
                                    }
                                )
                            }
                            AvalonDropdownMenuItem(
                                text = stringResource(Res.string.nav_upload_plugin),
                                icon = Lucide.Plus,
                                onClick = {
                                    isProfileMenuExpanded = false
                                    selectAndUploadPlugin { success, message ->
                                        onUploadFinished(success, message)
                                    }
                                }
                            )
                            AvalonDropdownMenuItem(
                                text = stringResource(Res.string.nav_logout),
                                icon = Lucide.LogOut,
                                textColor = Color(0xFFDC2626),
                                iconColor = Color(0xFFDC2626),
                                onClick = {
                                    isProfileMenuExpanded = false
                                    onLogout?.invoke()
                                }
                            )
                        }
                    } else {
                        if (isProfileMenuExpanded) {
                            TvDrawerEffect(
                                title = stringResource(Res.string.nav_profile),
                                icon = Lucide.User,
                                onDismiss = { isProfileMenuExpanded = false }
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    item {
                                        AvalonTvDrawerItem(
                                            title = stringResource(Res.string.nav_settings),
                                            icon = Lucide.Settings,
                                            onClick = {
                                                isProfileMenuExpanded = false
                                                onSettingsClick?.invoke()
                                                focusManager.moveFocus(FocusDirection.Right)
                                            }
                                        )
                                    }
                                    item {
                                        AvalonTvDrawerItem(
                                            title = stringResource(Res.string.nav_external_integrations),
                                            icon = Lucide.Wrench,
                                            onClick = {
                                                isProfileMenuExpanded = false
                                                onIntegrationsClick?.invoke()
                                                focusManager.moveFocus(FocusDirection.Right)
                                            }
                                        )
                                    }
                                    if (userRole == UserRole.ADMIN) {
                                        item {
                                            AvalonTvDrawerItem(
                                                title = stringResource(Res.string.nav_admin_panel),
                                                icon = Lucide.Shield,
                                                onClick = {
                                                    isProfileMenuExpanded = false
                                                    onAdminClick?.invoke()
                                                    focusManager.moveFocus(FocusDirection.Right)
                                                }
                                            )
                                        }
                                    }
                                    item {
                                        AvalonTvDrawerItem(
                                            title = stringResource(Res.string.nav_upload_plugin),
                                            icon = Lucide.Plus,
                                            onClick = {
                                                isProfileMenuExpanded = false
                                                selectAndUploadPlugin { success, message ->
                                                    onUploadFinished(success, message)
                                                }
                                            }
                                        )
                                    }
                                    if (onLogout != null) {
                                        item {
                                            AvalonTvDrawerItem(
                                                title = stringResource(Res.string.nav_logout),
                                                icon = Lucide.LogOut,
                                                onClick = {
                                                    isProfileMenuExpanded = false
                                                    onLogout.invoke()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            val mainItems = sidebarItems.filter { it.group == 0 }
            val collectionItems = sidebarItems.filter { it.group != 0 }

            items(mainItems) { item ->
                val isSelected = selectedItem?.itemId == item.itemId
                SidebarMenuItem(
                    item = item,
                    isSelected = isSelected,
                    onSelected = onSelected,
                    isMainGroup = true,
                    isExpanded = isExpanded,
                    modifier = if (isSelected) Modifier.focusRequester(selectedItemFocusRequester) else Modifier
                )
            }

            if (collectionItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(collectionItems) { item ->
                    val isSelected = selectedItem?.itemId == item.itemId
                    SidebarMenuItem(
                        item = item,
                        isSelected = isSelected,
                        onSelected = onSelected,
                        isMainGroup = false,
                        isExpanded = isExpanded,
                        modifier = if (isSelected) Modifier.focusRequester(selectedItemFocusRequester) else Modifier
                    )
                }
            }
        }

        // Upload status
        if (uploadStatus.isNotEmpty()) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uploadStatus,
                    fontSize = 12.sp,
                    color = if (uploadStatus.contains("Error", ignoreCase = true) || uploadStatus.contains("Ошибка", ignoreCase = true)) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp).alpha(contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

    }
}

@Composable
fun SidebarMenuItem(
    item: SidebarItem,
    isSelected: Boolean,
    onSelected: (SidebarItem) -> Unit,
    isMainGroup: Boolean,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    if (item.type == SidebarItemType.DIVIDER) {
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        return
    }

    val icon = when (item.iconName) {
        "home" -> Lucide.House
        "movie" -> Lucide.Clapperboard
        "tv" -> Lucide.Tv
        "flame" -> Lucide.Flame
        "folder" -> Lucide.Folder
        "list" -> Lucide.List
        "plus" -> Lucide.Plus
        "settings" -> Lucide.Settings
        "heart" -> Lucide.Heart
        "eye" -> Lucide.Eye
        "star" -> Lucide.Star
        "user" -> Lucide.User
        "LibraryBooks" -> Lucide.Folder
        "search" -> Lucide.Search
        else -> if (isMainGroup) Lucide.LayoutGrid else Lucide.Hash
    }

    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    val isTv = LocalDeviceTarget.current.isTv
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f, 
                shape = RoundedCornerShape(8.dp),
                focusEnabled = !isTv || isExpanded || isSelected,
                onClick = { 
                    onSelected(item)
                    focusManager.moveFocus(FocusDirection.Right)
                }
            )
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                else Color.Transparent
            )
            
            .padding(vertical = if (isMainGroup) 12.dp else 10.dp, horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (isMainGroup) 20.dp else 18.dp),
                tint = contentColor
            )
            val textAlpha by animateFloatAsState(targetValue = if (isExpanded) 1f else 0f, label = "textAlpha")
            Row(modifier = Modifier.alpha(textAlpha)) {
                Spacer(modifier = Modifier.width(12.dp))
                val displayTitle = getLocalizedSidebarTitle(item)
                Text(
                    text = displayTitle,
                    fontSize = if (isMainGroup) 16.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun getLocalizedSidebarTitle(item: SidebarItem): String {
    return when (item.itemId) {
        "home" -> stringResource(Res.string.nav_home)
        "movies" -> stringResource(Res.string.nav_movies)
        "tv_shows" -> stringResource(Res.string.nav_tv_shows)
        "trends" -> stringResource(Res.string.nav_trends)
        "search" -> stringResource(Res.string.nav_search)
        "collection" -> stringResource(Res.string.nav_my_collection)
        else -> item.title ?: ""
    }
}

@Composable
fun SidebarActionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    isFocusable: Boolean,
    onClick: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val isTv = LocalDeviceTarget.current.isTv

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f, 
                shape = RoundedCornerShape(8.dp),
                focusEnabled = !isTv || isFocusable,
                onClick = onClick
            )
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            val textAlpha by animateFloatAsState(targetValue = if (isExpanded) 1f else 0f, label = "textAlpha")
            Row(modifier = Modifier.alpha(textAlpha)) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
