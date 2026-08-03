package jobs.procrush.matching.service

import jobs.procrush.matching.dto.RecommendationsUpdatedEventDto
import java.util.UUID

class RecommendationsEventService(
    private val notifier: RedisRecommendationsNotifier,
) {
    suspend fun streamEvents(
        userId: UUID,
        onEvent: suspend (RecommendationsUpdatedEventDto) -> Unit,
    ) {
        val channel = notifier.subscribe(userId)
        try {
            for (event in channel) {
                onEvent(event)
            }
        } finally {
            notifier.unsubscribe(userId, channel)
        }
    }
}
