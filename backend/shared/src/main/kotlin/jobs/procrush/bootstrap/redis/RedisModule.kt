package jobs.procrush.bootstrap.redis

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.matching.service.RedisMatchInterestNotifier

data class RedisModule(
    val client: RedisClient,
    val distributedLock: RedisDistributedLock,
    private var matchInterestNotifier: RedisMatchInterestNotifier? = null,
) {
    fun attachMatchInterestNotifier(notifier: RedisMatchInterestNotifier) {
        matchInterestNotifier = notifier
    }

    fun close() {
        matchInterestNotifier?.close()
        client.close()
    }

    companion object {
        fun create(config: AppConfig): RedisModule {
            val client = RedisClient.connect(config.redis)
            val distributedLock = RedisDistributedLock(client)
            return RedisModule(client = client, distributedLock = distributedLock)
        }
    }
}
