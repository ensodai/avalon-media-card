package org.ensodai.avalonmediacard.presentation


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.ensodai.avalonmediacard.contract.auth.LoginRequest
import org.ensodai.avalonmediacard.contract.auth.RegisterRequest
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.rpc.ActionRpcService
import org.ensodai.avalonmediacard.contract.rpc.AuthRpcService
import org.ensodai.avalonmediacard.contract.rpc.SduiRpcService
import org.ensodai.avalonmediacard.contract.slot.RefreshIntegrationsCommand
import org.ensodai.avalonmediacard.core.clearUrlQueryParameters
import org.ensodai.avalonmediacard.core.getUrlQueryParameters
import org.ensodai.avalonmediacard.data.AppSettingsStorage
import org.ensodai.avalonmediacard.data.TokenStorage
import org.ensodai.avalonmediacard.data.UiModeOverride
import org.ensodai.avalonmediacard.data.repository.GlobalManifestRepository
import org.ensodai.avalonmediacard.data.rpc.RpcConnectionManager
import org.ensodai.avalonmediacard.presentation.locale.setAppLocale
import org.ensodai.avalonmediacard.presentation.navigation.ScreenKey
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.DeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.login.LoginScreen
import org.ensodai.avalonmediacard.presentation.telemetry.LocalTelemetryTracker
import org.ensodai.avalonmediacard.presentation.telemetry.TelemetryTracker
import org.koin.compose.koinInject
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.getString

private val logger = AppLogging.logger("App")

