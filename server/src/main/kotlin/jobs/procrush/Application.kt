package jobs.procrush

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import jobs.procrush.config.AppConfig
import jobs.procrush.db.DatabaseFactory
import jobs.procrush.di.AppContext
import jobs.procrush.plugins.configureCallLogging
import jobs.procrush.plugins.configureCors
import jobs.procrush.plugins.configureSerialization
import jobs.procrush.plugins.configureStatusPages
import jobs.procrush.routes.authRoutes
import jobs.procrush.routes.employerRoutes
import jobs.procrush.routes.referenceRoutes
import jobs.procrush.routes.seekerProfileRoutes
import jobs.procrush.routes.seekerSurveyRoutes

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

    routing {
        get("/") {
            call.respondText("ProCrush API")
        }
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        authRoutes(app.config, app.userAuthService, app.sessionService, app.roleGuard)
        referenceRoutes(app.roleGuard, app.referenceRepository)
        seekerProfileRoutes(app.roleGuard, app.seekerProfileService, app.personalityProfileService)
        seekerSurveyRoutes(app.roleGuard, app.surveyService, app.personalityProfileService)
        employerRoutes(app.roleGuard, app.employerProfileService)
    }
}
