package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class JackettResponse(
    val Results: List<JackettResult> = emptyList()
)


fun ProwlarrResult.toJackettResult(): JackettResult {
    return JackettResult(
        title = title,
        link = downloadUrl,
        magnetUri = if (infoUrl?.startsWith("magnet:") == true) infoUrl else null,
        size = size,
        seeders = seeders,
        peers = seeders + leechers,
        tracker = indexer
    )
}

