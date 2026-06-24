package jobs.procrush.matching.cache

import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisClient

object MatchingCacheKeys {
    fun seekerJobs(config: RedisConfig, seekerId: Long): String =
        config.key("rec", "seeker", seekerId.toString(), "jobs")

    fun jobCandidates(config: RedisConfig, jobProfileId: Long): String =
        config.key("rec", "job", jobProfileId.toString(), "candidates")
}

class MatchingCacheInvalidator(
    private val redis: RedisClient,
    private val config: RedisConfig,
) {
    fun invalidateSeekerJobs(seekerId: Long) {
        redis.del(MatchingCacheKeys.seekerJobs(config, seekerId))
    }

    fun invalidateJobCandidates(jobProfileId: Long) {
        redis.del(MatchingCacheKeys.jobCandidates(config, jobProfileId))
    }
}
