package org.ensodai.avalonmediacard.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenuItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinInject()
) {
    val state by viewModel.viewState.collectAsState()
    val actions = viewModel.actions
    val deviceTarget = LocalDeviceTarget.current

    var isSelectingTarget by remember { mutableStateOf(false) }
    var isSelectingAppLanguage by remember { mutableStateOf(false) }
    var isSelectingPosterLanguage by remember { mutableStateOf(false) }
    var isSelectingTitleMode by remember { mutableStateOf(false) }
    var isSelectingOverviewLanguage by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val selectedTargetModeText = when (state.uiModeOverride) {
        UiModeOverride.AUTO -> stringResource(Res.string.settings_ui_mode_auto)
        UiModeOverride.TV -> stringResource(Res.string.settings_ui_mode_tv)
        UiModeOverride.PC -> stringResource(Res.string.settings_ui_mode_pc)
    }

    val selectedAppLanguageText = getLanguageLabel(state.uiLocale, isMediaOption = false)

    val selectedPosterLanguageText = getLanguageLabel(state.posterLanguage ?: "auto", isMediaOption = true)

    val selectedTitleModeText = getLanguageLabel(
        state.titleLanguage ?: if (state.titleMode == TitleDisplayMode.ORIGINAL) "original" else "auto",
        isMediaOption = true
    )

    val selectedOverviewLanguageText = getLanguageLabel(state.overviewLanguage ?: "auto", isMediaOption = true)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .focusGroup()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Lucide.Settings,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.settings_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.settings_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // === GENERAL SETTINGS === //
            Text(
                text = stringResource(Res.string.settings_section_general),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Язык интерфейса
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvAndWebHoverEffect(
                        shape = RoundedCornerShape(8.dp),
                        onClick = { isSelectingAppLanguage = true }
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.settings_language),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedAppLanguageText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Lucide.Languages,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Режим таргета UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvAndWebHoverEffect(
                        shape = RoundedCornerShape(8.dp),
                        onClick = { isSelectingTarget = true }
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.settings_ui_mode),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedTargetModeText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // === TMDB & MEDIA SETTINGS === //
            Text(
                text = stringResource(Res.string.settings_section_media),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
            )

            // Язык постеров
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvAndWebHoverEffect(
                        shape = RoundedCornerShape(8.dp),
                        onClick = { isSelectingPosterLanguage = true }
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.settings_poster_language),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedPosterLanguageText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Lucide.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Режим названий
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvAndWebHoverEffect(
                        shape = RoundedCornerShape(8.dp),
                        onClick = { isSelectingTitleMode = true }
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.settings_title_mode),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedTitleModeText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Lucide.Film,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Язык синопсиса
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvAndWebHoverEffect(
                        shape = RoundedCornerShape(8.dp),
                        onClick = { isSelectingOverviewLanguage = true }
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.settings_overview_language),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedOverviewLanguageText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Lucide.BookOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TMDB Token
            Text(
                text = stringResource(Res.string.settings_tmdb_token_title),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp)
            )

            Text(
                text = stringResource(Res.string.settings_tmdb_token_description),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = state.tmdbReadToken ?: "",
                onValueChange = { actions.onTmdbTokenChanged(it.ifBlank { null }) },
                placeholder = { Text(stringResource(Res.string.settings_tmdb_token_placeholder), fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .tvAndWebHoverEffect(
                            shape = RoundedCornerShape(8.dp),
                            onClick = actions.onSaveClicked,
                            activeBorderColor = MaterialTheme.colorScheme.primary
                        )
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Lucide.Save, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.settings_btn_save),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            if (state.successMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = state.successMessage!!, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }

        // --- ВЫБОР ЯЗЫКА ИНТЕРФЕЙСА --- //
        if (isSelectingAppLanguage) {
            LanguageSelectionModal(
                title = stringResource(Res.string.settings_language),
                options = AppLocales.supported,
                selectedCode = state.uiLocale,
                isTv = deviceTarget.isTv,
                isMediaOption = false,
                onDismiss = { isSelectingAppLanguage = false },
                onSelected = { code ->
                    actions.onLanguageSelected(code)
                    isSelectingAppLanguage = false
                }
            )
        }

        // --- ВЫБОР ЯЗЫКА ПОСТЕРОВ --- //
        if (isSelectingPosterLanguage) {
            val posterOptions = remember {
                listOf(
                    LanguageDescriptor("auto", "As in app", "Auto"),
                    LanguageDescriptor("original", "Original / Textless", "Original")
                ) + AppLocales.supported.filter { it.code != "auto" }
            }
            LanguageSelectionModal(
                title = stringResource(Res.string.settings_poster_language),
                options = posterOptions,
                selectedCode = state.posterLanguage ?: "auto",
                isTv = deviceTarget.isTv,
                isMediaOption = true,
                onDismiss = { isSelectingPosterLanguage = false },
                onSelected = { code ->
                    actions.onPosterLanguageSelected(if (code == "auto") null else code)
                    isSelectingPosterLanguage = false
                }
            )
        }

        // --- ВЫБОР ЯЗЫКА НАЗВАНИЙ --- //
        if (isSelectingTitleMode) {
            val titleOptions = remember {
                listOf(
                    LanguageDescriptor("auto", "As in app", "Auto"),
                    LanguageDescriptor("original", "Original", "Original")
                ) + AppLocales.supported.filter { it.code != "auto" }
            }
            LanguageSelectionModal(
                title = stringResource(Res.string.settings_title_mode),
                options = titleOptions,
                selectedCode = state.titleLanguage ?: if (state.titleMode == TitleDisplayMode.ORIGINAL) "original" else "auto",
                isTv = deviceTarget.isTv,
                isMediaOption = true,
                onDismiss = { isSelectingTitleMode = false },
                onSelected = { code ->
                    actions.onTitleLanguageSelected(if (code == "auto") null else code)
                    isSelectingTitleMode = false
                }
            )
        }

        // --- ВЫБОР ЯЗЫКА СИНОПСИСА --- //
        if (isSelectingOverviewLanguage) {
            val overviewOptions = remember {
                listOf(
                    LanguageDescriptor("auto", "As in app", "Auto")
                ) + AppLocales.supported.filter { it.code != "auto" }
            }
            LanguageSelectionModal(
                title = stringResource(Res.string.settings_overview_language),
                options = overviewOptions,
                selectedCode = state.overviewLanguage ?: "auto",
                isTv = deviceTarget.isTv,
                isMediaOption = true,
                onDismiss = { isSelectingOverviewLanguage = false },
                onSelected = { code ->
                    actions.onOverviewLanguageSelected(if (code == "auto") null else code)
                    isSelectingOverviewLanguage = false
                }
            )
        }

        // --- ВЫБОР РЕЖИМА ИНТЕРФЕЙСА --- //
        if (isSelectingTarget) {
            if (deviceTarget.isTv) {
                TvDrawerEffect(
                    title = stringResource(Res.string.settings_ui_mode),
                    icon = Lucide.Monitor,
                    onDismiss = { isSelectingTarget = false }
                ) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.settings_ui_mode_auto),
                                icon = Lucide.Zap,
                                isSelected = state.uiModeOverride == UiModeOverride.AUTO,
                                onClick = { 
                                    actions.onUiModeSelected(UiModeOverride.AUTO)
                                    isSelectingTarget = false 
                                }
                            )
                        }
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.settings_ui_mode_tv),
                                icon = Lucide.Tv,
                                isSelected = state.uiModeOverride == UiModeOverride.TV,
                                onClick = { 
                                    actions.onUiModeSelected(UiModeOverride.TV)
                                    isSelectingTarget = false 
                                }
                            )
                        }
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.settings_ui_mode_pc),
                                icon = Lucide.Monitor,
                                isSelected = state.uiModeOverride == UiModeOverride.PC,
                                onClick = { 
                                    actions.onUiModeSelected(UiModeOverride.PC)
                                    isSelectingTarget = false 
                                }
                            )
                        }
                    }
                }
            } else {
                val density = LocalDensity.current
                AvalonDropdownMenu(
                    expanded = isSelectingTarget,
                    alignment = Alignment.Center,
                    offset = IntOffset(0, with(density) { 0.dp.roundToPx() }),
                    onDismissRequest = { isSelectingTarget = false },
                    width = 250.dp
                ) {
                    AvalonDropdownMenuItem(
                        text = stringResource(Res.string.settings_ui_mode_auto),
                        icon = Lucide.Zap,
                        onClick = {
                            actions.onUiModeSelected(UiModeOverride.AUTO)
                            isSelectingTarget = false
                        }
                    )
                    AvalonDropdownMenuItem(
                        text = stringResource(Res.string.settings_ui_mode_tv),
                        icon = Lucide.Tv,
                        onClick = {
                            actions.onUiModeSelected(UiModeOverride.TV)
                            isSelectingTarget = false
                        }
                    )
                    AvalonDropdownMenuItem(
                        text = stringResource(Res.string.settings_ui_mode_pc),
                        icon = Lucide.Monitor,
                        onClick = {
                            actions.onUiModeSelected(UiModeOverride.PC)
                            isSelectingTarget = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun getLanguageLabel(code: String, isMediaOption: Boolean = false): String {
    return when (code) {
        "auto" -> if (isMediaOption) stringResource(Res.string.settings_language_as_in_app) else stringResource(Res.string.settings_language_auto)
        "original" -> stringResource(Res.string.settings_language_original)
        "ru" -> stringResource(Res.string.settings_language_ru)
        "en" -> stringResource(Res.string.settings_language_en)
        else -> AppLocales.supported.firstOrNull { it.code == code }?.displayName ?: code
    }
}

@Composable
private fun LanguageSelectionModal(
    title: String,
    options: List<LanguageDescriptor>,
    selectedCode: String,
    isTv: Boolean,
    isMediaOption: Boolean = false,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    if (isTv) {
        TvDrawerEffect(
            title = title,
            icon = Lucide.Languages,
            onDismiss = onDismiss
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                options.forEach { lang ->
                    item(key = lang.code) {
                        AvalonTvDrawerItem(
                            title = getLanguageLabel(lang.code, isMediaOption = isMediaOption),
                            icon = if (lang.code == "auto") Lucide.Globe else if (lang.code == "original") Lucide.Film else Lucide.Languages,
                            isSelected = selectedCode == lang.code,
                            onClick = { onSelected(lang.code) }
                        )
                    }
                }
            }
        }
    } else {
        val density = LocalDensity.current
        AvalonDropdownMenu(
            expanded = true,
            alignment = Alignment.Center,
            offset = IntOffset(0, with(density) { 0.dp.roundToPx() }),
            onDismissRequest = onDismiss,
            width = 260.dp
        ) {
            options.forEach { lang ->
                AvalonDropdownMenuItem(
                    text = getLanguageLabel(lang.code, isMediaOption = isMediaOption),
                    icon = if (lang.code == "auto") Lucide.Globe else if (lang.code == "original") Lucide.Film else Lucide.Languages,
                    onClick = { onSelected(lang.code) }
                )
            }
        }
    }
}
