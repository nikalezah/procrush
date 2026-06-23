package jobs.procrush.bootstrap

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.modules.AuthModule
import jobs.procrush.bootstrap.modules.MatchingEventsModule
import jobs.procrush.bootstrap.modules.PersonalityWorkerModule
import jobs.procrush.bootstrap.modules.SurveyModule
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class WorkerContext(
    val config: AppConfig,
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
        fun create(config: AppConfig): WorkerContext {
            DatabaseFactory.init(config, runMigrations = false)
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val redis = RedisModule.create(config)
            val rabbitMq = RabbitMqModule.create(config.rabbitMq)
            val auth = AuthModule.create(config, redis)
            val survey = SurveyModule.create(auth)
            val matchingRepository = jobs.procrush.matching.repository.MatchingRepository(auth.referenceRepository)
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
                    matchingEvents = matchingEvents,
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
