package jobs.procrush.matchingsvc.messaging

import jobs.procrush.bootstrap.config.KafkaConfig
import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisClient

class MatchingEventDedup(
    private val redis: RedisClient,
    private val config: RedisConfig,
    private val kafkaConfig: KafkaConfig,
) {
    fun tryMarkProcessing(eventId: String): Boolean =
        redis.setNxEx(
            dedupKey(eventId),
            "1",
            kafkaConfig.dedupTtlSeconds,
        )

    fun release(eventId: String) {
        redis.del(dedupKey(eventId))
    }

    private fun dedupKey(eventId: String): String = config.key("dedup", "matching", eventId)
}
