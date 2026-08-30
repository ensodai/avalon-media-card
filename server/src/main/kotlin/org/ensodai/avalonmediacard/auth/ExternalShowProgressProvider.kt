package org.ensodai.avalonmediacard.auth

interface ExternalShowProgressProvider {
    val serviceName: String

    suspend fun getShowWatchedProgress(
        accessToken: String,
        showTmdbId: Int
    ): WatchedProgress?
}
