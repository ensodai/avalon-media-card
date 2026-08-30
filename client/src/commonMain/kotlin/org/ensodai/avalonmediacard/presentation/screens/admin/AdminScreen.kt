package org.ensodai.avalonmediacard.presentation.screens.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.Wrench
import org.ensodai.avalonmediacard.presentation.screens.admin.components.AdminIntegrationsTab
import org.ensodai.avalonmediacard.presentation.screens.admin.components.AdminSystemTab
import org.ensodai.avalonmediacard.presentation.screens.admin.components.AdminUsersTab
import org.ensodai.avalonmediacard.presentation.screens.admin.model.AdminTab
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AdminScreen(
    viewModel: AdminViewModel = koinInject()
) {
    val state by viewModel.viewState.collectAsState()
    val actions = viewModel.actions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header & Tab Navigation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = stringResource(Res.string.admin_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.admin_subtitle),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Pill Tab Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminTab.entries.forEach { tab ->
                    val isSelected = state.selectedTab == tab
                    val tabIcon: ImageVector = when (tab) {
                        AdminTab.USERS -> Lucide.Users
                        AdminTab.INTEGRATIONS -> Lucide.Wrench
                        AdminTab.SYSTEM -> Lucide.Settings
                    }

                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .tvAndWebHoverEffect(
                                shape = RoundedCornerShape(12.dp),
                                clickEnabled = true,
                                onClick = { actions.onTabSelected(tab) },
                                activeBorderColor = Color.White,
                                activeBorderWidth = 1.5.dp
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(containerColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = tabIcon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(tab.titleRes),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (state.selectedTab) {
                AdminTab.USERS -> AdminUsersTab(state, actions)
                AdminTab.INTEGRATIONS -> AdminIntegrationsTab(state, actions)
                AdminTab.SYSTEM -> AdminSystemTab(state, actions)
            }
        }
    }
}
