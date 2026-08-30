package org.ensodai.avalonmediacard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.rpc.ActionRpcService
import org.ensodai.avalonmediacard.contract.rpc.AuthRpcService
import org.ensodai.avalonmediacard.contract.rpc.SduiRpcService
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.data.TokenStorage
import org.ensodai.avalonmediacard.presentation.components.Sidebar
import org.ensodai.avalonmediacard.presentation.components.getLocalizedSidebarTitle
import org.ensodai.avalonmediacard.presentation.navigation.*
import org.ensodai.avalonmediacard.presentation.screens.admin.AdminScreen
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.*
import org.ensodai.avalonmediacard.presentation.screens.customListScreen.CustomListScreen
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.DashboardScreen
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.DetailsScreen
import org.ensodai.avalonmediacard.presentation.screens.dynamic.DynamicScreen
import org.ensodai.avalonmediacard.presentation.screens.integrations.IntegrationsScreen
import org.ensodai.avalonmediacard.presentation.screens.mediaScreen.MediaListScreen
import org.ensodai.avalonmediacard.presentation.screens.moviesScreen.MoviesScreen
import org.ensodai.avalonmediacard.presentation.screens.myCollectionScreen.MyCollectionScreen
import org.ensodai.avalonmediacard.presentation.screens.person.PersonScreen
import org.ensodai.avalonmediacard.presentation.screens.search.SearchScreen
import org.ensodai.avalonmediacard.presentation.screens.settings.SettingsScreen
import org.ensodai.avalonmediacard.presentation.screens.trendsScreen.TrendsScreen
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.screens.tvShowsScreen.TvShowsScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

sealed class RootTab {
    data class Plugin(val item: SidebarItem) : RootTab()
    data object Settings : RootTab()
    data object Integrations : RootTab()
    data object Admin : RootTab()
}

