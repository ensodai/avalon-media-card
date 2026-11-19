package org.ensodai.avalonmediacard.domain.repository

import org.ensodai.avalonmediacard.contract.model.UserStatsDto

interface StatsRepository {
    suspend fun getUserStats(): UserStatsDto
}
