package org.ensodai.avalonmediacard.plugins.mediadetails

import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.slot.ServerAction

@Serializable
data class LoadMoreRecommendations(val key: MediaKey, val page: Int) : ServerAction

@Serializable
data class LoadMoreSimilar(val key: MediaKey, val page: Int) : ServerAction
