package org.ensodai.avalonmediacard.plugins.torrserver.data.network.providers

import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.JackettResult

interface TorrentSearchProvider {
    suspend fun search(query: String): List<JackettResult>
}
