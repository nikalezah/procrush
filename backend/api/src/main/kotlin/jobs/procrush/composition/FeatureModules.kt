package jobs.procrush.composition

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.api.rabbitmq.RabbitMqModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.port.MatchingCachePort
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.personality.messaging.MessagingLog
import jobs.procrush.personality.messaging.PersonalityCommandPublisher
import jobs.procrush.personality.messaging.PersonalityResultConsumer
import jobs.procrush.personality.messaging.PersonalityResultDedup
import jobs.procrush.personality.port.PersonalitySurveyCoordinator
import jobs.procrush.personality.service.PersonalityGenerationCoordinator
import jobs.procrush.personality.service.PersonalityGenerationLockGuard
import jobs.procrush.personality.service.PersonalityProfileReader
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.personality.service.PersonalityResultApplyService
import jobs.procrush.personality.service.RedisPersonalityStatusNotifier
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import jobs.procrush.seeker.repository.SeekerSuperpowersAndTalentsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.LoggerFactory

data class SurveyModule(
    val surveyService: jobs.procrush.survey.service.SurveyService,
) {
    companion object {
        fun create(
            auth: AuthModule,
            personalityCoordinator: PersonalitySurveyCoordinator,
        ): SurveyModule {
            val surveyRepository = jobs.procrush.survey.repository.SurveyRepository()
            val surveyService =
                jobs.procrush.survey.service.SurveyService(
                    auth.seekerRepository,
                    surveyRepository,
                    personalityCoordinator,
                )
            return SurveyModule(surveyService = surveyService)
        }
    }
}

data class PersonalityModule(
    val coordinator: PersonalityGenerationCoordinator,
    val personalityProfileService: PersonalityProfileService,
    val personalityStatusNotifier: RedisPersonalityStatusNotifier,
    private val resultConsumer: PersonalityResultConsumer,
) {
    fun close() {
        resultConsumer.stop()
        personalityStatusNotifier.close()
    }

    companion object {
        fun create(
            config: AppConfig,
            auth: AuthModule,
            survey: SurveyModule,
            redis: RedisModule,
            rabbitMq: RabbitMqModule,
            matchingCache: MatchingCachePort,
            matchingEvents: MatchingEventPort,
            scope: CoroutineScope,
        ): PersonalityModule {
            val profileRepository = SeekerPersonalProfileRepository()
            val superpowersRepository = SeekerSuperpowersAndTalentsRepository()
            val lockGuard = PersonalityGenerationLockGuard(redis.distributedLock, config.redis)
            val commandLog = LoggerFactory.getLogger(PersonalityCommandPublisher::class.java)
            val publisher =
                PersonalityCommandPublisher(
                    rabbitMq.publisher,
                    rabbitMq.config,
                    MessagingLog { message, args -> commandLog.info(message, *args) },
                )
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
                    referenceRepository = auth.referenceRepository,
                    surveyService = survey.surveyService,
                    lockGuard = lockGuard,
                    publisher = publisher,
                    matchingCache = matchingCache,
                    matchingEvents = matchingEvents,
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
            val applyService =
                PersonalityResultApplyService(
                    profileRepository = profileRepository,
                    referenceRepository = auth.referenceRepository,
                    lockGuard = lockGuard,
                    statusNotifier = personalityStatusNotifier,
                    matchingCache = matchingCache,
                    matchingEvents = matchingEvents,
                )
            val resultConsumer =
                PersonalityResultConsumer(
                    messageConsumer = rabbitMq.createConsumer(),
                    applyService = applyService,
                    dedup =
                        PersonalityResultDedup(
                            redis = redis.client,
                            config = config.redis,
                            rabbitMqConfig = rabbitMq.config,
                        ),
                    rabbitMqConfig = rabbitMq.config,
                )
            personalityStatusNotifier.start()
            resultConsumer.start()
            return PersonalityModule(
                coordinator = coordinator,
                personalityProfileService = personalityProfileService,
                personalityStatusNotifier = personalityStatusNotifier,
                resultConsumer = resultConsumer,
            )
        }
    }
}

