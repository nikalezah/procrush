package jobs.procrush.bootstrap.modules

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.llm.LlmFactory
import jobs.procrush.personality.llm.PersonalityProfileValidator
import jobs.procrush.personality.llm.PersonalityPromptBuilder
import jobs.procrush.personality.service.PersonalityGenerationCoordinator
import jobs.procrush.personality.service.PersonalityProfileGenerator
import jobs.procrush.personality.service.PersonalityProfileReader
import jobs.procrush.personality.service.PersonalityProfileService
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
) {
    companion object {
        fun create(
            config: AppConfig,
            auth: AuthModule,
            survey: SurveyModule,
        ): PersonalityModule {
            val profileRepository = SeekerPersonalProfileRepository()
            val superpowersRepository = SeekerSuperpowersAndTalentsRepository()
            val llmClient = LlmFactory.createClient(config.llm)
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val generator =
                PersonalityProfileGenerator(
                    seekerRepository = auth.seekerRepository,
                    profileRepository = profileRepository,
                    referenceRepository = auth.referenceRepository,
                    surveyService = survey.surveyService,
                    llmConfig = config.llm,
                    llmClient = llmClient,
                    promptBuilder = PersonalityPromptBuilder(),
                    validator = PersonalityProfileValidator(),
                    scope = coroutineScope,
                )
            val coordinator =
                PersonalityGenerationCoordinator(
                    seekerRepository = auth.seekerRepository,
                    profileRepository = profileRepository,
                    surveyService = survey.surveyService,
                    generator = generator,
                )
            val reader =
                PersonalityProfileReader(
                    seekerRepository = auth.seekerRepository,
                    profileRepository = profileRepository,
                    superpowersRepository = superpowersRepository,
                    surveyService = survey.surveyService,
                    generator = generator,
                )
            val personalityProfileService =
                PersonalityProfileService(
                    reader = reader,
                    coordinator = coordinator,
                    surveyService = survey.surveyService,
                )
            return PersonalityModule(
                coordinator = coordinator,
                personalityProfileService = personalityProfileService,
            )
        }
    }
}

data class MatchingModule(
    val matchingService: jobs.procrush.matching.service.MatchingService,
) {
    companion object {
        fun create(auth: AuthModule, survey: SurveyModule): MatchingModule {
            val matchingRepository =
                jobs.procrush.matching.repository.MatchingRepository(auth.referenceRepository)
            val matchingService =
                jobs.procrush.matching.service.MatchingService(
                    seekerRepository = auth.seekerRepository,
                    matchingRepository = matchingRepository,
                    surveyService = survey.surveyService,
                )
            return MatchingModule(matchingService = matchingService)
        }
    }
}

data class SeekerModule(
    val seekerProfileService: jobs.procrush.seeker.service.SeekerProfileService,
) {
    companion object {
        fun create(auth: AuthModule, matching: MatchingModule): SeekerModule {
            val seekerProfileService =
                jobs.procrush.seeker.service.SeekerProfileService(
                    auth.seekerRepository,
                    auth.referenceRepository,
                    matching.matchingService,
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
                )
            return EmployerModule(employerProfileService = employerProfileService)
        }
    }
}
