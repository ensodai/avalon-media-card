package org.ensodai.avalonmediacard.database.providers

import org.ensodai.avalonmediacard.contract.plugins.SourceMapping
import org.ensodai.avalonmediacard.contract.plugins.SourceMappingProvider
import org.ensodai.avalonmediacard.database.MediaTable
import org.ensodai.avalonmediacard.database.SourceMappingTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.koin.core.annotation.Single

@Single(binds = [SourceMappingProvider::class])
class SourceMappingProviderImpl : SourceMappingProvider {

    private fun String.toIntList(): List<Int> = this.split(",").mapNotNull { it.trim().toIntOrNull() }
    private fun List<Int>?.toCsv(): String? = this?.joinToString(",")

    override suspend fun getMappingsBySourceId(sourceId: String): List<SourceMapping> = dbQuery {
        (SourceMappingTable leftJoin MediaTable)
            .selectAll()
            .where { SourceMappingTable.sourceId eq sourceId }
            .map(::rowToSourceMapping)
    }

    override suspend fun getMappingsByMediaId(mediaId: String): List<SourceMapping> = dbQuery {
        val parsed =
            MediaTable.selectAll().where { MediaTable.externalId eq mediaId }.firstOrNull()?.get(MediaTable.id)?.value
                ?: return@dbQuery emptyList()
        (SourceMappingTable leftJoin MediaTable)
            .selectAll()
            .where { SourceMappingTable.mediaId eq parsed }
            .map(::rowToSourceMapping)
    }

    override suspend fun getMappings(mediaId: String, sourceId: String): List<SourceMapping> = dbQuery {
        val parsed =
            MediaTable.selectAll().where { MediaTable.externalId eq mediaId }.firstOrNull()?.get(MediaTable.id)?.value
                ?: return@dbQuery emptyList()
        (SourceMappingTable leftJoin MediaTable)
            .selectAll()
            .where { (SourceMappingTable.mediaId eq parsed) and (SourceMappingTable.sourceId eq sourceId) }
            .map(::rowToSourceMapping)
    }

    private fun rowToSourceMapping(row: ResultRow) = SourceMapping(
        id = row[SourceMappingTable.id].value.toString(),
        sourceType = row[SourceMappingTable.sourceType],
        sourceId = row[SourceMappingTable.sourceId],
        itemKey = row[SourceMappingTable.itemKey],
        seasons = row[SourceMappingTable.seasons]?.toIntList(),
        episodes = row[SourceMappingTable.episodes]?.toIntList(),
        isAbsolute = row[SourceMappingTable.isAbsolute],
        isManual = row[SourceMappingTable.isManual],
        mediaId = row.getOrNull(MediaTable.externalId) ?: row[SourceMappingTable.mediaId]?.value?.toString(),
        fileIndex = row[SourceMappingTable.fileIndex],
        fileSize = row[SourceMappingTable.fileSize],
        streamUrl = row[SourceMappingTable.streamUrl],
        quality = row[SourceMappingTable.quality]
    )

    override suspend fun saveMapping(mapping: SourceMapping): SourceMapping = dbQuery {
        val existing = SourceMappingTable.selectAll().where {
            (SourceMappingTable.sourceId eq mapping.sourceId) and (SourceMappingTable.itemKey eq mapping.itemKey)
        }.singleOrNull()

        val mediaIdVal = mapping.mediaId
        val internalMediaId = if (mediaIdVal != null) {
            MediaTable.insertIgnore {
                it[id] = kotlin.uuid.Uuid.random()
                it[catalogId] = "tmdb"
                it[externalId] = mediaIdVal
                it[mediaType] = "movie"
            }
            MediaTable.selectAll().where { MediaTable.externalId eq mediaIdVal }.firstOrNull()?.get(MediaTable.id)?.value
        } else null

        val savedId = if (existing != null) {
            SourceMappingTable.update({ SourceMappingTable.id eq existing[SourceMappingTable.id] }) {
                it[SourceMappingTable.sourceType] = mapping.sourceType
                it[SourceMappingTable.seasons] = mapping.seasons.toCsv()
                it[SourceMappingTable.episodes] = mapping.episodes.toCsv()
                it[SourceMappingTable.isAbsolute] = mapping.isAbsolute
                it[SourceMappingTable.isManual] = mapping.isManual
                if (internalMediaId != null) it[SourceMappingTable.mediaId] = internalMediaId
                if (mapping.fileIndex != null) it[SourceMappingTable.fileIndex] = mapping.fileIndex
                if (mapping.fileSize != null) it[SourceMappingTable.fileSize] = mapping.fileSize
                if (mapping.streamUrl != null) it[SourceMappingTable.streamUrl] = mapping.streamUrl
                if (mapping.quality != null) it[SourceMappingTable.quality] = mapping.quality
            }
            existing[SourceMappingTable.id].value
        } else {
            val newId = kotlin.uuid.Uuid.random()
            SourceMappingTable.insert {
                it[SourceMappingTable.id] = newId
                it[SourceMappingTable.sourceType] = mapping.sourceType
                it[SourceMappingTable.sourceId] = mapping.sourceId
                it[SourceMappingTable.itemKey] = mapping.itemKey
                it[SourceMappingTable.seasons] = mapping.seasons.toCsv()
                it[SourceMappingTable.episodes] = mapping.episodes.toCsv()
                it[SourceMappingTable.isAbsolute] = mapping.isAbsolute
                it[SourceMappingTable.isManual] = mapping.isManual
                if (internalMediaId != null) it[SourceMappingTable.mediaId] = internalMediaId
                it[SourceMappingTable.fileIndex] = mapping.fileIndex
                it[SourceMappingTable.fileSize] = mapping.fileSize
                it[SourceMappingTable.streamUrl] = mapping.streamUrl
                it[SourceMappingTable.quality] = mapping.quality
            }
            newId
        }

        mapping.copy(id = savedId.toString())
    }

    override suspend fun saveMappingsBatch(mappings: List<SourceMapping>) {
        if (mappings.isEmpty()) return
        for (m in mappings) {
            saveMapping(m)
        }
    }

    override suspend fun clearMappingsByMediaId(mediaId: String) {
        dbQuery {
            val parsed = MediaTable.selectAll().where { MediaTable.externalId eq mediaId }.firstOrNull()
                ?.get(MediaTable.id)?.value
                ?: return@dbQuery
            SourceMappingTable.deleteWhere { SourceMappingTable.mediaId eq parsed }
        }
    }

    override suspend fun clearMappingsBySourceId(sourceId: String) {
        dbQuery {
            SourceMappingTable.deleteWhere { SourceMappingTable.sourceId eq sourceId }
        }
    }
}
