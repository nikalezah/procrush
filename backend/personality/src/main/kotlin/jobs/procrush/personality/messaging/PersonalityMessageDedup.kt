package jobs.procrush.personality.messaging

import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisClient

class PersonalityMessageDedup(
    private val redis: RedisClient,
    private val config: RedisConfig,
    private val rabbitMqConfig: jobs.procrush.bootstrap.config.RabbitMqConfig,
) {
    fun tryMarkProcessing(messageId: String): Boolean =
        redis.setNxEx(
            dedupKey(messageId),
            "1",
            rabbitMqConfig.dedupTtlSeconds,
        )

    fun release(messageId: String) {
        redis.del(dedupKey(messageId))
    }

    private fun dedupKey(messageId: String): String = config.key("dedup", "personality", messageId)
}
