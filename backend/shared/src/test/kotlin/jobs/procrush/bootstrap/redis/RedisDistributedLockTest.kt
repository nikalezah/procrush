package jobs.procrush.bootstrap.redis

import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers
class RedisDistributedLockTest : RedisTestSupport() {
    private val lock = RedisDistributedLock(redisClient())

    @Test
    fun onlyOneLockHolderAtATime() {
        val key = redisConfig().key("lock", "test", "1")
        val first = lock.tryAcquire(key, ttlSeconds = 30)
        val second = lock.tryAcquire(key, ttlSeconds = 30)
        assertTrue(first != null)
        assertNull(second)
        lock.release(first)
        val third = lock.tryAcquire(key, ttlSeconds = 30)
        assertTrue(third != null)
        lock.release(third)
    }

    @Test
    fun releaseDoesNotDeleteForeignLock() {
        val key = redisConfig().key("lock", "test", "2")
        val first = lock.tryAcquire(key, ttlSeconds = 30)!!
        val second = lock.tryAcquire(key, ttlSeconds = 30)
        assertNull(second)
        val foreign = RedisDistributedLock.LockHandle(key, token = "foreign-token")
        lock.release(foreign)
        assertTrue(lock.isHeld(key))
        lock.release(first)
        assertFalse(lock.isHeld(key))
    }
}
