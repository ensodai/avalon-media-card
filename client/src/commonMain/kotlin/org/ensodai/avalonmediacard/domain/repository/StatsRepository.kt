package org.ensodai.avalonmediacard.domain.repository

import org.ensodai.avalonmediacard.contract.model.DayActivityDto
import org.ensodai.avalonmediacard.contract.model.UserStatsDto

interface StatsRepository {
    suspend fun getUserStats(): UserStatsDto
    suspend fun getDayActivity(date: String): DayActivityDto
}
