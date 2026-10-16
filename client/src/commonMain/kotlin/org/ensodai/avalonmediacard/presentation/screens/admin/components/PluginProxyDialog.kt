package org.ensodai.avalonmediacard.presentation.screens.admin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.presentation.overlay.TvModalSurface
import org.ensodai.avalonmediacard.presentation.screens.admin.action.AdminActions
import org.ensodai.avalonmediacard.presentation.screens.admin.components.proxy.PluginProxyConfigPanel
import org.ensodai.avalonmediacard.presentation.screens.admin.components.proxy.PluginProxyListItem
import org.ensodai.avalonmediacard.presentation.screens.admin.viewState.AdminViewState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun PluginProxyDialog(
    state: AdminViewState,
    actions: AdminActions
) {
    TvModalSurface(
        isOpen = state.isPluginProxiesDialogOpen,
        onDismissRequest = actions.onClosePluginProxiesDialog,
        scrimColor = Color.Black.copy(alpha = 0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Globe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(Res.string.admin_plugin_proxy_dialog_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.admin_plugin_proxy_dialog_subtitle),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvAndWebHoverEffect(
                                shape = CircleShape,
                                scaleTarget = 1.15f,
                                clickEnabled = true,
                                onClick = actions.onClosePluginProxiesDialog,
                                activeBorderColor = Color.White,
                                activeBorderWidth = 1.5.dp
                            )
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = stringResource(Res.string.admin_plugin_proxy_close),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content body
                if (state.isPluginProxiesLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
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
                        Text(
                            text = stringResource(Res.string.admin_plugin_proxy_no_plugins),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
}
