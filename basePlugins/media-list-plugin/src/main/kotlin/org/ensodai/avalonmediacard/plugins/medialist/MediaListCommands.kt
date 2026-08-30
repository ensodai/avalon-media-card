package org.ensodai.avalonmediacard.plugins.medialist

import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.slot.ServerAction

@Serializable
data class LoadMoreMediaList(
    val key: MediaKey,
    val listType: String,
    val page: Int
) : ServerAction
