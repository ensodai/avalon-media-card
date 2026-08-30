package org.ensodai.avalonmediacard.presentation.screens.admin.model

import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.StringResource

enum class AdminTab(val titleRes: StringResource) {
    USERS(Res.string.admin_tab_users),
    INTEGRATIONS(Res.string.admin_tab_integrations),
    SYSTEM(Res.string.admin_tab_system)
}
