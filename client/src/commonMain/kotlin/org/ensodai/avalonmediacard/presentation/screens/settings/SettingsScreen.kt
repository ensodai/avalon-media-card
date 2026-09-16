package org.ensodai.avalonmediacard.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.model.TitleDisplayMode
import org.ensodai.avalonmediacard.data.AppLocales
import org.ensodai.avalonmediacard.data.LanguageDescriptor
import org.ensodai.avalonmediacard.data.UiModeOverride
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonButton
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenuItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTextField
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.PopupAnchorSide
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.rememberSideAnchorPopupPositionProvider
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.LocalTvDrawerState
import org.ensodai.avalonmediacard.presentation.screens.settings.components.SettingsLanguageDrawerScreen
import org.ensodai.avalonmediacard.presentation.screens.settings.components.getSettingsLanguageLabel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinInject()
) {
    val state by viewModel.viewState.collectAsState()
    val actions = viewModel.actions
    val deviceTarget = LocalDeviceTarget.current
    val tvDrawerState = LocalTvDrawerState.current

    val appLanguageFocusRequester = remember { FocusRequester() }
    val uiModeFocusRequester = remember { FocusRequester() }
    val posterLanguageFocusRequester = remember { FocusRequester() }
    val titleModeFocusRequester = remember { FocusRequester() }
    val overviewLanguageFocusRequester = remember { FocusRequester() }
    val tmdbTokenFocusRequester = remember { FocusRequester() }
    val saveButtonFocusRequester = remember { FocusRequester() }

    var isSelectingAppLanguageDropdown by remember { mutableStateOf(false) }
    var isSelectingPosterLanguageDropdown by remember { mutableStateOf(false) }
    var isSelectingTitleModeDropdown by remember { mutableStateOf(false) }
    var isSelectingOverviewLanguageDropdown by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        runCatching { appLanguageFocusRequester.requestFocus() }
    }

    val selectedAppLanguageText = getSettingsLanguageLabel(state.uiLocale, isMediaOption = false)
    val selectedPosterLanguageText = getSettingsLanguageLabel(state.posterLanguage ?: "auto", isMediaOption = true)
    val selectedTitleModeText = getSettingsLanguageLabel(
        state.titleLanguage ?: if (state.titleMode == TitleDisplayMode.ORIGINAL) "original" else "auto",
        isMediaOption = true
    )
    val selectedOverviewLanguageText = getSettingsLanguageLabel(state.overviewLanguage ?: "auto", isMediaOption = true)

    val posterOptions = remember {
        listOf(
            LanguageDescriptor("auto", "As in app", "Auto"),
            LanguageDescriptor("original", "Original / Textless", "Original")
        ) + AppLocales.supported.filter { it.code != "auto" }
    }

    val titleOptions = remember {
        listOf(
            LanguageDescriptor("auto", "As in app", "Auto"),
            LanguageDescriptor("original", "Original", "Original")
        ) + AppLocales.supported.filter { it.code != "auto" }
    }

    val overviewOptions = remember {
        listOf(
            LanguageDescriptor("auto", "As in app", "Auto")
        ) + AppLocales.supported.filter { it.code != "auto" }
    }

    val appLangTitle = stringResource(Res.string.settings_language)
    val uiModeTitle = stringResource(Res.string.settings_ui_mode)
    val posterLangTitle = stringResource(Res.string.settings_poster_language)
    val titleModeTitle = stringResource(Res.string.settings_title_mode)
    val overviewLangTitle = stringResource(Res.string.settings_overview_language)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.settings_subtitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Card 1: Основные настройки (General Settings)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(Res.string.settings_section_general),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Язык интерфейса
                    SettingsRowItem(
                        title = appLangTitle,
                        value = selectedAppLanguageText,
                        icon = Lucide.Languages,
                        focusRequester = appLanguageFocusRequester,
                        onClick = {
                            if (deviceTarget.isTv) {
                                tvDrawerState.open(
                                    SettingsLanguageDrawerScreen(
                                        key = "settings_app_language",
                                        title = appLangTitle,
                                        icon = Lucide.Languages,
                                        callerFocusRequester = appLanguageFocusRequester,
                                        options = AppLocales.supported,
                                        selectedCode = state.uiLocale,
                                        isMediaOption = false,
                                        onSelected = actions.onLanguageSelected
                                    ),
                                    caller = appLanguageFocusRequester
                                )
                            } else {
                                isSelectingAppLanguageDropdown = true
                            }
                        },
                        dropdownContent = {
                            if (!deviceTarget.isTv) {
                                SettingsLanguageDropdown(
                                    expanded = isSelectingAppLanguageDropdown,
                                    options = AppLocales.supported,
                                    isMediaOption = false,
                                    onDismiss = { isSelectingAppLanguageDropdown = false },
                                    onSelected = actions.onLanguageSelected
                                )
                            }
                        }
                    )

                    // Режим интерфейса (Сегментированный селектор)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = uiModeTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        UiModeSegmentedSelector(
                            currentMode = state.uiModeOverride,
                            onSelect = actions.onUiModeSelected,
                            focusRequester = uiModeFocusRequester
                        )
                    }
                }

                // Card 2: TMDB и локализация контента (Media Settings)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Globe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(Res.string.settings_section_media),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Язык постеров
                    SettingsRowItem(
                        title = posterLangTitle,
                        value = selectedPosterLanguageText,
                        icon = Lucide.Image,
                        focusRequester = posterLanguageFocusRequester,
                        onClick = {
                            if (deviceTarget.isTv) {
                                tvDrawerState.open(
                                    SettingsLanguageDrawerScreen(
                                        key = "settings_poster_language",
                                        title = posterLangTitle,
                                        icon = Lucide.Image,
                                        callerFocusRequester = posterLanguageFocusRequester,
                                        options = posterOptions,
                                        selectedCode = state.posterLanguage ?: "auto",
                                        isMediaOption = true,
                                        onSelected = { code ->
                                            actions.onPosterLanguageSelected(if (code == "auto") null else code)
                                        }
                                    ),
                                    caller = posterLanguageFocusRequester
                                )
                            } else {
                                isSelectingPosterLanguageDropdown = true
                            }
                        },
                        dropdownContent = {
                            if (!deviceTarget.isTv) {
                                SettingsLanguageDropdown(
                                    expanded = isSelectingPosterLanguageDropdown,
                                    options = posterOptions,
                                    isMediaOption = true,
                                    onDismiss = { isSelectingPosterLanguageDropdown = false },
                                    onSelected = { code ->
                                        actions.onPosterLanguageSelected(if (code == "auto") null else code)
                                    }
                                )
                            }
                        }
                    )

                    // Язык названий
                    SettingsRowItem(
                        title = titleModeTitle,
                        value = selectedTitleModeText,
                        icon = Lucide.Film,
                        focusRequester = titleModeFocusRequester,
                        onClick = {
                            if (deviceTarget.isTv) {
                                tvDrawerState.open(
                                    SettingsLanguageDrawerScreen(
                                        key = "settings_title_mode",
                                        title = titleModeTitle,
                                        icon = Lucide.Film,
                                        callerFocusRequester = titleModeFocusRequester,
                                        options = titleOptions,
                                        selectedCode = state.titleLanguage ?: if (state.titleMode == TitleDisplayMode.ORIGINAL) "original" else "auto",
                                        isMediaOption = true,
                                        onSelected = { code ->
                                            actions.onTitleLanguageSelected(if (code == "auto") null else code)
                                        }
                                    ),
                                    caller = titleModeFocusRequester
                                )
                            } else {
                                isSelectingTitleModeDropdown = true
                            }
                        },
                        dropdownContent = {
                            if (!deviceTarget.isTv) {
                                SettingsLanguageDropdown(
                                    expanded = isSelectingTitleModeDropdown,
                                    options = titleOptions,
                                    isMediaOption = true,
                                    onDismiss = { isSelectingTitleModeDropdown = false },
                                    onSelected = { code ->
                                        actions.onTitleLanguageSelected(if (code == "auto") null else code)
                                    }
                                )
                            }
                        }
                    )

                    // Язык описания (синопсиса)
                    SettingsRowItem(
                        title = overviewLangTitle,
                        value = selectedOverviewLanguageText,
                        icon = Lucide.BookOpen,
                        focusRequester = overviewLanguageFocusRequester,
                        onClick = {
                            if (deviceTarget.isTv) {
                                tvDrawerState.open(
                                    SettingsLanguageDrawerScreen(
                                        key = "settings_overview_language",
                                        title = overviewLangTitle,
                                        icon = Lucide.BookOpen,
                                        callerFocusRequester = overviewLanguageFocusRequester,
                                        options = overviewOptions,
                                        selectedCode = state.overviewLanguage ?: "auto",
                                        isMediaOption = true,
                                        onSelected = { code ->
                                            actions.onOverviewLanguageSelected(if (code == "auto") null else code)
                                        }
                                    ),
                                    caller = overviewLanguageFocusRequester
                                )
                            } else {
                                isSelectingOverviewLanguageDropdown = true
                            }
                        },
                        dropdownContent = {
                            if (!deviceTarget.isTv) {
                                SettingsLanguageDropdown(
                                    expanded = isSelectingOverviewLanguageDropdown,
                                    options = overviewOptions,
                                    isMediaOption = true,
                                    onDismiss = { isSelectingOverviewLanguageDropdown = false },
                                    onSelected = { code ->
                                        actions.onOverviewLanguageSelected(if (code == "auto") null else code)
                                    }
                                )
                            }
                        }
                    )
                }

                // Card 3: API-токен TMDB
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(Res.string.settings_tmdb_token_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val isConfigured = !state.tmdbReadToken.isNullOrBlank()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isConfigured) Color(0xFF10B981).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.dp,
                                    if (isConfigured) Color(0xFF10B981).copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isConfigured) stringResource(Res.string.admin_integrations_configured) else stringResource(Res.string.admin_integrations_not_set),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(Res.string.settings_tmdb_token_description),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AvalonTextField(
                        value = state.tmdbReadToken ?: "",
                        onValueChange = { actions.onTmdbTokenChanged(it.ifBlank { null }) },
                        placeholder = stringResource(Res.string.settings_tmdb_token_placeholder),
                        leadingIcon = {
                            Icon(
                                imageVector = Lucide.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        focusRequester = tmdbTokenFocusRequester,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvalonButton(
                            text = stringResource(Res.string.settings_btn_save),
                            onClick = actions.onSaveClicked,
                            isLoading = state.isLoading,
                            modifier = Modifier.focusRequester(saveButtonFocusRequester)
                        )
                    }

                    if (state.error != null) {
                        Text(text = state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    if (state.successMessage != null) {
                        Text(text = state.successMessage!!, color = Color(0xFF10B981), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRowItem(
    title: String,
    value: String,
    icon: ImageVector,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dropdownContent: (@Composable () -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .tvAndWebHoverEffect(
                    shape = RoundedCornerShape(12.dp),
                    clickEnabled = true,
                    onClick = onClick,
                    activeBorderColor = Color.White,
                    activeBorderWidth = 1.5.dp
                )
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        dropdownContent?.invoke()
    }
}

@Composable
private fun SettingsLanguageDropdown(
    expanded: Boolean,
    options: List<LanguageDescriptor>,
    isMediaOption: Boolean,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    if (!expanded) return
    val positionProvider = rememberSideAnchorPopupPositionProvider(preferredSide = PopupAnchorSide.RIGHT)
    AvalonDropdownMenu(
        expanded = true,
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        width = 260.dp
    ) {
        options.forEach { lang ->
            val icon = when (lang.code) {
                "auto" -> Lucide.Globe
                "original" -> Lucide.Film
                else -> Lucide.Languages
            }
            AvalonDropdownMenuItem(
                text = getSettingsLanguageLabel(lang.code, isMediaOption = isMediaOption),
                icon = icon,
                onClick = {
                    onSelected(lang.code)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun UiModeSegmentedSelector(
    currentMode: UiModeOverride,
    onSelect: (UiModeOverride) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        Triple(UiModeOverride.AUTO, Res.string.settings_ui_mode_auto, Lucide.Zap),
        Triple(UiModeOverride.TV, Res.string.settings_ui_mode_tv, Lucide.Tv),
        Triple(UiModeOverride.PC, Res.string.settings_ui_mode_pc, Lucide.Monitor)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        modes.forEach { (mode, titleRes, icon) ->
            val isSelected = currentMode == mode
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(if (isSelected) Modifier.focusRequester(focusRequester) else Modifier)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .tvAndWebHoverEffect(
                        scaleTarget = 1.02f,
                        shape = RoundedCornerShape(8.dp),
                        clickEnabled = true,
                        onClick = { onSelect(mode) },
                        activeBorderColor = Color.White,
                        activeBorderWidth = 1.5.dp
                    )
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(titleRes),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
