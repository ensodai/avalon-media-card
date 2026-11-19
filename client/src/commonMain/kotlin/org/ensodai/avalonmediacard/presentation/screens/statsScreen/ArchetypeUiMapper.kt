package org.ensodai.avalonmediacard.presentation.screens.statsScreen

import androidx.compose.ui.graphics.Color
import org.ensodai.avalonmediacard.contract.model.ArchetypeType

object ArchetypeUiMapper {

    fun getGradientColors(type: ArchetypeType?): List<Color> {
        return when (type) {
            ArchetypeType.SCIFI_VISIONARY -> listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
            ArchetypeType.SUSPENSE_HUNTER -> listOf(Color(0xFF8E0E00), Color(0xFF1F1C18))
            ArchetypeType.DEEP_EMPATH -> listOf(Color(0xFFE55D87), Color(0xFF5FC3E4))
            ArchetypeType.DOPAMINE_SEEKER -> listOf(Color(0xFFF7971E), Color(0xFFFFD200))
            ArchetypeType.CURIOUS_VOYAGER -> listOf(Color(0xFF11998E), Color(0xFF38EF7D))
            null -> listOf(Color(0xFF2C3E50), Color(0xFF34495E))
        }
    }
}
