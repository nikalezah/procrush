package jobs.procrush.personality.service

import jobs.procrush.bootstrap.redis.RedisTestSupport
import jobs.procrush.personality.dto.PersonalityProfileStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Testcontainers
class RedisPersonalityStatusNotifierTest : RedisTestSupport() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notifier: RedisPersonalityStatusNotifier
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        notifier =
            RedisPersonalityStatusNotifier(
                redis = redisClient(),
                config = redisConfig(),
                scope = scope,
            )
        notifier.start()
    }

    @Test
    fun notifyDeliversStatusToSubscriber() =
        runBlocking {
            val channel = notifier.subscribe(userId)
            val result = async { channel.receive() }
            notifier.notify(userId, PersonalityProfileStatus.READY)
            assertEquals(PersonalityProfileStatus.READY, result.await())
            notifier.unsubscribe(userId, channel)
        }

    @Test
    fun notifyDeliversToAllSubscribersForUser() =
        runBlocking {
            val first = notifier.subscribe(userId)
            val second = notifier.subscribe(userId)
            val firstResult = async { first.receive() }
            val secondResult = async { second.receive() }
            notifier.notify(userId, PersonalityProfileStatus.FAILED)
            assertEquals(PersonalityProfileStatus.FAILED, firstResult.await())
            assertEquals(PersonalityProfileStatus.FAILED, secondResult.await())
            notifier.unsubscribe(userId, first)
            notifier.unsubscribe(userId, second)
        }

    @Test
    fun unsubscribeStopsDelivery() =
        runBlocking {
            val channel = notifier.subscribe(userId)
            notifier.unsubscribe(userId, channel)
            notifier.notify(userId, PersonalityProfileStatus.READY)
            assertTrue(channel.isClosedForReceive)
        }

    @Test
    fun notifyDoesNotAffectOtherUsers() =
        runBlocking {
            val otherUserId = UUID.randomUUID()
            val channel = notifier.subscribe(userId)
            val otherChannel = notifier.subscribe(otherUserId)
            val result = async { channel.receive() }
            val otherResult = async { otherChannel.receive() }
            notifier.notify(otherUserId, PersonalityProfileStatus.READY)
            assertFalse(result.isCompleted)
            assertEquals(PersonalityProfileStatus.READY, otherResult.await())
            notifier.notify(userId, PersonalityProfileStatus.FAILED)
            assertEquals(PersonalityProfileStatus.FAILED, result.await())
            notifier.unsubscribe(userId, channel)
            notifier.unsubscribe(otherUserId, otherChannel)
        }
}
