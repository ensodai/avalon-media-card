package org.ensodai.avalonmediacard.presentation.screens.admin.components.proxy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Server
import com.composables.icons.lucide.User
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.presentation.screens.admin.action.AdminActions
import org.ensodai.avalonmediacard.presentation.screens.admin.viewState.AdminViewState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTextField

@Composable
fun PluginProxyFormFields(
    state: AdminViewState,
    actions: AdminActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Row 1: Protocol selector + Port
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(Res.string.admin_plugin_proxy_type_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ProxyProtocolSelector(
                    selectedType = state.editProxyType,
                    onTypeSelected = actions.onEditProxyTypeChanged
                )
            }

            Column(
                modifier = Modifier.width(130.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(Res.string.admin_plugin_proxy_port_placeholder),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AvalonTextField(
                    value = state.editProxyPort,
                    onValueChange = actions.onEditProxyPortChanged,
                    placeholder = "8080",
                    height = 42.dp,
                    fontSize = 14.sp,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Row 2: Host & IP Input (full width)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(Res.string.admin_plugin_proxy_host_placeholder),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AvalonTextField(
                value = state.editProxyHost,
                onValueChange = actions.onEditProxyHostChanged,
                placeholder = "127.0.0.1 или proxy.domain.com",
                leadingIcon = {
                    Icon(
                        imageVector = Lucide.Server,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                },
                height = 42.dp,
                fontSize = 14.sp,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Username & Password side-by-side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(Res.string.admin_plugin_proxy_username_placeholder),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AvalonTextField(
                    value = state.editProxyUsername,
                    onValueChange = actions.onEditProxyUsernameChanged,
                    placeholder = stringResource(Res.string.admin_plugin_proxy_username_placeholder),
                    leadingIcon = {
                        Icon(
                            imageVector = Lucide.User,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    height = 42.dp,
                    fontSize = 14.sp,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(Res.string.admin_plugin_proxy_password_placeholder),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AvalonTextField(
                    value = state.editProxyPassword,
                    onValueChange = actions.onEditProxyPasswordChanged,
                    placeholder = stringResource(Res.string.admin_plugin_proxy_password_placeholder),
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = Lucide.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    height = 42.dp,
                    fontSize = 14.sp,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
