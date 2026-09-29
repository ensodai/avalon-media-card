package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.HardDrive
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Users
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerNavigator
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerScreen
import org.jetbrains.compose.resources.stringResource

/**
 * ТВ-шторка настроек медиакарточки (Совместный просмотр, выбор источника).
 */
class MediaDetailsSettingsDrawerScreen(
    override val title: String,
    override val callerFocusRequester: FocusRequester? = null,
    private val onOpenWatchParty: (() -> Unit)? = null,
    private val onRequestOtherSource: (() -> Unit)? = null
) : TvDrawerScreen {
    override val key: String = "media_details_settings"
    override val icon: ImageVector = Lucide.Settings

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (onOpenWatchParty != null) {
                item(key = "watch_party") {
                    AvalonTvDrawerItem(
                        title = stringResource(Res.string.watch_party_title),
                        subtitle = stringResource(Res.string.watch_party_step_setup),
                        icon = Lucide.Users,
                        onClick = {
                            navigator.clear()
                            onOpenWatchParty()
                        }
                    )
                }
            }

            if (onRequestOtherSource != null) {
                item(key = "other_source") {
                    AvalonTvDrawerItem(
                        title = stringResource(Res.string.details_sources_select),
                        subtitle = stringResource(Res.string.player_btn_select_other_source),
                        icon = Lucide.HardDrive,
                        onClick = {
                            navigator.clear()
                            onRequestOtherSource.invoke()
                        }
                    )
                }
            }
        }
    }
}