data class MatchingModule(
    val matchingService: jobs.procrush.matching.cache.CachedMatchingService,
    val matchInterestService: jobs.procrush.matching.service.MatchInterestService,
    val matchInterestNotifier: jobs.procrush.matching.service.RedisMatchInterestNotifier,
    val recommendationsEventService: jobs.procrush.matching.service.RecommendationsEventService,
    val recommendationsNotifier: jobs.procrush.matching.service.RedisRecommendationsNotifier,
    val cacheInvalidator: MatchingCacheInvalidator,
    private val matchResultsConsumer: jobs.procrush.matching.messaging.MatchResultsConsumer,
) {
    fun close() {
        matchResultsConsumer.stop()
        recommendationsNotifier.close()
    }

    companion object {
        fun create(
            auth: AuthModule,
            survey: SurveyModule,
            redis: RedisModule,
            config: AppConfig,
        ): MatchingModule {
            val matchingRepository =
                jobs.procrush.matching.repository.MatchingRepository(auth.referenceRepository)
            val matchScoreRepository = jobs.procrush.matching.repository.MatchScoreRepository()
            val matchInterestRepository = jobs.procrush.matching.repository.MatchInterestRepository()
            val cacheInvalidator = MatchingCacheInvalidator(redis.client, config.redis)
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val matchInterestNotifier =
                jobs.procrush.matching.service.RedisMatchInterestNotifier(
                    redis = redis.client,
                    config = config.redis,
                    scope = coroutineScope,
                )
            val recommendationsNotifier =
                jobs.procrush.matching.service.RedisRecommendationsNotifier(
                    redis = redis.client,
                    config = config.redis,
                    scope = coroutineScope,
                )
            val matchingQueries =
                jobs.procrush.matching.service.LocalMatchingQueries(
                    matchScoreRepository = matchScoreRepository,
                    matchingRepository = matchingRepository,
                    seekerRepository = auth.seekerRepository,
                    referenceRepository = auth.referenceRepository,
                )
            val matchingService =
                jobs.procrush.matching.cache.CachedMatchingService(
                    delegate = matchingQueries,
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
            val recommendationsEventService =
                jobs.procrush.matching.service.RecommendationsEventService(recommendationsNotifier)
            val applyService =
                jobs.procrush.matching.service.MatchResultsApplyService(
                    matchScoreRepository = matchScoreRepository,
                    matchingRepository = matchingRepository,
                    cacheInvalidator = cacheInvalidator,
                    seekerRepository = auth.seekerRepository,
                    employerRepository = auth.employerRepository,
                    recommendationsNotifier = recommendationsNotifier,
                )
            val matchResultsConsumer =
                jobs.procrush.matching.messaging.MatchResultsConsumer(
                    kafkaConfig = config.kafka,
                    applyService = applyService,
                    dedup =
                        jobs.procrush.matching.messaging.MatchResultsEventDedup(
                            redis = redis.client,
                            config = config.redis,
                            kafkaConfig = config.kafka,
                        ),
                )
            redis.registerOnClose(matchInterestNotifier)
            redis.registerOnClose(recommendationsNotifier)
            matchInterestNotifier.start()
            recommendationsNotifier.start()
            matchResultsConsumer.start()
            return MatchingModule(
                matchingService = matchingService,
                matchInterestService = matchInterestService,
                matchInterestNotifier = matchInterestNotifier,
                recommendationsEventService = recommendationsEventService,
                recommendationsNotifier = recommendationsNotifier,
                cacheInvalidator = cacheInvalidator,
                matchResultsConsumer = matchResultsConsumer,
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
            matchingEvents: MatchingEventPort,
        ): SeekerModule {
            val seekerProfileService =
                jobs.procrush.seeker.service.SeekerProfileService(
                    auth.seekerRepository,
                    auth.referenceRepository,
                    matching.matchingService,
                    matching.matchInterestService,
                    survey.surveyService,
                    matching.cacheInvalidator,
                    matchingEvents,
                )
            return SeekerModule(seekerProfileService = seekerProfileService)
        }
    }
}

data class EmployerModule(
    val employerProfileService: jobs.procrush.employer.service.EmployerProfileService,
) {
    companion object {
        fun create(
            auth: AuthModule,
            matching: MatchingModule,
            matchingEvents: MatchingEventPort,
        ): EmployerModule {
            val employerProfileService =
                jobs.procrush.employer.service.EmployerProfileService(
                    auth.employerRepository,
                    auth.referenceRepository,
                    matching.matchingService,
                    matching.matchInterestService,
                    matching.cacheInvalidator,
                    matchingEvents,
                )
            return EmployerModule(employerProfileService = employerProfileService)
        }
    }
}
