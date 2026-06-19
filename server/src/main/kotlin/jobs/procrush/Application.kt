package jobs.procrush

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import jobs.procrush.auth.route.authRoutes
import jobs.procrush.bootstrap.AppContext
import jobs.procrush.bootstrap.DatabaseFactory
import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.plugins.configureCallLogging
import jobs.procrush.bootstrap.plugins.configureCors
import jobs.procrush.bootstrap.plugins.configureSerialization
import jobs.procrush.bootstrap.plugins.configureStatusPages
import jobs.procrush.employer.route.employerRoutes
import jobs.procrush.personality.route.personalityProfileRoutes
import jobs.procrush.seeker.route.seekerProfileRoutes
import jobs.procrush.shared.route.referenceRoutes
import jobs.procrush.survey.route.seekerSurveyRoutes

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "::", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val config = AppConfig.fromEnvironment()
    DatabaseFactory.init(config)
    val app = AppContext.create(config)

    configureSerialization()
    configureStatusPages()
    configureCallLogging()
    configureCors(config)
    install(SSE)

    routing {
        get("/") {
            call.respondText("ProCrush API")
        }
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        authRoutes(app.config, app.userAuthService, app.sessionService, app.roleGuard)
        referenceRoutes(app.roleGuard, app.referenceRepository)
        seekerProfileRoutes(app.roleGuard, app.seekerProfileService)
        personalityProfileRoutes(app.roleGuard, app.personalityProfileService)
        seekerSurveyRoutes(app.roleGuard, app.surveyService, app.personalityProfileService)
        employerRoutes(app.roleGuard, app.employerProfileService)
    }
}
