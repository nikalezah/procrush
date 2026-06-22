package jobs.procrush.matching.service

import jobs.procrush.bootstrap.redis.RedisTestSupport
import jobs.procrush.matching.dto.InterestStatus
import jobs.procrush.matching.dto.MatchInterestEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Testcontainers
class RedisMatchInterestNotifierTest : RedisTestSupport() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notifier: RedisMatchInterestNotifier
    private val userId = UUID.randomUUID()

    private val sampleEvent =
        MatchInterestEventDto(
            jobProfileId = 1L,
            seekerId = 2L,
            interestStatus = InterestStatus.INCOMING,
        )

    @BeforeEach
    fun setUp() {
        notifier =
            RedisMatchInterestNotifier(
                redis = redisClient(),
                config = redisConfig(),
                scope = scope,
            )
        notifier.start()
    }

    @Test
    fun notifyDeliversEventToSubscriber() =
        runBlocking {
            val channel = notifier.subscribe(userId)
            val result = async { channel.receive() }
            notifier.notify(userId, sampleEvent)
            assertEquals(sampleEvent, result.await())
            notifier.unsubscribe(userId, channel)
        }

    @Test
    fun notifyDeliversToAllSubscribersForUser() =
        runBlocking {
            val first = notifier.subscribe(userId)
            val second = notifier.subscribe(userId)
            val firstResult = async { first.receive() }
            val secondResult = async { second.receive() }
            notifier.notify(userId, sampleEvent)
            assertEquals(sampleEvent, firstResult.await())
            assertEquals(sampleEvent, secondResult.await())
            notifier.unsubscribe(userId, first)
            notifier.unsubscribe(userId, second)
        }

    @Test
    fun unsubscribeStopsDelivery() =
        runBlocking {
            val channel = notifier.subscribe(userId)
            notifier.unsubscribe(userId, channel)
            notifier.notify(userId, sampleEvent)
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
            notifier.notify(otherUserId, sampleEvent)
            assertFalse(result.isCompleted)
            assertEquals(sampleEvent, otherResult.await())
            notifier.notify(userId, sampleEvent.copy(interestStatus = InterestStatus.MUTUAL))
            assertEquals(InterestStatus.MUTUAL, result.await().interestStatus)
            notifier.unsubscribe(userId, channel)
            notifier.unsubscribe(otherUserId, otherChannel)
        }

    @Test
    fun streamReceivesMultipleEvents() =
        runBlocking {
            val channel = notifier.subscribe(userId)
            val events = mutableListOf<MatchInterestEventDto>()
            val collector =
                launch {
                    repeat(2) {
                        events.add(channel.receive())
                    }
                }
            notifier.notify(userId, sampleEvent)
            notifier.notify(userId, sampleEvent.copy(interestStatus = InterestStatus.MUTUAL))
            collector.join()
            assertEquals(2, events.size)
            notifier.unsubscribe(userId, channel)
        }
}
