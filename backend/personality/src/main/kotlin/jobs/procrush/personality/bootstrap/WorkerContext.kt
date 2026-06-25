package jobs.procrush.personality.bootstrap

import jobs.procrush.bootstrap.DatabaseFactory
import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.composition.AuthModule
import jobs.procrush.composition.MatchingEventsModule
import jobs.procrush.composition.SurveyModule
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.repository.MatchingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class WorkerContext(
    val config: WorkerAppConfig,
    val redisModule: RedisModule,
    val rabbitMqModule: RabbitMqModule,
    private val matchingEventsModule: MatchingEventsModule,
    private val workerModule: PersonalityWorkerModule,
) {
    fun close() {
        workerModule.stop()
        matchingEventsModule.close()
        rabbitMqModule.close()
        redisModule.close()
    }

    companion object {
        fun create(config: WorkerAppConfig): WorkerContext {
            DatabaseFactory.init(config, runMigrations = false)
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val redis = RedisModule.create(config)
            val rabbitMq = RabbitMqModule.create(config.rabbitMq)
            val auth = AuthModule.create()
            val survey = SurveyModule.create(auth)
            val matchingRepository = MatchingRepository(auth.referenceRepository)
            val matchingEvents = MatchingEventsModule.create(config, auth, matchingRepository)
            val cacheInvalidator = MatchingCacheInvalidator(redis.client, config.redis)
            val workerModule =
                PersonalityWorkerModule.create(
                    config = config,
                    auth = auth,
                    survey = survey,
                    redis = redis,
                    rabbitMq = rabbitMq,
                    matchingCacheInvalidator = cacheInvalidator,
                    matchingEvents = matchingEvents.eventPort,
                    scope = coroutineScope,
                )
            workerModule.start()
            return WorkerContext(
                config = config,
                redisModule = redis,
                rabbitMqModule = rabbitMq,
                matchingEventsModule = matchingEvents,
                workerModule = workerModule,
            )
        }
    }
}
