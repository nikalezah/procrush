package jobs.procrush.composition

import jobs.procrush.api.rabbitmq.RabbitMqModule
import jobs.procrush.auth.repository.SessionStore
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.matching.kafka.MatchingEventsRuntime
import jobs.procrush.matching.messaging.MatchResultsConsumer
import jobs.procrush.matching.service.RedisMatchInterestNotifier
import jobs.procrush.matching.service.RedisRecommendationsNotifier
import jobs.procrush.personality.PersonalityRuntime

internal class ApiRuntime(
    private val redisModule: RedisModule,
    private val rabbitMqModule: RabbitMqModule,
    private val matchingEventsRuntime: MatchingEventsRuntime,
    private val matchResultsConsumer: MatchResultsConsumer,
    private val matchInterestNotifier: RedisMatchInterestNotifier,
    private val recommendationsNotifier: RedisRecommendationsNotifier,
    private val personalityRuntime: PersonalityRuntime,
    private val deferredCoordinator: DeferredPersonalitySurveyCoordinator,
    private val sessionStore: SessionStore,
) {
    fun start() {
        deferredCoordinator.bind(personalityRuntime.coordinator)
        sessionStore.purgeExpired()
        matchInterestNotifier.start()
        recommendationsNotifier.start()
        matchResultsConsumer.start()
        personalityRuntime.start()
    }

    fun close() {
        personalityRuntime.close()
        matchResultsConsumer.stop()
        recommendationsNotifier.close()
        matchInterestNotifier.close()
        matchingEventsRuntime.close()
        rabbitMqModule.close()
        redisModule.close()
    }
}
