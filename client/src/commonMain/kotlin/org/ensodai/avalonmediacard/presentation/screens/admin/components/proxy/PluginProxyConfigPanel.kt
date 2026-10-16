package org.ensodai.avalonmediacard.presentation.screens.admin.components.proxy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.contract.admin.PluginProxySettingDto
import org.ensodai.avalonmediacard.presentation.screens.admin.action.AdminActions
import org.ensodai.avalonmediacard.presentation.screens.admin.viewState.AdminViewState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonButton
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun PluginProxyConfigPanel(
    selectedPlugin: PluginProxySettingDto,
    state: AdminViewState,
    actions: AdminActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selected Plugin Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = selectedPlugin.pluginName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ID: ${selectedPlugin.pluginId} • v${selectedPlugin.pluginVersion}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .tvAndWebHoverEffect(
                        shape = RoundedCornerShape(16.dp),
                        scaleTarget = 1.04f,
                        clickEnabled = true,
                        onClick = { actions.onEditProxyEnabledChanged(!state.editProxyEnabled) },
                        activeBorderWidth = 0.dp,
                        activeBorderColor = Color.Transparent
                    )
            ) {
                Switch(
                    checked = state.editProxyEnabled,
                    onCheckedChange = null
                )
            }
        }

        Text(
            text = stringResource(Res.string.admin_plugin_proxy_enable_switch),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (state.editProxyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!state.editProxyEnabled) {
            PluginProxyDisabledView(
                selectedPlugin = selectedPlugin,
                isSaving = state.isPluginProxySaving,
                onDisableAndSave = actions.onSavePluginProxy,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Form fields (Protocol, Port, Host, Credentials)
            PluginProxyFormFields(
                state = state,
                actions = actions
            )

            // Test and Save Result Banners
            PluginProxyStatusBanners(
                testResult = state.pluginProxyTestResult,
                testSuccess = state.pluginProxyTestSuccess,
                saveResult = state.pluginProxySaveResult
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvalonButton(
                    text = if (state.isPluginProxyTesting) stringResource(Res.string.admin_plugin_proxy_btn_testing) else stringResource(Res.string.admin_plugin_proxy_btn_test),
                    onClick = actions.onTestPluginProxy,
                    isLoading = state.isPluginProxyTesting,
                    enabled = state.editProxyHost.isNotBlank(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    height = 44.dp,
                    fontSize = 14.sp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.42f)
                )

                AvalonButton(
                    text = if (state.isPluginProxySaving) stringResource(Res.string.admin_plugin_proxy_btn_saving) else stringResource(Res.string.admin_plugin_proxy_btn_save),
                    onClick = actions.onSavePluginProxy,
                    isLoading = state.isPluginProxySaving,
                    height = 44.dp,
                    fontSize = 14.sp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.58f)
                )
            }
        }
    }
}
