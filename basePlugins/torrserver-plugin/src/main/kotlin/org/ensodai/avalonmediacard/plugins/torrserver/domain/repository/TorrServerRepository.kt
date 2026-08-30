package org.ensodai.avalonmediacard.plugins.torrserver.domain.repository

import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerFile
import kotlin.uuid.Uuid

interface TorrServerRepository {
    suspend fun testConnection(host: String, login: String?, pass: String?): String
    suspend fun testGstConnection(host: String, login: String?, pass: String?): String
    suspend fun addTorrent(urlOrMagnet: String, fileBytes: ByteArray?, userId: Uuid?): String?
    suspend fun getFiles(hash: String, userId: Uuid?): List<TorrServerFile>?
    suspend fun dropTorrent(hash: String, userId: Uuid?)
    suspend fun buildStreamUrl(hash: String, fileIndex: Int?, filePath: String, userId: Uuid?, useGst: Boolean = false): String
    suspend fun getGstProbe(hash: String, fileIndex: Int?, userId: Uuid?): org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerGstProbeInfo?
}
