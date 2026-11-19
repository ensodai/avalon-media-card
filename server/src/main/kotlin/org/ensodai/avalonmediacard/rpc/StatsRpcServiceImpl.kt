package org.ensodai.avalonmediacard.rpc

import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.CriticProfileType
import org.ensodai.avalonmediacard.contract.model.LibraryStatsDto
import org.ensodai.avalonmediacard.contract.model.RatingDistributionDto
import org.ensodai.avalonmediacard.contract.model.TimeMilestoneDto
import org.ensodai.avalonmediacard.contract.model.TimeMilestoneType
import org.ensodai.avalonmediacard.contract.model.TimeStatsDto
import org.ensodai.avalonmediacard.contract.model.TopPeopleStatsDto
import org.ensodai.avalonmediacard.contract.model.UserStatsDto
import org.ensodai.avalonmediacard.contract.rpc.StatsRpcService
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.ensodai.avalonmediacard.service.UserStatsService
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class StatsRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val userStatsService: UserStatsService
) : StatsRpcService {

    override suspend fun getUserStats(): UserStatsDto {
        val state = session.state.value
        val userId = (state as? AuthState.Authorized)?.userId ?: return emptyUserStats()
        return userStatsService.getUserStats(userId)
    }

    private fun emptyUserStats(): UserStatsDto {
        return UserStatsDto(
            timeOverview = TimeStatsDto(
                totalMinutes = 0,
                moviesMinutes = 0,
                showsMinutes = 0,
                milestone = TimeMilestoneDto(
                    type = TimeMilestoneType.FIRST_STEPS,
                    param = 0
                )
            ),
            libraryOverview = LibraryStatsDto(
                totalMoviesCount = 0,
                totalEpisodesCount = 0,
                totalShowsCount = 0,
                watchingShowsCount = 0,
                completedShowsCount = 0,
                onHoldShowsCount = 0,
                dnfShowsCount = 0
            ),
            genreBreakdown = emptyList(),
            ratingDistribution = RatingDistributionDto(
                averageRating = null,
                totalRatedCount = 0,
                distribution = (1..10).associateWith { 0 },
                criticProfile = CriticProfileType.NO_RATINGS
            ),
            topPeople = TopPeopleStatsDto(
                directors = emptyList(),
                actors = emptyList()
            ),
            archetype = null,
            decadesBreakdown = emptyList()
        )
    }
}
