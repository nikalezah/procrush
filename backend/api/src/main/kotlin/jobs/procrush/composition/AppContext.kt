package jobs.procrush.composition

import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.auth.service.SessionService
import jobs.procrush.auth.service.UserAuthService
import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.employer.service.EmployerProfileService
import jobs.procrush.matching.client.MatchingServiceClient
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.seeker.service.SeekerProfileService
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.service.SurveyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

data class AppContext(
    val config: AppConfig,
    val redisModule: RedisModule,
    val rabbitMqModule: RabbitMqModule,
    private val matchingEventsModule: MatchingEventsModule,
    private val matchingModule: MatchingModule,
    val matchingClient: MatchingServiceClient,
    val userAuthService: UserAuthService,
    val sessionService: SessionService,
    val roleGuard: RoleGuard,
    val seekerProfileService: SeekerProfileService,
    val employerProfileService: EmployerProfileService,
    val surveyService: SurveyService,
    val personalityProfileService: PersonalityProfileService,
    val matchInterestService: MatchInterestService,
    val referenceRepository: ReferenceRepository,
    private val personalityModule: PersonalityModule,
) {
    fun close() {
        personalityModule.personalityStatusNotifier.close()
        matchingModule.close()
        matchingEventsModule.close()
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
            val matchingRepository = jobs.procrush.matching.repository.MatchingRepository(auth.referenceRepository)
            val matchingEvents = MatchingEventsModule.create(config, auth, matchingRepository)
            val matching = MatchingModule.create(auth, survey, redis, config)
            val personality =
                PersonalityModule.create(
                    config = config,
                    auth = auth,
                    survey = survey,
                    redis = redis,
                    rabbitMq = rabbitMq,
                    matchingCache = matching.cacheInvalidator,
                    matchingEvents = matchingEvents.eventPort,
                    scope = coroutineScope,
                )
            deferredCoordinator.bind(personality.coordinator)
            val seeker = SeekerModule.create(auth, matching, survey, matchingEvents.eventPort)
            val employer = EmployerModule.create(auth, matching, matchingEvents.eventPort)

            auth.sessionRepository.purgeExpired()

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
                referenceRepository = auth.referenceRepository,
                personalityModule = personality,
                matchingEventsModule = matchingEvents,
                matchingModule = matching,
                matchingClient = matching.matchingClient,
            )
        }
    }
}

suspend fun AppContext.checkMatchingServiceHealth(): Boolean = matchingClient.pingHealth()

fun AppContext.checkMatchingServiceHealthBlocking(): Boolean = runBlocking { checkMatchingServiceHealth() }
