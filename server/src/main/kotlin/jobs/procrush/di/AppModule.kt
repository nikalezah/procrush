package jobs.procrush.di

import jobs.procrush.auth.RoleGuard
import jobs.procrush.auth.SessionService
import jobs.procrush.auth.UserAuthService
import jobs.procrush.auth.UserProfileEnricher
import jobs.procrush.config.AppConfig
import jobs.procrush.db.EmployerRepository
import jobs.procrush.db.ReferenceRepository
import jobs.procrush.db.SeekerPersonalProfileRepository
import jobs.procrush.db.SeekerRepository
import jobs.procrush.db.SeekerSuperpowersAndTalentsRepository
import jobs.procrush.db.SessionRepository
import jobs.procrush.db.SurveyRepository
import jobs.procrush.db.UserRepository
import jobs.procrush.domain.PersonalityProfileValidator
import jobs.procrush.domain.PersonalityPromptBuilder
import jobs.procrush.domain.ProfileProvisioningService
import jobs.procrush.domain.SurveyService
import jobs.procrush.domain.employer.EmployerProfileService
import jobs.procrush.domain.personality.PersonalityGenerationCoordinator
import jobs.procrush.domain.personality.PersonalityProfileGenerator
import jobs.procrush.domain.personality.PersonalityProfileReader
import jobs.procrush.domain.personality.PersonalityProfileService
import jobs.procrush.domain.seeker.SeekerProfileService
import jobs.procrush.llm.LlmFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
            val userRepository = UserRepository()
            val sessionRepository = SessionRepository()
            val seekerRepository = SeekerRepository()
            val referenceRepository = ReferenceRepository()
            val employerRepository = EmployerRepository(referenceRepository)
            val profileProvisioningService = ProfileProvisioningService(seekerRepository, employerRepository)
            val profileEnricher = UserProfileEnricher(seekerRepository, employerRepository)
            val userAuthService = UserAuthService(userRepository, profileProvisioningService, profileEnricher)
            val sessionService =
                SessionService(config, sessionRepository, userRepository, profileEnricher::enrich)
            val roleGuard = RoleGuard(config, sessionService)

            val surveyRepository = SurveyRepository()
            val surveyService = SurveyService(seekerRepository, surveyRepository)

            val profileRepository = SeekerPersonalProfileRepository()
            val superpowersRepository = SeekerSuperpowersAndTalentsRepository()
            val llmClient = LlmFactory.createClient(config.llm)
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val generator =
                PersonalityProfileGenerator(
                    seekerRepository = seekerRepository,
                    profileRepository = profileRepository,
                    referenceRepository = referenceRepository,
                    surveyService = surveyService,
                    llmConfig = config.llm,
                    llmClient = llmClient,
                    promptBuilder = PersonalityPromptBuilder(),
                    validator = PersonalityProfileValidator(),
                    scope = coroutineScope,
                )
            val coordinator =
                PersonalityGenerationCoordinator(
                    seekerRepository = seekerRepository,
                    profileRepository = profileRepository,
                    surveyService = surveyService,
                    generator = generator,
                )
            surveyService.attachPersonalityCoordinator(coordinator)

            val reader =
                PersonalityProfileReader(
                    seekerRepository = seekerRepository,
                    profileRepository = profileRepository,
                    superpowersRepository = superpowersRepository,
                    surveyService = surveyService,
                    generator = generator,
                )
            val personalityProfileService =
                PersonalityProfileService(
                    reader = reader,
                    coordinator = coordinator,
                    surveyService = surveyService,
                )

            val seekerProfileService = SeekerProfileService(seekerRepository, referenceRepository)
            val employerProfileService = EmployerProfileService(employerRepository, referenceRepository)

            sessionRepository.purgeExpired()

            return AppContext(
                config = config,
                userAuthService = userAuthService,
                sessionService = sessionService,
                roleGuard = roleGuard,
                seekerProfileService = seekerProfileService,
                employerProfileService = employerProfileService,
                surveyService = surveyService,
                personalityProfileService = personalityProfileService,
                referenceRepository = referenceRepository,
            )
        }
    }
}
