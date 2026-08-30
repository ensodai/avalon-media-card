package org.ensodai.avalonmediacard.plugins.torrserver.data.repository

import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.TorrServerRepository
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerFile
import org.ensodai.avalonmediacard.plugins.torrserver.data.network.TorrServerApiClient
import kotlin.uuid.Uuid

class TorrServerRepositoryImpl(
    private val apiClient: TorrServerApiClient
) : TorrServerRepository {
    
    override suspend fun testConnection(host: String, login: String?, pass: String?): String {
        return apiClient.testConnection(host, login, pass)
    }

    override suspend fun testGstConnection(host: String, login: String?, pass: String?): String {
        return apiClient.testGstConnection(host, login, pass)
    }

    override suspend fun addTorrent(urlOrMagnet: String, fileBytes: ByteArray?, userId: Uuid?): String? {
        return apiClient.addTorrent(urlOrMagnet, fileBytes, userId)
    }

    override suspend fun getFiles(hash: String, userId: Uuid?): List<TorrServerFile>? {
        return apiClient.getFiles(hash, userId)
    }

    override suspend fun dropTorrent(hash: String, userId: Uuid?) {
        apiClient.dropTorrent(hash, userId)
    }

    override suspend fun buildStreamUrl(hash: String, fileIndex: Int?, filePath: String, userId: Uuid?, useGst: Boolean): String {
        return apiClient.buildStreamUrl(hash, fileIndex, filePath, userId, useGst)
    }

    override suspend fun getGstProbe(hash: String, fileIndex: Int?, userId: Uuid?): org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerGstProbeInfo? {
        return apiClient.getGstProbe(hash, fileIndex, userId)
    }
}
