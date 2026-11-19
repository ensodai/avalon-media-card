package org.ensodai.avalonmediacard.data.repository

import org.ensodai.avalonmediacard.contract.model.UserStatsDto
import org.ensodai.avalonmediacard.contract.rpc.StatsRpcService
import org.ensodai.avalonmediacard.domain.repository.StatsRepository
import org.koin.core.annotation.Single

@Single
class StatsRepositoryImpl(
    private val statsRpcService: StatsRpcService
) : StatsRepository {
    override suspend fun getUserStats(): UserStatsDto {
        return statsRpcService.getUserStats()
    }
}
