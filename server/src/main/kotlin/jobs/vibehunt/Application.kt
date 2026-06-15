package jobs.procrush

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jobs.procrush.auth.RoleGuard
import jobs.procrush.auth.SessionService
import jobs.procrush.auth.UserAuthService
import jobs.procrush.config.AppConfig
import jobs.procrush.db.DatabaseFactory
import jobs.procrush.db.EmployerRepository
import jobs.procrush.db.ReferenceRepository
import jobs.procrush.db.SeekerPersonalProfileRepository
import jobs.procrush.db.SeekerRepository
import jobs.procrush.db.SurveyRepository
import jobs.procrush.db.SessionRepository
import jobs.procrush.db.UserRepository
import jobs.procrush.domain.EmployerProfileService
import jobs.procrush.domain.PersonalityProfileService
import jobs.procrush.domain.PersonalityProfileValidator
import jobs.procrush.domain.PersonalityPromptBuilder
import jobs.procrush.domain.ProfileProvisioningService
import jobs.procrush.domain.SeekerProfileService
import jobs.procrush.domain.SurveyService
import jobs.procrush.llm.LlmClientFactory
import jobs.procrush.plugins.configureCallLogging
import jobs.procrush.plugins.configureCors
import jobs.procrush.plugins.configureSerialization
import jobs.procrush.routes.authRoutes
import jobs.procrush.routes.employerRoutes
import jobs.procrush.routes.referenceRoutes
import jobs.procrush.routes.seekerRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    // Railway private networking is IPv6; :: accepts both v4 and v6 on Linux.
    embeddedServer(Netty, port = port, host = "::", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val config = AppConfig.fromEnvironment()
    DatabaseFactory.init(config)

    val userRepository = UserRepository()
    val sessionRepository = SessionRepository()
    val seekerRepository = SeekerRepository()
    val employerRepository = EmployerRepository()
    val referenceRepository = ReferenceRepository()
    val profileProvisioningService = ProfileProvisioningService(seekerRepository, employerRepository)
    val userAuthService =
        UserAuthService(userRepository, profileProvisioningService, seekerRepository, employerRepository)
    val sessionService = SessionService(config, sessionRepository, userRepository, userAuthService::withProfileName)
    val roleGuard = RoleGuard(config, sessionService)
    val surveyRepository = SurveyRepository()
    val surveyService = SurveyService(seekerRepository, surveyRepository)
    val profileRepository = SeekerPersonalProfileRepository()
    val llmClient = LlmClientFactory.create(config.llm)
    val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val personalityProfileService =
        PersonalityProfileService(
            seekerRepository = seekerRepository,
            profileRepository = profileRepository,
            surveyService = surveyService,
            llmConfig = config.llm,
            llmClient = llmClient,
            promptBuilder = PersonalityPromptBuilder(),
            validator = PersonalityProfileValidator(),
            scope = coroutineScope,
        )
    surveyService.setProfileGenerationTrigger { userId ->
        personalityProfileService.maybeTriggerGeneration(userId)
    }
    val seekerProfileService =
        SeekerProfileService(seekerRepository, referenceRepository, surveyService, personalityProfileService)
    val employerProfileService = EmployerProfileService(employerRepository, referenceRepository)

    configureSerialization()
    configureCallLogging()
    configureCors(config)

    routing {
        get("/") {
            call.respondText("ProCrush API")
        }
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        authRoutes(config, userAuthService, sessionService)
        referenceRoutes(roleGuard, referenceRepository)
        seekerRoutes(roleGuard, seekerProfileService, surveyService, personalityProfileService)
        employerRoutes(roleGuard, employerProfileService)
    }
}
