package jobs.procrush.bootstrap.modules

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.client.MatchingServiceClient
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.matching.service.HttpMatchingQueries
import jobs.procrush.personality.messaging.PersonalityJobPublisher
import jobs.procrush.personality.service.PersonalityGenerationCoordinator
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
            matchingEvents: MatchingEventPort,
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
            personalityStatusNotifier.start()
            return PersonalityModule(
                coordinator = coordinator,
                personalityProfileService = personalityProfileService,
                personalityStatusNotifier = personalityStatusNotifier,
            )
        }
    }
}

data class MatchingModule(
    val matchingService: jobs.procrush.matching.cache.CachedMatchingService,
    val matchInterestService: jobs.procrush.matching.service.MatchInterestService,
    val matchInterestNotifier: jobs.procrush.matching.service.RedisMatchInterestNotifier,
    val cacheInvalidator: MatchingCacheInvalidator,
    val matchingClient: MatchingServiceClient,
) {
    fun close() {
        matchingClient.close()
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
            val matchInterestRepository = jobs.procrush.matching.repository.MatchInterestRepository()
            val cacheInvalidator = MatchingCacheInvalidator(redis.client, config.redis)
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val matchInterestNotifier =
                jobs.procrush.matching.service.RedisMatchInterestNotifier(
                    redis = redis.client,
                    config = config.redis,
                    scope = coroutineScope,
                )
            val matchingClient = MatchingServiceClient(config.matchingServiceUrl)
            val matchingQueries =
                HttpMatchingQueries(
                    client = matchingClient,
                    seekerRepository = auth.seekerRepository,
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
            redis.registerOnClose(matchInterestNotifier)
            matchInterestNotifier.start()
            return MatchingModule(
                matchingService = matchingService,
                matchInterestService = matchInterestService,
                matchInterestNotifier = matchInterestNotifier,
                cacheInvalidator = cacheInvalidator,
                matchingClient = matchingClient,
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
