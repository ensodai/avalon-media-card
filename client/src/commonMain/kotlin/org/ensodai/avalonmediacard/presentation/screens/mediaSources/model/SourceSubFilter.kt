package org.ensodai.avalonmediacard.presentation.screens.mediaSources.model

import androidx.compose.runtime.Immutable

@Immutable
data class SourceSubFilter(
    val id: String,
    val title: String,
    val count: Int? = null
)
