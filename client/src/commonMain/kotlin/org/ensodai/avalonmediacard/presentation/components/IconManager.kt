package org.ensodai.avalonmediacard.presentation.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.slot.IconType

object IconManager {
    fun getIcon(type: IconType?): ImageVector {
        if (type == null) return Lucide.Info // Fallback icon
        return when (type) {
            IconType.PLAY -> Lucide.Play
            IconType.VIDEO -> Lucide.Video
            IconType.HEART -> Lucide.Heart
            IconType.HEART_FILLED -> Lucide.Heart // Note: if you have a filled version, use it. Usually Lucide icons are outlined. For filled, you'd use a different set or tint differently, but we'll map to Heart for now
            IconType.PLUS -> Lucide.Plus
            IconType.CHECK -> Lucide.Check
            IconType.STAR -> Lucide.Star
            IconType.STAR_FILLED -> Lucide.Star
            IconType.COLLECTION -> Lucide.Folder
            IconType.LIST -> Lucide.List
            IconType.MORE -> Lucide.Menu
        }
    }
}
