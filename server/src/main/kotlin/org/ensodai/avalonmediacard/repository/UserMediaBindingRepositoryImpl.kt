package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import org.ensodai.avalonmediacard.contract.plugins.UserMediaBinding
import org.ensodai.avalonmediacard.contract.plugins.UserMediaBindingProvider
import org.ensodai.avalonmediacard.database.MediaTable
import org.ensodai.avalonmediacard.database.UserMediaBindingTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [UserMediaBindingProvider::class])
class UserMediaBindingRepositoryImpl : UserMediaBindingProvider {

    private val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun observeActiveBinding(userId: Uuid, mediaId: String): Flow<UserMediaBinding?> = flow {
        emit(getActiveBinding(userId, mediaId))
        updates.collect {
            emit(getActiveBinding(userId, mediaId))
        }
    }

    override suspend fun getBinding(userId: Uuid, mediaId: String, sourceType: String): String? {
        return dbQuery {
            val parsed = MediaTable.selectAll().where { MediaTable.externalId eq mediaId }.firstOrNull()
                ?.get(MediaTable.id)?.value
                ?: return@dbQuery null
            UserMediaBindingTable
                .selectAll()
                .where {
                    (UserMediaBindingTable.userId eq userId) and
                            (UserMediaBindingTable.mediaId eq parsed) and
                            (UserMediaBindingTable.sourceType eq sourceType)
                }
                .firstOrNull()
                ?.get(UserMediaBindingTable.sourceId)
        }
    }

    override suspend fun getActiveBinding(userId: Uuid, mediaId: String): UserMediaBinding? {
        return dbQuery {
            val parsed = MediaTable.selectAll().where { MediaTable.externalId eq mediaId }.firstOrNull()
                ?.get(MediaTable.id)?.value
                ?: return@dbQuery null
            UserMediaBindingTable
                .selectAll()
                .where {
                    (UserMediaBindingTable.userId eq userId) and
                            (UserMediaBindingTable.mediaId eq parsed)
                }
                .firstOrNull()
                ?.let {
                    UserMediaBinding(
                        sourceType = it[UserMediaBindingTable.sourceType],
                        sourceId = it[UserMediaBindingTable.sourceId]
                    )
                }
        }
    }

    override suspend fun saveBinding(userId: Uuid, mediaId: String, sourceType: String, sourceId: String) {
        dbQuery {
            MediaTable.insertIgnore {
                it[id] = Uuid.random()
                it[catalogId] = "tmdb"
                it[externalId] = mediaId
                it[mediaType] = "movie" // Default fallback
            }
            val parsed = MediaTable.selectAll().where { MediaTable.externalId eq mediaId }.firstOrNull()
                ?.get(MediaTable.id)?.value
                ?: return@dbQuery

            val exists = UserMediaBindingTable
                .selectAll()
                .where {
                    (UserMediaBindingTable.userId eq userId) and
                            (UserMediaBindingTable.mediaId eq parsed)
                }
                .firstOrNull() != null

            if (exists) {
                UserMediaBindingTable.update({
                    (UserMediaBindingTable.userId eq userId) and
                            (UserMediaBindingTable.mediaId eq parsed)
                }) {
                    it[UserMediaBindingTable.sourceType] = sourceType
                    it[UserMediaBindingTable.sourceId] = sourceId
                }
            } else {
                UserMediaBindingTable.insert {
                    it[UserMediaBindingTable.userId] = userId
                    it[UserMediaBindingTable.mediaId] = parsed
                    it[UserMediaBindingTable.sourceType] = sourceType
                    it[UserMediaBindingTable.sourceId] = sourceId
                }
            }
        }
        updates.tryEmit(Unit)
    }

    override suspend fun deleteBinding(userId: Uuid, mediaId: String, sourceType: String) {
        dbQuery {
            val parsed = MediaTable.selectAll().where { MediaTable.externalId eq mediaId }.firstOrNull()
                ?.get(MediaTable.id)?.value
                ?: return@dbQuery
            UserMediaBindingTable.deleteWhere {
                (UserMediaBindingTable.userId eq userId) and
                        (UserMediaBindingTable.mediaId eq parsed) and
                        (UserMediaBindingTable.sourceType eq sourceType)
            }
        }
        updates.tryEmit(Unit)
    }

    override suspend fun deleteAllBindings(userId: Uuid, mediaId: String) {
        dbQuery {
            val parsed = MediaTable.selectAll().where { MediaTable.externalId eq mediaId }.firstOrNull()
                ?.get(MediaTable.id)?.value
                ?: return@dbQuery
            UserMediaBindingTable.deleteWhere {
                (UserMediaBindingTable.userId eq userId) and
                        (UserMediaBindingTable.mediaId eq parsed)
            }
        }
        updates.tryEmit(Unit)
    }
}
