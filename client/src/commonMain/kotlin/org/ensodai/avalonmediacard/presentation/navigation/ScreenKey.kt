package org.ensodai.avalonmediacard.presentation.navigation


import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import kotlin.uuid.Uuid

@Serializable
data class ScreenKey(
    val screen: Screen,
    val id: String = Uuid.random().toString()
) : NavKey
