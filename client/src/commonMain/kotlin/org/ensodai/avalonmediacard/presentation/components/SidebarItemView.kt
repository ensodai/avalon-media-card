package org.ensodai.avalonmediacard.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate

@Composable
fun SidebarItemView(
    component: SidebarItem,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                component.screen?.let { onAction(ActionNavigate(it)) }
            }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (component.iconName) {
            "home" -> Lucide.House
            "folder" -> Lucide.Folder
            "list" -> Lucide.List
            "plus" -> Lucide.Plus
            "arrow-left" -> Lucide.ArrowLeft
            "settings" -> Lucide.Settings
            "heart" -> Lucide.Heart
            "eye" -> Lucide.Eye
            "star" -> Lucide.Star
            "user" -> Lucide.User
            else -> null
        }
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = getLocalizedSidebarTitle(component),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
