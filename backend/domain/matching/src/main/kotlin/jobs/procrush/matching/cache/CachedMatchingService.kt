package jobs.procrush.matching.cache

import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisClient
import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.JobRecommendationDto
import jobs.procrush.matching.service.MatchingQueries
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class CachedMatchingService(
    private val delegate: MatchingQueries,
    private val resolveSeekerId: (UUID) -> Long?,
    private val redis: RedisClient,
    private val config: RedisConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(CachedMatchingService::class.java)

    private val jobListSerializer = ListSerializer(JobRecommendationDto.serializer())
    private val candidateListSerializer = ListSerializer(CandidateRecommendationDto.serializer())

    fun jobRecommendationsForSeeker(userId: UUID): List<JobRecommendationDto> {
        val seekerId = resolveSeekerId(userId) ?: return emptyList()
        val cacheKey = MatchingCacheKeys.seekerJobs(config, seekerId)
        redis.get(cacheKey)?.let { cached ->
            logger.debug("recommendation cache HIT seekerId={}", seekerId)
            return json.decodeFromString(jobListSerializer, cached)
        }
        logger.debug("recommendation cache MISS seekerId={}", seekerId)
        val recommendations = delegate.jobRecommendationsForSeeker(userId)
        if (recommendations.isNotEmpty()) {
            redis.setEx(
                cacheKey,
                config.recommendationCacheTtlSeconds,
                json.encodeToString(jobListSerializer, recommendations),
            )
        }
        return recommendations
    }

    fun candidateRecommendationsForJob(
        occupationId: Long,
        jobProfileId: Long,
    ): List<CandidateRecommendationDto> {
        val cacheKey = MatchingCacheKeys.jobCandidates(config, jobProfileId)
        redis.get(cacheKey)?.let { cached ->
            logger.debug("recommendation cache HIT jobProfileId={}", jobProfileId)
            return json.decodeFromString(candidateListSerializer, cached)
        }
        logger.debug("recommendation cache MISS jobProfileId={}", jobProfileId)
        val candidates = delegate.candidateRecommendationsForJob(occupationId, jobProfileId)
        if (candidates.isNotEmpty()) {
            redis.setEx(
                cacheKey,
                config.recommendationCacheTtlSeconds,
                json.encodeToString(candidateListSerializer, candidates),
            )
        }
        return candidates
    }

    fun jobRecommendationForSeeker(seekerId: Long, jobProfileId: Long): JobRecommendationDto? =
        delegate.jobRecommendationForSeeker(seekerId, jobProfileId)

    fun candidateRecommendationForJob(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? =
        delegate.candidateRecommendationForJob(seekerId, jobProfileId)

    fun jobRecommendationDisplay(jobProfileId: Long): JobRecommendationDto? =
        delegate.jobRecommendationDisplay(jobProfileId)

    fun candidateRecommendationDisplay(seekerId: Long, jobProfileId: Long): CandidateRecommendationDto? =
        delegate.candidateRecommendationDisplay(seekerId, jobProfileId)

    fun countMatchedCandidatesForOccupation(occupationId: Long): Int =
        delegate.countMatchedCandidatesForOccupation(occupationId)

    fun countMatchedCandidatesForOccupations(occupationIds: List<Long>): Map<Long, Int> =
        delegate.countMatchedCandidatesForOccupations(occupationIds)
}