@Composable
fun App() {
    val sduiRpcService = koinInject<SduiRpcService>()
    val actionRpcService = koinInject<ActionRpcService>()
    val tokenStorage = koinInject<TokenStorage>()
    val authRpcService = koinInject<AuthRpcService>()
    val connectionManager = koinInject<RpcConnectionManager>()
    val dialogManager = koinInject<DialogManager>()
    val telemetryTracker = koinInject<TelemetryTracker>()
    val appSettingsStorage = koinInject<AppSettingsStorage>()

    val tokenState by tokenStorage.token.collectAsState()
    val userRoleState by tokenStorage.userRole.collectAsState()
    val isLoaded by tokenStorage.isLoaded.collectAsState()
    val currentLanguage by appSettingsStorage.language.collectAsState(initial = "auto")
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentLanguage) {
        setAppLocale(currentLanguage)
    }

    val customColorScheme = darkColorScheme(
        background = Color(0xFF000000),         // Абсолютный черный (OLED) - `--bg-base`
        surface = Color(0xFF0A0A0A),            // Подложки 1 уровня - `--bg-surface-1`
        surfaceVariant = Color(0xFF141414),     // Сайдбар, Карточки фильмов - `--bg-surface-2`
        primary = Color.White,                  // Акцентный цвет (Белый)
        onPrimary = Color(0xFF000000),          // Текст на белых кнопках (Черный)
        primaryContainer = Color(0xFF1F1F1F),   // Hover-состояния карточек, активный пункт меню - `--bg-surface-3`
        onPrimaryContainer = Color.White,       // Текст активного пункта меню (Белый)
        secondary = Color.White,
        secondaryContainer = Color(0xFF141414), // Фон карточек виджетов - `--bg-surface-2`
        onSecondaryContainer = Color(0xFFA1A1AA), // `--text-secondary`
        onBackground = Color(0xFFFFFFFF),       // Основной яркий белый text - `--text-primary`
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFA1A1AA),   // Нейтральный серый: описания, роли, год, жанр - `--text-secondary`
        outline = Color(0xFF27272A),            // Разделители блоков, рамки - `--border-subtle`
        outlineVariant = Color(0xFF3F3F46)      // Состояние фокуса / границы кнопок - `--border-focus`
    )

    key(currentLanguage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        color = Color.Transparent,
                        size = this.size,
                        blendMode = BlendMode.Clear
                    )
                }
        ) {
            MaterialTheme(colorScheme = customColorScheme) {
            val scope = rememberCoroutineScope()
            var sidebarItems by remember { mutableStateOf<List<SidebarItem>>(emptyList()) }
            var isSidebarLoaded by remember { mutableStateOf(false) }
            var selectedItem by remember { mutableStateOf<SidebarItem?>(null) }
            var previousItem by remember { mutableStateOf<SidebarItem?>(null) }
            val tabBackStacks = remember { mutableStateMapOf<String, List<ScreenKey>>() }
            var uploadStatus by remember { mutableStateOf("") }

            if (!isLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
                return@MaterialTheme
            }

            if (tokenState == null) {
                LoginScreen(
                    initialServerUrl = tokenStorage.cachedServerUrl ?: org.ensodai.avalonmediacard.data.platformServerUrl,
                    onLoginSuccess = { authResponse ->
                        val token = authResponse.token
                        if (!token.isNullOrBlank()) {
                            scope.launch {
                                tokenStorage.saveToken(
                                    token = token,
                                    role = authResponse.role,
                                    userId = authResponse.userId,
                                    username = authResponse.username
                                )
                            }
                        }
                    },
                    onLoginClick = { username, password, url ->
                        runCatching {
                            tokenStorage.saveServerUrl(url)
                            connectionManager.clearConnection()
                            authRpcService.login(LoginRequest(username, password))
                        }.mapCatching { response ->
                            if (response.success && !response.token.isNullOrBlank()) {
                                response
                            } else {
                                throw Exception(response.error ?: "Login failed")
                            }
                        }
                    },
                    onRegisterClick = { username, password, url ->
                        runCatching {
                            tokenStorage.saveServerUrl(url)
                            connectionManager.clearConnection()
                            authRpcService.register(RegisterRequest(username, password))
                        }.mapCatching { response ->
                            if (response.success && !response.token.isNullOrBlank()) {
                                response
                            } else {
                                throw Exception(response.error ?: "Registration failed")
                            }
                        }
                    }
                )
                return@MaterialTheme
            }

            val manifestRepository = koinInject<GlobalManifestRepository>()
            val isManifestLoaded by manifestRepository.isLoaded.collectAsState()

            LaunchedEffect(tokenState) {
                connectionManager.clearConnection()
                if (tokenState != null) {
                    launch {
                        try {
                            manifestRepository.refreshManifest()
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            e.printStackTrace()
                        }
                    }
                    launch {
                        try {
                            sduiRpcService.streamSidebar().collect { components ->
                                logger.d { "[CLIENT_SIDEBAR_LOG] streamSidebar collected ${components.size} items: ${components.map { it.title ?: it.itemId }}" }
                                val items = components
                                sidebarItems = items
                                isSidebarLoaded = true
                                val currentSelected = selectedItem
                                if (currentSelected == null) {
                                    selectedItem = items.firstOrNull()
                                } else {
                                    val matching = items.firstOrNull { it.itemId == currentSelected.itemId }
                                    if (matching != null) {
                                        selectedItem = matching
                                    } else {
                                        selectedItem = items.firstOrNull()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            e.printStackTrace()
                        }
                    }

                    // Перехват OAuth параметров из URL
                    launch {
                        val params = getUrlQueryParameters()
                        val code = params["code"]
                        val state = params["state"]
                        if (!code.isNullOrBlank() && !state.isNullOrBlank()) {
                            try {
                                val success = authRpcService.exchangeOAuthCode(service = state, code = code)
                                if (success) {
                                    uploadStatus = getString(Res.string.nav_oauth_success, state.replaceFirstChar { it.uppercase() })
                                    try {
                                        actionRpcService.handleAction(RefreshIntegrationsCommand(service = state))
                                    } catch (e: Exception) {
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        e.printStackTrace()
                                    }
                                } else {
                                    uploadStatus = getString(Res.string.nav_oauth_error, state)
                                }
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                uploadStatus = getString(Res.string.nav_oauth_auth_error, e.message ?: "")
                            } finally {
                                clearUrlQueryParameters()
                            }
                        }
                    }
                } else {
                    sidebarItems = emptyList()
                    selectedItem = null
                    previousItem = null
                    isSidebarLoaded = false
                    tabBackStacks.clear()
                    uploadStatus = ""
                    manifestRepository.clear()
                }
            }

            if (!isManifestLoaded || !isSidebarLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
                return@MaterialTheme
            }

            val configuration = remember {
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(ScreenKey::class)
                        }
                    }
                }
            }
            
            val uiModeOverride by appSettingsStorage.uiModeOverride.collectAsState()
            val baseTarget = LocalDeviceTarget.current
            
            val finalTarget = when (uiModeOverride) {
                UiModeOverride.AUTO -> baseTarget
                UiModeOverride.TV -> if (baseTarget == DeviceTarget.ANDROID_MOBILE || baseTarget == DeviceTarget.ANDROID_TV) DeviceTarget.ANDROID_TV else DeviceTarget.TV_WEB
                UiModeOverride.PC -> DeviceTarget.DESKTOP_WEB
            }

            CompositionLocalProvider(
                LocalTelemetryTracker provides telemetryTracker,
                LocalDeviceTarget provides finalTarget
            ) {
                MainAppContent(
                    sidebarItems = sidebarItems,
                    tokenStorage = tokenStorage,
                    sduiRpcService = sduiRpcService,
                    actionRpcService = actionRpcService,
                    dialogManager = dialogManager,
                    configuration = configuration,
                    uploadStatus = uploadStatus,
                    onUploadStatusChange = { uploadStatus = it },
                    userRole = userRoleState,
                    scope = scope
                )
            }
        }
    }
}
}
