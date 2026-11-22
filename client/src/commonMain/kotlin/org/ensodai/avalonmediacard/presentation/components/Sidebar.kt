package org.ensodai.avalonmediacard.presentation.components

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.ensodai.avalonmediacard.designsystem.components.AvalonDivider
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenuItem
import org.ensodai.avalonmediacard.designsystem.components.AvalonPopupAnchorSide
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.rememberSideAnchorPopupPositionProvider
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalContentFocusRequester
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
    modifier: Modifier = Modifier,
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
    userRole: UserRole? = null,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    val outlineColor = AvalonTheme.colors.borderSubtle
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
    val contentFocusRequester = LocalContentFocusRequester.current
    AvalonBackHandler(enabled = isExpanded) {
        val moved = focusManager.moveFocus(FocusDirection.Right)
        if (!moved) {
            runCatching { contentFocusRequester.requestFocus() }
        }
        isHovered = false
    }

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(AvalonTheme.colors.bgSidebar)
            .onFocusChanged {
                hasFocus = it.hasFocus
                if (it.hasFocus) {
                    logger.d { "[FOCUS_DEBUG] Sidebar GOT FOCUS! (isFocused=${it.isFocused})" }
                } else {
                    logger.d { "[FOCUS_DEBUG] Sidebar LOST FOCUS!" }
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

                    val deviceTarget = LocalDeviceTarget.current

                    if (!deviceTarget.isTv) {
                        val positionProvider = rememberSideAnchorPopupPositionProvider(preferredSide = AvalonPopupAnchorSide.RIGHT)
                        AvalonDropdownMenu(
                            expanded = isProfileMenuExpanded,
                            onDismissRequest = { isProfileMenuExpanded = false },
                            popupPositionProvider = positionProvider,
                            width = 250.dp
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
                AvalonText(
                    text = uploadStatus,
                    fontSize = 12.sp,
                    color = if (uploadStatus.contains("Error", ignoreCase = true) || uploadStatus.contains("Ошибка", ignoreCase = true)) AvalonTheme.colors.statusDanger
                    else AvalonTheme.colors.accentPrimary,
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
        AvalonDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = AvalonTheme.colors.borderSubtle
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
        "bell" -> Lucide.Bell
        "users" -> Lucide.Users
        "stats", "chart" -> Lucide.ChartColumn
        else -> if (isMainGroup) Lucide.LayoutGrid else Lucide.Hash
    }

    val contentColor = if (isSelected) AvalonTheme.colors.textPrimary
    else AvalonTheme.colors.textSecondary

    val isTv = LocalDeviceTarget.current.isTv
    val focusManager = LocalFocusManager.current
    val contentFocusRequester = LocalContentFocusRequester.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f, 
                shape = RoundedCornerShape(8.dp),
                focusEnabled = !isTv || isExpanded || isSelected,
                onClick = { 
                    onSelected(item)
                    val moved = focusManager.moveFocus(FocusDirection.Right)
                    if (!moved) {
                        runCatching { contentFocusRequester.requestFocus() }
                    }
                }
            )
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) AvalonTheme.colors.bgElevated
                else Color.Transparent
            )
            
            .padding(vertical = if (isMainGroup) 12.dp else 10.dp, horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvalonIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (isMainGroup) 20.dp else 18.dp),
                tint = contentColor
            )
            val textAlpha by animateFloatAsState(targetValue = if (isExpanded) 1f else 0f, label = "textAlpha")
            Row(modifier = Modifier.alpha(textAlpha)) {
                Spacer(modifier = Modifier.width(12.dp))
                val displayTitle = getLocalizedSidebarTitle(item)
                AvalonText(
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
        "episodes_notifications" -> stringResource(Res.string.episodes_notifications_title)
        "watch_rooms" -> stringResource(Res.string.nav_watch_rooms)
        "stats" -> stringResource(Res.string.nav_stats)
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
    val contentColor = AvalonTheme.colors.textSecondary
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
            AvalonIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            val textAlpha by animateFloatAsState(targetValue = if (isExpanded) 1f else 0f, label = "textAlpha")
            Row(modifier = Modifier.alpha(textAlpha)) {
                Spacer(modifier = Modifier.width(12.dp))
                AvalonText(
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
