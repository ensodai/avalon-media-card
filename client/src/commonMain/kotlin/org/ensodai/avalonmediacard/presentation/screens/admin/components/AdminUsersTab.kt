package org.ensodai.avalonmediacard.presentation.screens.admin.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.contract.model.UserRole
import org.ensodai.avalonmediacard.contract.model.UserStatus
import org.ensodai.avalonmediacard.presentation.screens.admin.action.AdminActions
import org.ensodai.avalonmediacard.presentation.screens.admin.viewState.AdminViewState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonButton
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTextField
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun AdminUsersTab(
    state: AdminViewState,
    actions: AdminActions
) {
    var isCreateUserExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        actions.loadUsers()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Actions & Stats Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.admin_users_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val adminCount = state.users.count { it.role == UserRole.ADMIN }
                    Text(
                        text = stringResource(Res.string.admin_users_count_summary, state.users.size, adminCount, state.users.size - adminCount),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AvalonButton(
                    text = if (isCreateUserExpanded) stringResource(Res.string.admin_users_btn_hide_form) else stringResource(Res.string.admin_users_btn_new_user),
                    onClick = {
                        isCreateUserExpanded = !isCreateUserExpanded
                        if (!isCreateUserExpanded) {
                            actions.clearMessages()
                        }
                    },
                    modifier = Modifier.height(44.dp)
                )
            }
        }

        // Expandable Create User Section
        item {
            AnimatedVisibility(
                visible = isCreateUserExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.UserPlus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(Res.string.admin_users_create_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AvalonTextField(
                            value = state.usernameInput,
                            onValueChange = actions.onUsernameChanged,
                            placeholder = stringResource(Res.string.admin_users_placeholder_username),
                            leadingIcon = {
                                Icon(
                                    imageVector = Lucide.User,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        AvalonTextField(
                            value = state.passwordInput,
                            onValueChange = actions.onPasswordChanged,
                            placeholder = stringResource(Res.string.admin_users_placeholder_password),
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Lucide.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    if (state.successMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.successMessage,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AvalonButton(
                            text = stringResource(Res.string.admin_users_btn_create_account),
                            onClick = actions.onCreateUserClicked,
                            isLoading = state.isLoading,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
            }
        }

        // Users List
        if (state.isUsersLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (state.usersError != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.usersError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 15.sp
                    )
                }
            }
        } else if (state.users.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.admin_users_not_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            items(state.users, key = { it.id }) { user ->
                val isAdmin = user.role == UserRole.ADMIN
                val isFrozen = user.status == UserStatus.FROZEN
                val isCurrentUser = (user.id == state.currentUserId) || (state.currentUsername != null && user.username.equals(state.currentUsername, ignoreCase = true))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left info: Avatar, Username, Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Lucide.ShieldCheck else Lucide.User,
                                contentDescription = null,
                                tint = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = user.username,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (isCurrentUser) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.admin_users_you_badge),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Role Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = user.role.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                // Status Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isFrozen) Color(0xFFEF4444).copy(alpha = 0.15f)
                                            else Color(0xFF10B981).copy(alpha = 0.15f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isFrozen) Color(0xFFEF4444).copy(alpha = 0.4f)
                                            else Color(0xFF10B981).copy(alpha = 0.4f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = user.status.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isFrozen) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }

                    // Right action buttons with TV/Web hover
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isCurrentUser) {
                            // Role toggle
                            AdminActionButton(
                                text = if (isAdmin) stringResource(Res.string.admin_users_make_user) else stringResource(Res.string.admin_users_make_admin),
                                icon = if (isAdmin) Lucide.ShieldAlert else Lucide.ShieldCheck,
                                onClick = {
                                    val newRole = if (isAdmin) UserRole.USER else UserRole.ADMIN
                                    actions.onUserRoleChange(user.id, newRole)
                                }
                            )

                            // Status toggle (Freeze / Unfreeze)
                            AdminActionButton(
                                text = if (isFrozen) stringResource(Res.string.admin_users_unfreeze) else stringResource(Res.string.admin_users_freeze),
                                icon = if (isFrozen) Lucide.Check else Lucide.Lock,
                                tint = if (isFrozen) Color(0xFF10B981) else Color(0xFFF59E0B),
                                onClick = {
                                    val newStatus = if (isFrozen) UserStatus.ACTIVE else UserStatus.FROZEN
                                    actions.onUserStatusChange(user.id, newStatus)
                                }
                            )
                        }

                        // Password reset (available for everyone including current user)
                        AdminActionButton(
                            text = stringResource(Res.string.admin_users_btn_reset_password),
                            icon = Lucide.Key,
                            onClick = {
                                val newPassword = kotlin.uuid.Uuid.random().toString().substringBefore("-")
                                actions.onResetUserPassword(user.id, newPassword)
                            }
                        )

                        if (!isCurrentUser) {
                            // Delete button
                            AdminActionButton(
                                text = stringResource(Res.string.admin_users_btn_delete),
                                icon = Lucide.Trash2,
                                tint = Color(0xFFEF4444),
                                activeBorderColor = Color(0xFFEF4444),
                                onClick = { actions.onDeleteUser(user.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    activeBorderColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .tvAndWebHoverEffect(
                shape = RoundedCornerShape(10.dp),
                clickEnabled = true,
                onClick = onClick,
                activeBorderColor = activeBorderColor,
                activeBorderWidth = 1.5.dp
            )
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}
