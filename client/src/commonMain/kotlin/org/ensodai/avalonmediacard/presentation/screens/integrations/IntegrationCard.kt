package org.ensodai.avalonmediacard.presentation.screens.integrations

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun IntegrationCard(
    state: SlotUiState<SlotData.SettingsGroup>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.hasError && state.error != null) {
        org.ensodai.avalonmediacard.presentation.screens.commonComponents.SlotErrorCard(
            message = state.error,
            retryAction = state.retryAction,
            onAction = onAction,
            modifier = modifier
        )
        return
    }
    val data = state.data ?: SlotData.SettingsGroup(
        title = stringResource(Res.string.common_loading),
        description = stringResource(Res.string.common_loading),
        connectionStatus = ValidationStatus.None,
        fields = emptyList()
    )
    IntegrationCardInternal(
        component = data,
        onAction = onAction,
        modifier = modifier
    )
}

@Composable
private fun IntegrationCardInternal(
    component: SlotData.SettingsGroup,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }
    val textStates = remember { mutableStateMapOf<String, String>() }
    val toggleStates = remember { mutableStateMapOf<String, Boolean>() }

    val defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val activeBorderColor = Color.White.copy(alpha = 0.8f)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    val titleTextColor by animateColorAsState(
        targetValue = if (isHovered) Color.White else MaterialTheme.colorScheme.onSurface
    )
    val descTextColor by animateColorAsState(
        targetValue = if (isHovered) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
    )

    val hoverModifier = if (isExpanded) {
        Modifier.border(1.dp, defaultBorderColor, RoundedCornerShape(12.dp))
    } else {
        Modifier.tvAndWebHoverEffect(
            scaleTarget = 1.01f,
            activeBorderWidth = 1.dp,
            activeBorderColor = activeBorderColor,
            defaultBorderWidth = 1.dp,
            defaultBorderColor = defaultBorderColor,
            shape = RoundedCornerShape(12.dp),
            clickEnabled = false,
            focusEnabled = true,
            tiltEnabled = true,
            onStateChange = { isHovered = it }
        )
    }

    Column(
        modifier = modifier
            .then(hoverModifier)
            .animateContentSize()
            .background(backgroundColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleTextColor
                )
                component.description?.let { desc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        fontSize = 14.sp,
                        color = descTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            StatusBadge(status = component.connectionStatus)
        }

        if (isExpanded && (component.fields.isNotEmpty() || component.saveAction != null)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                component.fields.forEach { field ->
                    when (field) {
                        is SettingField.Toggle -> {
                            val currentValue = toggleStates[field.key] ?: field.value
                            ToggleFieldRow(
                                field = field,
                                value = currentValue,
                                onValueChange = { toggleStates[field.key] = it },
                                onAction = onAction
                            )
                        }

                        is SettingField.TextField -> {
                            val currentValue = textStates[field.key] ?: field.value
                            TextFieldRow(
                                field = field,
                                value = currentValue,
                                onValueChange = { textStates[field.key] = it },
                                onAction = onAction,
                                allFields = component.fields.filterIsInstance<SettingField.TextField>(),
                                textStates = textStates
                            )
                        }

                        is SettingField.Info -> {
                            InfoFieldRow(field = field, onAction = onAction)
                        }
                    }
                }

                component.saveAction?.let { action ->
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val executingAction = org.ensodai.avalonmediacard.presentation.core.LocalExecutingAction.current
                    val isExecuting = executingAction != null && executingAction::class == action::class

                    Button(
                        onClick = {
                            var enrichedAction = action
                            component.fields.filterIsInstance<SettingField.TextField>().forEach { field ->
                                val value = textStates[field.key] ?: field.value
                                enrichedAction = enrichedAction.withParameter(field.key, value)
                            }
                            component.fields.filterIsInstance<SettingField.Toggle>().forEach { field ->
                                val value = toggleStates[field.key] ?: field.value
                                enrichedAction = enrichedAction.withParameter(field.key, value.toString())
                            }
                            onAction(enrichedAction)
                            if (action !is ActionOpenUrl) {
                                isExpanded = false
                            }
                        },
                        enabled = component.isSaveEnabled && !isExecuting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (isExecuting) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = component.saveActionLabel ?: stringResource(Res.string.common_save),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ValidationStatus) {
    val statusText: String
    val badgeColors: ButtonColors

    when (status) {
        ValidationStatus.Success -> {
            statusText = stringResource(Res.string.integrations_connected)
            badgeColors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                contentColor = Color(0xFF4CAF50),
                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                disabledContentColor = Color(0xFF4CAF50)
            )
        }

        ValidationStatus.Error -> {
            statusText = stringResource(Res.string.integrations_error)
            badgeColors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                contentColor = MaterialTheme.colorScheme.error,
                disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                disabledContentColor = MaterialTheme.colorScheme.error
            )
        }

        ValidationStatus.Pending -> {
            statusText = stringResource(Res.string.integrations_checking)
            badgeColors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Gray
            )
        }

        ValidationStatus.None -> {
            statusText = stringResource(Res.string.integrations_not_connected)
            badgeColors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Gray
            )
        }
    }

    Button(
        onClick = {},
        colors = badgeColors,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(28.dp),
        enabled = false
    ) {
        Text(text = statusText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToggleFieldRow(
    field: SettingField.Toggle,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    onAction: (Action) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = field.label,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f).padding(end = 16.dp)
        )
        Switch(
            checked = value,
            onCheckedChange = { newValue ->
                onValueChange(newValue)
                field.onChangeAction?.let { action -> 
                    onAction(action.withParameter(field.key, newValue.toString())) 
                }
            }
        )
    }
}

@Composable
private fun TextFieldRow(
    field: SettingField.TextField,
    value: String,
    onValueChange: (String) -> Unit,
    onAction: (Action) -> Unit,
    allFields: List<SettingField.TextField> = emptyList(),
    textStates: Map<String, String> = emptyMap()
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = field.label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val visualTransformation =
                if (field.isSensitive) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = field.isEnabled,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                visualTransformation = visualTransformation,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (field.isEnabled) 1f else 0.5f)
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty() && !field.placeholder.isNullOrEmpty()) {
                            Text(
                                text = field.placeholder.toString(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth(), propagateMinConstraints = true) {
                            innerTextField()
                        }
                    }
                }
            )

            field.validateAction?.let { action ->
                Spacer(modifier = Modifier.width(12.dp))
                
                val executingAction = org.ensodai.avalonmediacard.presentation.core.LocalExecutingAction.current
                val isExecuting = executingAction != null && executingAction::class == action::class

                Button(
                    onClick = {
                        var enrichedAction = action
                        allFields.forEach { f ->
                            val v = textStates[f.key] ?: f.value
                            enrichedAction = enrichedAction.withParameter(f.key, v)
                        }
                        onAction(enrichedAction)
                    },
                    enabled = field.isEnabled && value.isNotEmpty() && !isExecuting,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isExecuting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.integrations_check),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (field.validationStatus != ValidationStatus.None && !field.validationMessage.isNullOrEmpty()) {
            val color = when (field.validationStatus) {
                ValidationStatus.Success -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                ValidationStatus.Error -> androidx.compose.ui.graphics.Color(0xFFFF5252)
                ValidationStatus.None, ValidationStatus.Pending -> androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f)
            }
            Text(
                text = field.validationMessage.toString(),
                color = color,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun InfoFieldRow(
    field: SettingField.Info,
    onAction: (Action) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = field.label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = field.description,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        field.action?.let { action ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = field.actionLabel ?: stringResource(Res.string.integrations_details),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onAction(action) }
            )
        }
    }
}
