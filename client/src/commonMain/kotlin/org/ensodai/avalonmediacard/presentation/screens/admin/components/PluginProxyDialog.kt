package org.ensodai.avalonmediacard.presentation.screens.admin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.designsystem.components.AvalonCircularProgressIndicator
import org.ensodai.avalonmediacard.designsystem.components.AvalonDialog
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonIconButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.presentation.screens.admin.action.AdminActions
import org.ensodai.avalonmediacard.presentation.screens.admin.components.proxy.PluginProxyConfigPanel
import org.ensodai.avalonmediacard.presentation.screens.admin.components.proxy.PluginProxyListItem
import org.ensodai.avalonmediacard.presentation.screens.admin.viewState.AdminViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun PluginProxyDialog(
    state: AdminViewState,
    actions: AdminActions
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    AvalonDialog(
        isOpen = state.isPluginProxiesDialogOpen,
        onDismissRequest = actions.onClosePluginProxiesDialog,
        modifier = Modifier.widthIn(max = 920.dp).fillMaxHeight(0.92f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(shapes.control)
                            .background(colors.accentPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AvalonIcon(
                            imageVector = Lucide.Globe,
                            contentDescription = null,
                            tint = colors.accentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        AvalonText(
                            text = stringResource(Res.string.admin_plugin_proxy_dialog_title),
                            style = AvalonTheme.typography.sectionTitle,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        AvalonText(
                            text = stringResource(Res.string.admin_plugin_proxy_dialog_subtitle),
                            style = AvalonTheme.typography.metadata,
                            color = colors.textSecondary
                        )
                    }
                }

                AvalonIconButton(
                    onClick = actions.onClosePluginProxiesDialog,
                    contentDescription = stringResource(Res.string.admin_plugin_proxy_close),
                    modifier = Modifier.size(36.dp)
                ) {
                    AvalonIcon(
                        imageVector = Lucide.X,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Content body
            if (state.isPluginProxiesLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AvalonCircularProgressIndicator(
                        color = colors.accentPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else if (state.pluginProxySettings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AvalonText(
                        text = stringResource(Res.string.admin_plugin_proxy_no_plugins),
                        style = AvalonTheme.typography.cardTitle,
                        color = colors.textSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left: Plugin list
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.pluginProxySettings,
                            key = { it.pluginId }
                        ) { plugin ->
                            PluginProxyListItem(
                                plugin = plugin,
                                isSelected = plugin.pluginId == state.editingPluginId,
                                onClick = { actions.onSelectPluginForEdit(plugin.pluginId) }
                            )
                        }
                    }

                    // Right: Config form for selected plugin
                    val selectedPlugin = state.pluginProxySettings.find { it.pluginId == state.editingPluginId }
                    if (selectedPlugin != null) {
                        PluginProxyConfigPanel(
                            selectedPlugin = selectedPlugin,
                            state = state,
                            actions = actions,
                            modifier = Modifier.weight(0.58f)
                        )
                    }
                }
            }
        }
    }
}
