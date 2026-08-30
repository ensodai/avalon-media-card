package org.ensodai.avalonmediacard.presentation.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.auth.AuthResponse
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonButton
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(
    initialServerUrl: String,
    onLoginSuccess: (AuthResponse) -> Unit,
    onLoginClick: suspend (String, String, String) -> Result<AuthResponse>,
    onRegisterClick: suspend (String, String, String) -> Result<AuthResponse>
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val fillFieldsError = stringResource(Res.string.login_error_fill_all_fields)
    val genericError = stringResource(Res.string.login_error_generic)
    val connectionError = stringResource(Res.string.login_error_connection)
    val serverNotFoundError = stringResource(Res.string.login_error_server_not_found)
    val timeoutError = stringResource(Res.string.login_error_timeout)
    val sslError = stringResource(Res.string.login_error_ssl)

    fun formatAuthError(throwable: Throwable): String {
        val message = throwable.message.orEmpty()
        val className = throwable::class.simpleName.orEmpty()
        return when {
            message.contains("ConnectException", ignoreCase = true) ||
                className.contains("ConnectException", ignoreCase = true) ||
                message.contains("Connection refused", ignoreCase = true) ||
                message.contains("ECONNREFUSED", ignoreCase = true) -> connectionError

            message.contains("UnknownHostException", ignoreCase = true) ||
                className.contains("UnknownHostException", ignoreCase = true) ||
                message.contains("UnresolvedAddressException", ignoreCase = true) ||
                className.contains("UnresolvedAddressException", ignoreCase = true) -> serverNotFoundError

            message.contains("SocketTimeout", ignoreCase = true) ||
                className.contains("SocketTimeout", ignoreCase = true) ||
                message.contains("Timeout", ignoreCase = true) -> timeoutError

            message.contains("SSLHandshakeException", ignoreCase = true) ||
                className.contains("SSLHandshakeException", ignoreCase = true) ||
                message.contains("CertPathValidatorException", ignoreCase = true) ||
                message.contains("Trust anchor", ignoreCase = true) -> sslError

            message.isNotBlank() -> message
            else -> genericError
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = maxHeight)
                .verticalScroll(scrollState)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegisterMode) stringResource(Res.string.login_title_sign_up) else stringResource(Res.string.login_title_sign_in),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    AvalonTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            error = null
                        },
                        placeholder = stringResource(Res.string.login_field_username),
                        leadingIcon = {
                            Icon(
                                imageVector = Lucide.User,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AvalonTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            error = null
                        },
                        placeholder = stringResource(Res.string.login_field_password),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(
                                imageVector = Lucide.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AvalonTextField(
                        value = serverUrl,
                        onValueChange = { 
                            serverUrl = it
                            error = null
                        },
                        placeholder = stringResource(Res.string.login_field_server_url),
                        leadingIcon = {
                            Icon(
                                imageVector = Lucide.Globe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AvalonButton(
                        text = if (isRegisterMode) stringResource(Res.string.login_btn_sign_up) else stringResource(Res.string.login_btn_sign_in),
                        onClick = {
                            if (username.isBlank() || password.isBlank() || serverUrl.isBlank()) {
                                error = fillFieldsError
                                return@AvalonButton
                            }
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val res = if (isRegisterMode) {
                                        onRegisterClick(username.trim(), password, serverUrl.trim())
                                    } else {
                                        onLoginClick(username.trim(), password, serverUrl.trim())
                                    }
                                    res.fold(
                                        onSuccess = { authResponse ->
                                            if (authResponse.success && !authResponse.token.isNullOrBlank()) {
                                                onLoginSuccess(authResponse)
                                            } else {
                                                error = authResponse.error ?: genericError
                                            }
                                        },
                                        onFailure = {
                                            error = formatAuthError(it)
                                        }
                                    )
                                } catch (e: Throwable) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    error = formatAuthError(e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AvalonButton(
                        text = if (isRegisterMode) stringResource(Res.string.login_btn_have_account) else stringResource(Res.string.login_btn_no_account),
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            error = null
                        },
                        enabled = !isLoading,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
