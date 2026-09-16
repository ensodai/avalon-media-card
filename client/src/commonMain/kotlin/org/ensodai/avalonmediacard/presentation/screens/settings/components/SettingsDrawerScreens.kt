package org.ensodai.avalonmediacard.presentation.screens.settings.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Tv
import com.composables.icons.lucide.Zap
import org.ensodai.avalonmediacard.data.AppLocales
import org.ensodai.avalonmediacard.data.LanguageDescriptor
import org.ensodai.avalonmediacard.data.UiModeOverride
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerNavigator
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun getSettingsLanguageLabel(code: String, isMediaOption: Boolean = false): String {
    return when (code) {
        "auto" -> if (isMediaOption) stringResource(Res.string.settings_language_as_in_app) else stringResource(Res.string.settings_language_auto)
        "original" -> stringResource(Res.string.settings_language_original)
        "ru" -> stringResource(Res.string.settings_language_ru)
        "en" -> stringResource(Res.string.settings_language_en)
        else -> AppLocales.supported.firstOrNull { it.code == code }?.displayName ?: code
    }
}

/**
 * Экран выбора языка в правой ТВ-шторке.
 */
class SettingsLanguageDrawerScreen(
    override val key: String,
    override val title: String,
    override val icon: ImageVector = Lucide.Languages,
    override val callerFocusRequester: FocusRequester? = null,
    private val options: List<LanguageDescriptor>,
    private val selectedCode: String,
    private val isMediaOption: Boolean = false,
    private val onSelected: (String) -> Unit
) : TvDrawerScreen {

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(options, key = { it.code }) { lang ->
                val iconVector = when (lang.code) {
                    "auto" -> Lucide.Globe
                    "original" -> Lucide.Film
                    else -> Lucide.Languages
                }
                AvalonTvDrawerItem(
                    title = getSettingsLanguageLabel(lang.code, isMediaOption = isMediaOption),
                    icon = iconVector,
                    isSelected = selectedCode == lang.code,
                    onClick = {
                        onSelected(lang.code)
                        navigator.clear()
                    }
                )
            }
        }
    }
}

/**
 * Экран выбора режима интерфейса в правой ТВ-шторке.
 */
class SettingsUiModeDrawerScreen(
    override val title: String,
    override val callerFocusRequester: FocusRequester? = null,
    private val currentMode: UiModeOverride,
    private val onSelected: (UiModeOverride) -> Unit
) : TvDrawerScreen {
    override val key: String = "settings_ui_mode"
    override val icon: ImageVector = Lucide.Monitor

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.settings_ui_mode_auto),
                    icon = Lucide.Zap,
                    isSelected = currentMode == UiModeOverride.AUTO,
                    onClick = {
                        onSelected(UiModeOverride.AUTO)
                        navigator.clear()
                    }
                )
            }
            item {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.settings_ui_mode_tv),
                    icon = Lucide.Tv,
                    isSelected = currentMode == UiModeOverride.TV,
                    onClick = {
                        onSelected(UiModeOverride.TV)
                        navigator.clear()
                    }
                )
            }
            item {
                AvalonTvDrawerItem(
                    title = stringResource(Res.string.settings_ui_mode_pc),
                    icon = Lucide.Monitor,
                    isSelected = currentMode == UiModeOverride.PC,
                    onClick = {
                        onSelected(UiModeOverride.PC)
                        navigator.clear()
                    }
                )
            }
        }
    }
}
