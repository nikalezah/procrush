package jobs.procrush.matching.cache

import jobs.procrush.bootstrap.redis.RedisTestSupport
import jobs.procrush.matching.dto.JobRecommendationDto
import jobs.procrush.matching.service.MatchingQueries
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Testcontainers
class CachedMatchingServiceTest : RedisTestSupport() {
    private val config = redisConfig()
    private val redis = redisClient()
    private val seekerId = 42L
    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000042")
    private val cacheKey = MatchingCacheKeys.seekerJobs(config, seekerId)

    private val recommendations =
        listOf(
            JobRecommendationDto(
                id = 1L,
                companyName = "Acme",
                positionName = "Developer",
                description = "Kotlin",
                matchScore = 0.8,
                matchScoreDisplay = 80,
            ),
        )

    private var delegateCalls = 0
    private lateinit var cachedService: CachedMatchingService

    @BeforeEach
    fun setUp() {
        redis.del(cacheKey)
        delegateCalls = 0
        val delegate =
            object : MatchingQueries {
                override fun jobRecommendationsForSeeker(userId: UUID): List<JobRecommendationDto> {
                    delegateCalls++
                    return recommendations
                }

                override fun candidateRecommendationsForJob(occupationId: Long, jobProfileId: Long) =
                    emptyList<jobs.procrush.matching.dto.CandidateRecommendationDto>()

                override fun jobRecommendationForSeeker(seekerId: Long, jobProfileId: Long) = null

                override fun candidateRecommendationForJob(seekerId: Long, jobProfileId: Long) = null

                override fun jobRecommendationFallback(jobProfileId: Long) = null

                override fun candidateRecommendationFallback(seekerId: Long, jobProfileId: Long) = null

                override fun countMatchedCandidatesForOccupation(occupationId: Long) = 0

                override fun countMatchedCandidatesForOccupations(occupationIds: List<Long>) = emptyMap<Long, Int>()
            }
        cachedService =
            CachedMatchingService(
                delegate = delegate,
                resolveSeekerId = { 42L },
                redis = redis,
                config = config,
            )
    }

    @Test
    fun cachesRecommendationsOnSecondCall() {
        assertEquals(recommendations, cachedService.jobRecommendationsForSeeker(userId))
        assertEquals(1, delegateCalls)
        assertNotNull(redis.get(cacheKey))

        assertEquals(recommendations, cachedService.jobRecommendationsForSeeker(userId))
        assertEquals(1, delegateCalls)
    }

    @Test
    fun invalidationForcesRecompute() {
        cachedService.jobRecommendationsForSeeker(userId)
        MatchingCacheInvalidator(redis, config).invalidateSeekerJobs(seekerId)
        assertNull(redis.get(cacheKey))

        cachedService.jobRecommendationsForSeeker(userId)
        assertEquals(2, delegateCalls)
    }
}
