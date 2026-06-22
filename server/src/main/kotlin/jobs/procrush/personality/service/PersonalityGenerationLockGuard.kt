package jobs.procrush.personality.service

import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisDistributedLock
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.dto.SeekerPersonalProfileRecord

class PersonalityGenerationLockGuard(
    private val distributedLock: RedisDistributedLock,
    private val redisConfig: RedisConfig,
) {
    fun isJobActive(seekerId: Long): Boolean =
        distributedLock.isHeld(lockKey(seekerId))

    fun isStale(record: SeekerPersonalProfileRecord): Boolean =
        record.generationStatus == PersonalityProfileStatus.PROCESSING &&
            !isJobActive(record.seekerId)

    fun lockKey(seekerId: Long): String =
        redisConfig.key("lock", "personality", seekerId.toString())
}
