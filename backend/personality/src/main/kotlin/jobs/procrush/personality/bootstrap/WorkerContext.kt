package jobs.procrush.personality.bootstrap

import jobs.procrush.bootstrap.DatabaseFactory
import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.kafka.MatchingEventsRuntime
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.personality.messaging.PersonalityJobConsumer
import jobs.procrush.personality.port.PersonalitySurveyCoordinator
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.repository.SurveyRepository
import jobs.procrush.survey.service.SurveyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID

private object NoOpPersonalitySurveyCoordinator : PersonalitySurveyCoordinator {
    override fun onAllSurveysCompleted(userId: UUID) = Unit
}

data class WorkerContext(
    val config: WorkerAppConfig,
    val redisModule: RedisModule,
    val rabbitMqModule: RabbitMqModule,
    val personalityJobConsumer: PersonalityJobConsumer,
    private val matchingEventsRuntime: MatchingEventsRuntime,
    private val workerModule: PersonalityWorkerModule,
) {
    fun close() {
        workerModule.stop()
        matchingEventsRuntime.close()
        rabbitMqModule.close()
        redisModule.close()
    }

    companion object {
        fun create(config: WorkerAppConfig): WorkerContext {
            DatabaseFactory.init(config, runMigrations = false)
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val redis = RedisModule.create(config)
            val rabbitMq = RabbitMqModule.create(config.rabbitMq)
            val referenceRepository = ReferenceRepository()
            val seekerRepository = SeekerRepository()
            val employerRepository = EmployerRepository(referenceRepository)
            val matchingRepository = MatchingRepository(referenceRepository)
            val matchingEvents =
                MatchingEventsRuntime.create(
                    kafka = config.kafka,
                    seekerRepository = seekerRepository,
                    employerRepository = employerRepository,
                    matchingRepository = matchingRepository,
                    referenceRepository = referenceRepository,
                )
            val surveyService =
                SurveyService(
                    seekerRepository = seekerRepository,
                    surveyRepository = SurveyRepository(),
                    personalityCoordinator = NoOpPersonalitySurveyCoordinator,
                )
            val cacheInvalidator = MatchingCacheInvalidator(redis.client, config.redis)
            val workerModule =
                PersonalityWorkerModule.create(
                    config = config,
                    referenceRepository = referenceRepository,
                    surveyService = surveyService,
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
                personalityJobConsumer = workerModule.consumer,
                matchingEventsRuntime = matchingEvents,
                workerModule = workerModule,
            )
        }
    }
}
