package jobs.procrush.personality.service

import jobs.procrush.personality.dto.PersonalityProfileStatus
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PersonalityGenerationNotifier {
    private val waiters = ConcurrentHashMap<UUID, MutableSet<CompletableDeferred<PersonalityProfileStatus>>>()

    fun register(userId: UUID): CompletableDeferred<PersonalityProfileStatus> {
        val deferred = CompletableDeferred<PersonalityProfileStatus>()
        waiters.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(deferred)
        return deferred
    }

    fun notify(userId: UUID, status: PersonalityProfileStatus) {
        val pending = waiters.remove(userId) ?: return
        pending.forEach { it.complete(status) }
    }

    fun cancel(userId: UUID, deferred: CompletableDeferred<PersonalityProfileStatus>) {
        waiters[userId]?.remove(deferred)
        if (waiters[userId]?.isEmpty() == true) {
            waiters.remove(userId)
        }
    }
}
