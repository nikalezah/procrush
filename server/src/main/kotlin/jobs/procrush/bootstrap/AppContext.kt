package jobs.procrush.bootstrap

import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.auth.service.SessionService
import jobs.procrush.auth.service.UserAuthService
import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.modules.AuthModule
import jobs.procrush.bootstrap.modules.EmployerModule
import jobs.procrush.bootstrap.modules.MatchingModule
import jobs.procrush.bootstrap.modules.PersonalityModule
import jobs.procrush.bootstrap.modules.SeekerModule
import jobs.procrush.bootstrap.modules.SurveyModule
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.employer.service.EmployerProfileService
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.seeker.service.SeekerProfileService
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.service.SurveyService

data class AppContext(
    val config: AppConfig,
    val redisModule: RedisModule,
    val userAuthService: UserAuthService,
    val sessionService: SessionService,
    val roleGuard: RoleGuard,
    val seekerProfileService: SeekerProfileService,
    val employerProfileService: EmployerProfileService,
    val surveyService: SurveyService,
    val personalityProfileService: PersonalityProfileService,
    val matchInterestService: MatchInterestService,
    val referenceRepository: ReferenceRepository,
) {
    fun close() {
        redisModule.close()
    }

    companion object {
        fun create(config: AppConfig): AppContext {
            val redis = RedisModule.create(config)
            val auth = AuthModule.create(config, redis)
            val survey = SurveyModule.create(auth)
            val matching = MatchingModule.create(auth, survey, redis, config)
            redis.attachMatchInterestNotifier(matching.matchInterestNotifier)
            val personality =
                PersonalityModule.create(
                    config = config,
                    auth = auth,
                    survey = survey,
                    redis = redis,
                    matchingCacheInvalidator = matching.cacheInvalidator,
                )
            survey.attachPersonalityCoordinator(personality.coordinator)
            val seeker = SeekerModule.create(auth, matching, survey)
            val employer = EmployerModule.create(auth, matching)

            auth.sessionRepository.purgeExpired()

            return AppContext(
                config = config,
                redisModule = redis,
                userAuthService = auth.userAuthService,
                sessionService = auth.sessionService,
                roleGuard = auth.roleGuard,
                seekerProfileService = seeker.seekerProfileService,
                employerProfileService = employer.employerProfileService,
                surveyService = survey.surveyService,
                personalityProfileService = personality.personalityProfileService,
                matchInterestService = matching.matchInterestService,
                referenceRepository = auth.referenceRepository,
            )
        }
    }
}
