package jobs.procrush.matching.service

import jobs.procrush.matching.dto.MatchInterestEventDto
import kotlinx.coroutines.channels.Channel
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MatchInterestNotifier {
    private val subscribers =
        ConcurrentHashMap<UUID, MutableSet<Channel<MatchInterestEventDto>>>()

    fun subscribe(userId: UUID): Channel<MatchInterestEventDto> {
        val channel = Channel<MatchInterestEventDto>(Channel.BUFFERED)
        subscribers.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(channel)
        return channel
    }

    fun unsubscribe(userId: UUID, channel: Channel<MatchInterestEventDto>) {
        subscribers[userId]?.remove(channel)
        channel.close()
        if (subscribers[userId]?.isEmpty() == true) {
            subscribers.remove(userId)
        }
    }

    fun notify(userId: UUID, event: MatchInterestEventDto) {
        subscribers[userId]?.forEach { it.trySend(event) }
    }
}
