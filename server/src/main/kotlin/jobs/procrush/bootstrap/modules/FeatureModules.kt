package jobs.procrush.bootstrap.modules

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.llm.LlmFactory
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.personality.llm.PersonalityProfileValidator
import jobs.procrush.personality.llm.PersonalityPromptBuilder
import jobs.procrush.personality.messaging.PersonalityJobConsumer
import jobs.procrush.personality.messaging.PersonalityJobPublisher
import jobs.procrush.personality.messaging.PersonalityMessageDedup
import jobs.procrush.personality.service.PersonalityGenerationCoordinator
import jobs.procrush.personality.service.PersonalityGenerationHandler
import jobs.procrush.personality.service.PersonalityGenerationLockGuard
import jobs.procrush.personality.service.PersonalityProfileReader
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.personality.service.RedisPersonalityStatusNotifier
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.seeker.repository.SeekerSuperpowersAndTalentsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class SurveyModule(
    val surveyService: jobs.procrush.survey.service.SurveyService,
) {
    fun attachPersonalityCoordinator(coordinator: PersonalityGenerationCoordinator) {
        surveyService.attachPersonalityCoordinator(coordinator)
    }

    companion object {
        fun create(auth: AuthModule): SurveyModule {
            val surveyRepository = jobs.procrush.survey.repository.SurveyRepository()
            val surveyService =
                jobs.procrush.survey.service.SurveyService(auth.seekerRepository, surveyRepository)
            return SurveyModule(surveyService = surveyService)
        }
    }
}

data class PersonalityModule(
    val coordinator: PersonalityGenerationCoordinator,
    val personalityProfileService: PersonalityProfileService,
    val personalityStatusNotifier: RedisPersonalityStatusNotifier,
) {
    companion object {
        fun create(
            config: AppConfig,
            auth: AuthModule,
            survey: SurveyModule,
            redis: RedisModule,
            rabbitMq: RabbitMqModule,
            matchingCacheInvalidator: MatchingCacheInvalidator,
            scope: CoroutineScope,
        ): PersonalityModule {
            val profileRepository = SeekerPersonalProfileRepository()
            val superpowersRepository = SeekerSuperpowersAndTalentsRepository()
            val lockGuard = PersonalityGenerationLockGuard(redis.distributedLock, config.redis)
            val publisher = PersonalityJobPublisher(rabbitMq.publishChannel, rabbitMq.config)
            val personalityStatusNotifier =
                RedisPersonalityStatusNotifier(
                    redis = redis.client,
                    config = config.redis,
                    scope = scope,
                )
            val coordinator =
                PersonalityGenerationCoordinator(
                    seekerRepository = auth.seekerRepository,
                    profileRepository = profileRepository,
                    surveyService = survey.surveyService,
                    lockGuard = lockGuard,
                    publisher = publisher,
                    matchingCacheInvalidator = matchingCacheInvalidator,
                )
            val reader =
                PersonalityProfileReader(
                    seekerRepository = auth.seekerRepository,
                    profileRepository = profileRepository,
                    superpowersRepository = superpowersRepository,
                    surveyService = survey.surveyService,
                    lockGuard = lockGuard,
                )
            val personalityProfileService =
                PersonalityProfileService(
                    reader = reader,
                    coordinator = coordinator,
                    surveyService = survey.surveyService,
                    notifier = personalityStatusNotifier,
                )
            personalityStatusNotifier.start()
            return PersonalityModule(
                coordinator = coordinator,
                personalityProfileService = personalityProfileService,
                personalityStatusNotifier = personalityStatusNotifier,
            )
        }
    }
}

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
            config: AppConfig,
            auth: AuthModule,
            survey: SurveyModule,
            redis: RedisModule,
            rabbitMq: RabbitMqModule,
            matchingCacheInvalidator: MatchingCacheInvalidator,
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
                    matchingCacheInvalidator = matchingCacheInvalidator,
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

data class MatchingModule(
    val matchingService: jobs.procrush.matching.cache.CachedMatchingService,
    val matchInterestService: jobs.procrush.matching.service.MatchInterestService,
    val matchInterestNotifier: jobs.procrush.matching.service.RedisMatchInterestNotifier,
    val cacheInvalidator: MatchingCacheInvalidator,
) {
    companion object {
        fun create(
            auth: AuthModule,
            survey: SurveyModule,
            redis: RedisModule,
            config: AppConfig,
        ): MatchingModule {
            val matchingRepository =
                jobs.procrush.matching.repository.MatchingRepository(auth.referenceRepository)
            val matchInterestRepository = jobs.procrush.matching.repository.MatchInterestRepository()
            val cacheInvalidator =
                MatchingCacheInvalidator(redis.client, config.redis)
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val matchInterestNotifier =
                jobs.procrush.matching.service.RedisMatchInterestNotifier(
                    redis = redis.client,
                    config = config.redis,
                    scope = coroutineScope,
                )
            val coreMatchingService =
                jobs.procrush.matching.service.MatchingService(
                    seekerRepository = auth.seekerRepository,
                    matchingRepository = matchingRepository,
                    surveyService = survey.surveyService,
                )
            val matchingService =
                jobs.procrush.matching.cache.CachedMatchingService(
                    delegate = coreMatchingService,
                    resolveSeekerId = { userId -> auth.seekerRepository.findByUserId(userId)?.id },
                    redis = redis.client,
                    config = config.redis,
                )
            val matchInterestService =
                jobs.procrush.matching.service.MatchInterestService(
                    seekerRepository = auth.seekerRepository,
                    employerRepository = auth.employerRepository,
                    matchingService = matchingService,
                    matchingRepository = matchingRepository,
                    matchInterestRepository = matchInterestRepository,
                    surveyService = survey.surveyService,
                    notifier = matchInterestNotifier,
                )
            matchInterestNotifier.start()
            return MatchingModule(
                matchingService = matchingService,
                matchInterestService = matchInterestService,
                matchInterestNotifier = matchInterestNotifier,
                cacheInvalidator = cacheInvalidator,
            )
        }
    }
}

data class SeekerModule(
    val seekerProfileService: jobs.procrush.seeker.service.SeekerProfileService,
) {
    companion object {
        fun create(
            auth: AuthModule,
            matching: MatchingModule,
            survey: SurveyModule,
        ): SeekerModule {
            val seekerProfileService =
                jobs.procrush.seeker.service.SeekerProfileService(
                    auth.seekerRepository,
                    auth.referenceRepository,
                    matching.matchingService,
                    matching.matchInterestService,
                    survey.surveyService,
                    matching.cacheInvalidator,
                )
            return SeekerModule(seekerProfileService = seekerProfileService)
        }
    }
}

data class EmployerModule(
    val employerProfileService: jobs.procrush.employer.service.EmployerProfileService,
) {
    companion object {
        fun create(auth: AuthModule, matching: MatchingModule): EmployerModule {
            val employerProfileService =
                jobs.procrush.employer.service.EmployerProfileService(
                    auth.employerRepository,
                    auth.referenceRepository,
                    matching.matchingService,
                    matching.matchInterestService,
                    matching.cacheInvalidator,
                )
            return EmployerModule(employerProfileService = employerProfileService)
        }
    }
}
