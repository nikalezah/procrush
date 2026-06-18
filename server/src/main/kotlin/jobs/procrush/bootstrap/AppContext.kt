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
import jobs.procrush.employer.service.EmployerProfileService
import jobs.procrush.personality.service.PersonalityProfileService
import jobs.procrush.seeker.service.SeekerProfileService
import jobs.procrush.shared.repository.ReferenceRepository
import jobs.procrush.survey.service.SurveyService

data class AppContext(
    val config: AppConfig,
    val userAuthService: UserAuthService,
    val sessionService: SessionService,
    val roleGuard: RoleGuard,
    val seekerProfileService: SeekerProfileService,
    val employerProfileService: EmployerProfileService,
    val surveyService: SurveyService,
    val personalityProfileService: PersonalityProfileService,
    val referenceRepository: ReferenceRepository,
) {
    companion object {
        fun create(config: AppConfig): AppContext {
            val auth = AuthModule.create(config)
            val survey = SurveyModule.create(auth)
            val personality = PersonalityModule.create(config, auth, survey)
            survey.attachPersonalityCoordinator(personality.coordinator)
            val matching = MatchingModule.create(auth, survey)
            val seeker = SeekerModule.create(auth, matching)
            val employer = EmployerModule.create(auth, matching)

            auth.sessionRepository.purgeExpired()

            return AppContext(
                config = config,
                userAuthService = auth.userAuthService,
                sessionService = auth.sessionService,
                roleGuard = auth.roleGuard,
                seekerProfileService = seeker.seekerProfileService,
                employerProfileService = employer.employerProfileService,
                surveyService = survey.surveyService,
                personalityProfileService = personality.personalityProfileService,
                referenceRepository = auth.referenceRepository,
            )
        }
    }
}
