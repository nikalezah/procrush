package jobs.procrush.matching.service

import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.dto.RecommendationsUpdatedEventDto
import jobs.procrush.matching.events.MatchResultsUpdatedPayload
import jobs.procrush.matching.repository.MatchScoreRepository
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.seeker.repository.SeekerRepository
import org.slf4j.LoggerFactory

class MatchResultsApplyService(
    private val matchScoreRepository: MatchScoreRepository,
    private val matchingRepository: MatchingRepository,
    private val cacheInvalidator: MatchingCacheInvalidator,
    private val seekerRepository: SeekerRepository,
    private val employerRepository: EmployerRepository,
    private val recommendationsNotifier: RedisRecommendationsNotifier,
) {
    private val logger = LoggerFactory.getLogger(MatchResultsApplyService::class.java)

    fun apply(payload: MatchResultsUpdatedPayload) {
        val result = matchScoreRepository.applyResults(payload)
        result.affectedSeekerIds.forEach(cacheInvalidator::invalidateSeekerJobs)
        result.affectedJobProfileIds.forEach(cacheInvalidator::invalidateJobCandidates)

        val computedAt = result.computedAt.toString()
        result.affectedSeekerIds.forEach { seekerId ->
            val userId = seekerRepository.findUserIdBySeekerId(seekerId) ?: return@forEach
            recommendationsNotifier.scheduleNotify(
                userId = userId,
                event =
                    RecommendationsUpdatedEventDto(
                        scope = "seeker",
                        id = seekerId,
                        computedAt = computedAt,
                    ),
            )
        }
        result.affectedJobProfileIds.forEach { jobProfileId ->
            val job = matchingRepository.findJobProfileById(jobProfileId) ?: return@forEach
            val userId = employerRepository.findUserIdByEmployerId(job.employerId) ?: return@forEach
            recommendationsNotifier.scheduleNotify(
                userId = userId,
                event =
                    RecommendationsUpdatedEventDto(
                        scope = "job",
                        id = jobProfileId,
                        computedAt = computedAt,
                    ),
            )
        }
        logger.info(
            "Applied match.results_updated seekers={} jobs={} pairs={}",
            result.affectedSeekerIds.size,
            result.affectedJobProfileIds.size,
            payload.pairs.size,
        )
    }
}
