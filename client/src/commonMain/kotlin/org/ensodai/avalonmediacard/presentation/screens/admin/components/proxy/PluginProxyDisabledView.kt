package org.ensodai.avalonmediacard.presentation.screens.admin.components.proxy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldAlert
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.contract.admin.PluginProxySettingDto
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonButton

@Composable
fun PluginProxyDisabledView(
    selectedPlugin: PluginProxySettingDto,
    isSaving: Boolean,
    onDisableAndSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedPlugin.isEnabled) {
        // Switch was turned OFF by user, but proxy is still enabled on server -> prompt to save
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ShieldAlert,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.admin_plugin_proxy_disabled_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.admin_plugin_proxy_pending_disable_desc),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            AvalonButton(
                text = if (isSaving) stringResource(Res.string.admin_plugin_proxy_btn_saving) else stringResource(Res.string.admin_plugin_proxy_btn_disable),
                onClick = onDisableAndSave,
                isLoading = isSaving,
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White,
                height = 44.dp,
                fontSize = 14.sp,
                shape = RoundedCornerShape(12.dp)
            )
        }
    } else {
        // Proxy is already disabled on server and switch is OFF -> show direct connection info
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Globe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.admin_plugin_proxy_disabled_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.admin_plugin_proxy_disabled_desc),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