@Composable
fun MainAppContent(
    sidebarItems: List<SidebarItem>,
    tokenStorage: TokenStorage,
    sduiRpcService: SduiRpcService,
    actionRpcService: ActionRpcService,
    dialogManager: DialogManager,
    configuration: SavedStateConfiguration,
    uploadStatus: String,
    onUploadStatusChange: (String) -> Unit,
    userRole: UserRole? = null,
    scope: CoroutineScope
) {
    val focusManager = LocalFocusManager.current
    val activeTabState = remember { mutableStateOf<RootTab?>(null) }
    val rootOverlayState = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    LaunchedEffect(sidebarItems) {
        if (activeTabState.value == null && sidebarItems.isNotEmpty()) {
            activeTabState.value = RootTab.Plugin(sidebarItems.first())
        }
    }

    val authRpcService = koinInject<AuthRpcService>()
    LaunchedEffect(Unit) {
        try {
            authRpcService.getIntegrationsStatus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val saveableStateHolder = rememberSaveableStateHolder()
    val canGoBackState = remember { mutableStateOf(false) }
    val activePopActionState = remember { mutableStateOf<(() -> Unit)?>(null) }
    val isSidebarExpandedState = remember { mutableStateOf(false) }
    val tvDrawerState = remember { TvDrawerState() }
    val tabNavControllers = remember { mutableMapOf<String, AvalonNavController<ScreenKey>>() }

    val deviceTarget = LocalDeviceTarget.current

    val currentSidebarItems by rememberUpdatedState(sidebarItems)

    val mainLayout: @Composable () -> Unit = remember {
        @Composable {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 104.dp, top = 8.dp, end = 24.dp, bottom = 0.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    val isSystemScreen =
                        activeTabState.value is RootTab.Settings || activeTabState.value is RootTab.Integrations || activeTabState.value is RootTab.Admin
                    if (currentSidebarItems.isEmpty() && !isSystemScreen) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(Res.string.plugins_empty_title),
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(Res.string.plugins_empty_desc),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        val activeTabId = when (val tab = activeTabState.value) {
                            is RootTab.Admin -> "admin"
                            is RootTab.Settings -> "settings"
                            is RootTab.Integrations -> "integrations"
                            is RootTab.Plugin -> tab.item.itemId
                            null -> ""
                        }

                        val initializedTabs = remember { androidx.compose.runtime.mutableStateListOf<String>() }

                        if (activeTabId.isNotEmpty() && !initializedTabs.contains(activeTabId)) {
                            initializedTabs.add(activeTabId)
                        }

                        val validTabIds = androidx.compose.runtime.remember(currentSidebarItems) {
                            currentSidebarItems.map { it.itemId }.toSet() + setOf("admin", "settings", "integrations")
                        }
                        androidx.compose.runtime.LaunchedEffect(validTabIds) {
                            initializedTabs.retainAll(validTabIds)
                        }

                        initializedTabs.forEach { tabId ->
                            val isTabActive = tabId == activeTabId

                            androidx.compose.runtime.key(tabId) {
                                val visualManager =
                                    remember { org.ensodai.avalonmediacard.presentation.navigation.VisualStateManager<ScreenKey>() }

                                saveableStateHolder.SaveableStateProvider(tabId) {
                                    val targetScreen = when (tabId) {
                                        "admin" -> Screen.Admin
                                        "settings" -> Screen.Settings
                                        "integrations" -> Screen.Integrations
                                        else -> currentSidebarItems.find { it.itemId == tabId }?.screen
                                            ?: Screen.Dashboard
                                    }

                                    @Suppress("UNCHECKED_CAST")
                                    val backStack =
                                        rememberNavBackStack(
                                            configuration,
                                            ScreenKey(targetScreen)
                                        ) as NavBackStack<ScreenKey>
                                    val navController = rememberAvalonNavController(backStack)
                                    val navigation = remember(navController) { NavigationAvalon(navController) }

                                    DisposableEffect(tabId, navController) {
                                        tabNavControllers[tabId] = navController
                                        onDispose {
                                            tabNavControllers.remove(tabId)
                                        }
                                    }

                                    LaunchedEffect(navController.canPop, isTabActive) {
                                        if (isTabActive) canGoBackState.value = navController.canPop
                                    }
                                    LaunchedEffect(navController, isTabActive) {
                                        if (isTabActive) activePopActionState.value = { navController.pop() }
                                    }

                                    val actionHandler = remember(actionRpcService, navigation, dialogManager, scope) {
                                        AppActionHandler(
                                            actionRpcService = actionRpcService,
                                            navigation = navigation,
                                            dialogManager = dialogManager,
                                            scope = scope,
                                            onStatusChange = onUploadStatusChange
                                        )
                                    }

                                    CompositionLocalProvider(
                                        LocalAvalonNavController provides navController,
                                        LocalNavigation provides navigation
                                    ) {
                                        AvalonBackHandler(enabled = isTabActive && navController.canPop && !isSidebarExpandedState.value && !tvDrawerState.isOpen) {
                                            navController.pop()
                                        }
                                        if (isTabActive) {
                                            AvalonNavHost(
                                                controller = navController,
                                                manager = visualManager
                                            ) { screenKey ->
                                                val handleAction = actionHandler::handleAction
                                                when (val screen = screenKey.screen) {
                                                    is Screen.Dashboard, is Screen.PluginHome -> {
                                                        DashboardScreen(
                                                            screen = screen,
                                                            title = currentSidebarItems.find { it.itemId == tabId }?.let { getLocalizedSidebarTitle(it) }
                                                                ?: ""
                                                        )
                                                    }

                                                    is Screen.Movies -> {
                                                        MoviesScreen(
                                                            screen = screen,
                                                            title = currentSidebarItems.find { it.itemId == tabId }?.let { getLocalizedSidebarTitle(it) }
                                                                ?: ""
                                                        )
                                                    }

                                                    is Screen.TvShows -> {
                                                        TvShowsScreen(
                                                            screen = screen,
                                                            title = currentSidebarItems.find { it.itemId == tabId }?.let { getLocalizedSidebarTitle(it) }
                                                                ?: ""
                                                        )
                                                    }

                                                    is Screen.Trends -> {
                                                        TrendsScreen(
                                                            screen = screen,
                                                            title = currentSidebarItems.find { it.itemId == tabId }?.let { getLocalizedSidebarTitle(it) }
                                                                ?: ""
                                                        )
                                                    }

                                                    is Screen.Search -> {
                                                        SearchScreen(
                                                            screen = screen
                                                        )
                                                    }

                                                    is Screen.MediaList -> {
                                                        MediaListScreen(
                                                            screen = screen
                                                        )
                                                    }

                                                    is Screen.MyCollection -> {
                                                        val expectedCount =
                                                            currentSidebarItems.find { it.itemId == tabId }?.itemsCount
                                                        MyCollectionScreen(expectedItemsCount = expectedCount)
                                                    }

                                                    is Screen.MovieDetails -> {
                                                        DetailsScreen(
                                                            mediaKey = screen.key
                                                        )
                                                    }

                                                    is Screen.TvShowDetails -> {
                                                        DetailsScreen(
                                                            mediaKey = screen.key
                                                        )
                                                    }

                                                    is Screen.Person -> {
                                                        PersonScreen(
                                                            screen = screen
                                                        )
                                                    }

                                                    is Screen.Dynamic -> {
                                                        DynamicScreen(
                                                            screen = screen
                                                        )
                                                    }

                                                    is Screen.CustomList -> {
                                                        val expectedCount =
                                                            currentSidebarItems.find { it.itemId == tabId }?.itemsCount
                                                        CustomListScreen(
                                                            screen = screen,
                                                            expectedItemsCount = expectedCount
                                                        )
                                                    }

                                                    is Screen.Settings -> {
                                                        SettingsScreen()
                                                    }

                                                    is Screen.Integrations -> {
                                                        IntegrationsScreen()
                                                    }

                                                    is Screen.Admin -> {
                                                        AdminScreen()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Sidebar(
                    sidebarItems = currentSidebarItems,
                    selectedItem = (activeTabState.value as? RootTab.Plugin)?.item,
                    onSelected = { item ->
                        val currentTab = activeTabState.value
                        if (currentTab is RootTab.Plugin && currentTab.item.itemId == item.itemId) {
                            tabNavControllers[item.itemId]?.popToRoot()
                        } else {
                            activeTabState.value = RootTab.Plugin(item)
                        }
                    },
                    uploadStatus = uploadStatus,
                    onUploadFinished = { success, message ->
                        onUploadStatusChange(message)
                    },
                    canGoBack = canGoBackState.value,
                    onBack = { activePopActionState.value?.invoke() },
                    onLogout = {
                        scope.launch { tokenStorage.clearToken() }
                    },
                    onSettingsClick = {
                        if (activeTabState.value is RootTab.Settings) {
                            tabNavControllers["settings"]?.popToRoot()
                        } else {
                            activeTabState.value = RootTab.Settings
                        }
                    },
                    onIntegrationsClick = {
                        if (activeTabState.value is RootTab.Integrations) {
                            tabNavControllers["integrations"]?.popToRoot()
                        } else {
                            activeTabState.value = RootTab.Integrations
                        }
                    },
                    onAdminClick = {
                        if (activeTabState.value is RootTab.Admin) {
                            tabNavControllers["admin"]?.popToRoot()
                        } else {
                            activeTabState.value = RootTab.Admin
                        }
                    },
                    userRole = userRole,
                    onExpandedChange = { isSidebarExpandedState.value = it }
                )
            }
        }
    }

    CompositionLocalProvider(
        LocalTvDrawerState provides tvDrawerState,
        LocalRootOverlay provides rootOverlayState
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val appContent = @Composable {
                AvalonTvRightDrawerHost(state = tvDrawerState) {
                    mainLayout()
                    rootOverlayState.value?.invoke()
                }
            }

            if (deviceTarget.isTv) {
                TvFocusManagerProvider {
                    appContent()
                }
            } else {
                appContent()
            }
        }
    }
}
