package jobs.procrush.composition

import jobs.procrush.api.handler.ApiHandlers
import jobs.procrush.api.handler.AuthHandler
import jobs.procrush.api.handler.EmployerHandler
import jobs.procrush.api.handler.ReferenceHandler
import jobs.procrush.api.handler.SeekerPersonalityHandler
import jobs.procrush.api.handler.SeekerProfileHandler
import jobs.procrush.api.handler.SeekerSurveyHandler
import jobs.procrush.api.rabbitmq.RabbitMqModule
import jobs.procrush.auth.RoleGuard
import jobs.procrush.auth.UserAuthService
import jobs.procrush.auth.service.SessionService
import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.employer.service.EmployerProfileService
import jobs.procrush.matching.kafka.MatchingEventsRuntime
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.matching.service.RecommendationsEventService
import jobs.procrush.personality.PersonalityRuntime
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.seeker.service.SeekerProfileService
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.service.SurveyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class AppContext(
    val config: AppConfig,
    val redisModule: RedisModule,
    val rabbitMqModule: RabbitMqModule,
    private val matchingEventsRuntime: MatchingEventsRuntime,
    private val matchingModule: MatchingModule,
    val userAuthService: UserAuthService,
    val sessionService: SessionService,
    val roleGuard: RoleGuard,
    val seekerProfileService: SeekerProfileService,
    val employerProfileService: EmployerProfileService,
    val surveyService: SurveyService,
    val personalityProfileService: PersonalityProfileService,
    val matchInterestService: MatchInterestService,
    val recommendationsEventService: RecommendationsEventService,
    val referenceRepository: ReferenceRepository,
    val handlers: ApiHandlers,
    private val personalityRuntime: PersonalityRuntime,
) {
    fun close() {
        personalityRuntime.close()
        matchingModule.close()
        matchingEventsRuntime.close()
        rabbitMqModule.close()
        redisModule.close()
    }

    companion object {
        fun create(config: AppConfig): AppContext {
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val redis = RedisModule.create(config)
            val rabbitMq = RabbitMqModule.create(config.rabbitMq)
            val auth = AuthModule.create(config, redis)
            val deferredCoordinator = DeferredPersonalitySurveyCoordinator()
            val survey = SurveyModule.create(auth, deferredCoordinator)
            val matchingRepository = MatchingRepository(auth.referenceRepository)
            val matchingEvents =
                MatchingEventsRuntime.create(
                    kafka = config.kafka,
                    seekerRepository = auth.seekerRepository,
                    matchingRepository = matchingRepository,
                )
            val matching = MatchingModule.create(auth, survey, redis, config)
            val personality =
                PersonalityRuntime.create(
                    redisConfig = config.redis,
                    rabbitMqConfig = config.rabbitMq,
                    distributedLock = redis.distributedLock,
                    redis = redis.client,
                    messagePublisher = rabbitMq.publisher,
                    messageConsumer = rabbitMq.createConsumer(),
                    seekerRepository = auth.seekerRepository,
                    referenceRepository = auth.referenceRepository,
                    surveyService = survey.surveyService,
                    matchingCache = matching.cacheInvalidator,
                    matchingEvents = matchingEvents.eventPort,
                    scope = coroutineScope,
                )
            deferredCoordinator.bind(personality.coordinator)
            val seeker = SeekerModule.create(auth, matching, survey, matchingEvents.eventPort)
            val employer = EmployerModule.create(auth, matching, matchingEvents.eventPort)

            auth.sessionRepository.purgeExpired()

            val handlers =
                ApiHandlers(
                    auth = AuthHandler(config, auth.userAuthService, auth.sessionService, auth.roleGuard),
                    reference = ReferenceHandler(auth.roleGuard, auth.referenceRepository),
                    seekerProfile = SeekerProfileHandler(auth.roleGuard, seeker.seekerProfileService, matching.matchInterestService),
                    seekerSurvey = SeekerSurveyHandler(auth.roleGuard, survey.surveyService, personality.personalityProfileService),
                    seekerPersonality = SeekerPersonalityHandler(auth.roleGuard, personality.personalityProfileService),
                    employer = EmployerHandler(auth.roleGuard, employer.employerProfileService, matching.matchInterestService),
                )

            return AppContext(
                config = config,
                redisModule = redis,
                rabbitMqModule = rabbitMq,
                userAuthService = auth.userAuthService,
                sessionService = auth.sessionService,
                roleGuard = auth.roleGuard,
                seekerProfileService = seeker.seekerProfileService,
                employerProfileService = employer.employerProfileService,
                surveyService = survey.surveyService,
                personalityProfileService = personality.personalityProfileService,
                matchInterestService = matching.matchInterestService,
                recommendationsEventService = matching.recommendationsEventService,
                referenceRepository = auth.referenceRepository,
                personalityRuntime = personality,
                matchingEventsRuntime = matchingEvents,
                matchingModule = matching,
                handlers = handlers,
            )
        }
    }
}
