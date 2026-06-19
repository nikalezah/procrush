package jobs.procrush.personality.service

import jobs.procrush.personality.dto.PersonalityProfileStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PersonalityGenerationNotifierTest {
    private val notifier = PersonalityGenerationNotifier()
    private val userId = UUID.randomUUID()

    @Test
    fun notifyCompletesRegisteredWaiter() =
        runBlocking {
            val deferred = notifier.register(userId)
            val result = async { deferred.await() }
            notifier.notify(userId, PersonalityProfileStatus.READY)
            assertEquals(PersonalityProfileStatus.READY, result.await())
        }

    @Test
    fun notifyCompletesAllWaitersForUser() =
        runBlocking {
            val first = notifier.register(userId)
            val second = notifier.register(userId)
            val firstResult = async { first.await() }
            val secondResult = async { second.await() }
            notifier.notify(userId, PersonalityProfileStatus.FAILED)
            assertEquals(PersonalityProfileStatus.FAILED, firstResult.await())
            assertEquals(PersonalityProfileStatus.FAILED, secondResult.await())
        }

    @Test
    fun cancelRemovesWaiterWithoutCompleting() =
        runBlocking {
            val deferred = notifier.register(userId)
            notifier.cancel(userId, deferred)
            notifier.notify(userId, PersonalityProfileStatus.READY)
            assertFalse(deferred.isCompleted)
        }

    @Test
    fun notifyDoesNotAffectOtherUsers() =
        runBlocking {
            val otherUserId = UUID.randomUUID()
            val deferred = notifier.register(userId)
            val otherDeferred = notifier.register(otherUserId)
            val result = async { deferred.await() }
            val otherResult = async { otherDeferred.await() }
            notifier.notify(otherUserId, PersonalityProfileStatus.READY)
            assertFalse(result.isCompleted)
            assertEquals(PersonalityProfileStatus.READY, otherResult.await())
            notifier.notify(userId, PersonalityProfileStatus.FAILED)
            assertEquals(PersonalityProfileStatus.FAILED, result.await())
        }
}
