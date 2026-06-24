package jobs.procrush.bootstrap.redis

import java.util.UUID

class RedisDistributedLock(
    private val redis: RedisClient,
) {
    fun tryAcquire(
        key: String,
        ttlSeconds: Long,
    ): LockHandle? {
        val token = UUID.randomUUID().toString()
        val acquired =
            redis.setNxEx(
                key = key,
                value = token,
                seconds = ttlSeconds,
            )
        return if (acquired) LockHandle(key, token) else null
    }

    fun release(handle: LockHandle) {
        redis.releaseLock(handle.key, handle.token)
    }

    fun isHeld(key: String): Boolean = redis.exists(key)

    data class LockHandle(
        val key: String,
        val token: String,
    )
}
