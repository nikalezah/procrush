package jobs.procrush.personality.bootstrap

import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.bootstrap.modules.AuthModule
import jobs.procrush.bootstrap.modules.SurveyModule
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.llm.LlmFactory
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.personality.llm.PersonalityProfileLlmMapper
import jobs.procrush.personality.llm.PersonalityProfileValidator
import jobs.procrush.personality.llm.PersonalityPromptBuilder
import jobs.procrush.personality.messaging.PersonalityJobConsumer
import jobs.procrush.personality.messaging.PersonalityJobPublisher
import jobs.procrush.personality.messaging.PersonalityMessageDedup
import jobs.procrush.personality.service.PersonalityGenerationHandler
import jobs.procrush.personality.service.PersonalityGenerationLockGuard
import jobs.procrush.personality.service.RedisPersonalityStatusNotifier
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import kotlinx.coroutines.CoroutineScope

data class PersonalityWorkerModule(
    val consumer: PersonalityJobConsumer,
    val statusNotifier: RedisPersonalityStatusNotifier,
) {
    fun start() {
        statusNotifier.start()
        consumer.start()
    }

    fun stop() {
        consumer.stop()
        statusNotifier.close()
    }

    companion object {
        fun create(
            config: WorkerAppConfig,
            auth: AuthModule,
            survey: SurveyModule,
            redis: RedisModule,
            rabbitMq: RabbitMqModule,
            matchingCacheInvalidator: MatchingCacheInvalidator,
            matchingEvents: MatchingEventPort,
            scope: CoroutineScope,
        ): PersonalityWorkerModule {
            val profileRepository = SeekerPersonalProfileRepository()
            val lockGuard = PersonalityGenerationLockGuard(redis.distributedLock, config.redis)
            val publisher = PersonalityJobPublisher(rabbitMq.publishChannel, rabbitMq.config)
            val statusNotifier =
                RedisPersonalityStatusNotifier(
                    redis = redis.client,
                    config = config.redis,
                    scope = scope,
                )
            val handler =
                PersonalityGenerationHandler(
                    profileRepository = profileRepository,
                    referenceRepository = auth.referenceRepository,
                    surveyService = survey.surveyService,
                    llmConfig = config.llm,
                    llmClient = LlmFactory.createClient(config.llm),
                    promptBuilder = PersonalityPromptBuilder(),
                    validator = PersonalityProfileValidator(),
                    profileMapper = PersonalityProfileLlmMapper,
                    matchingCacheInvalidator = matchingCacheInvalidator,
                    matchingEvents = matchingEvents,
                )
            val dedup =
                PersonalityMessageDedup(
                    redis = redis.client,
                    config = config.redis,
                    rabbitMqConfig = rabbitMq.config,
                )
            val consumer =
                PersonalityJobConsumer(
                    rabbitMq = rabbitMq,
                    handler = handler,
                    publisher = publisher,
                    profileRepository = profileRepository,
                    statusNotifier = statusNotifier,
                    lockGuard = lockGuard,
                    distributedLock = redis.distributedLock,
                    dedup = dedup,
                    redisConfig = config.redis,
                    rabbitMqConfig = rabbitMq.config,
                )
            return PersonalityWorkerModule(
                consumer = consumer,
                statusNotifier = statusNotifier,
            )
        }
    }
}
