package jobs.procrush

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import jobs.procrush.api.route.generatedApiRoutes
import jobs.procrush.api.route.sseRoutes
import jobs.procrush.bootstrap.DatabaseFactory
import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.plugins.configureCallLogging
import jobs.procrush.bootstrap.plugins.configureCors
import jobs.procrush.bootstrap.plugins.configureSerialization
import jobs.procrush.bootstrap.plugins.configureStatusPages
import jobs.procrush.composition.AppContext
import jobs.procrush.composition.checkMatchingServiceHealthBlocking

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "::", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val config = AppConfig.fromEnvironment()
    DatabaseFactory.init(config)
    val app = AppContext.create(config)
    monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        app.close()
    }

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
            val redisStatus =
                runCatching { app.redisModule.client.ping() }
                    .map { if (it.equals("PONG", ignoreCase = true)) "ok" else "down" }
                    .getOrElse { "down" }
            val rabbitStatus =
                runCatching { if (app.rabbitMqModule.isConnected()) "ok" else "down" }
                    .getOrElse { "down" }
            val kafkaStatus =
                if (app.config.kafka.bootstrapServers.isNotBlank()) "ok" else "down"
            val matchingStatus =
                runCatching { if (app.checkMatchingServiceHealthBlocking()) "ok" else "down" }
                    .getOrElse { "down" }
            val healthy = redisStatus == "ok" && rabbitStatus == "ok" && kafkaStatus == "ok" && matchingStatus == "ok"
            if (healthy) {
                call.respond(
                    mapOf(
                        "status" to "ok",
                        "redis" to "ok",
                        "rabbitmq" to "ok",
                        "kafka" to "ok",
                        "matching" to "ok",
                    ),
                )
            } else {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf(
                        "status" to "degraded",
                        "redis" to redisStatus,
                        "rabbitmq" to rabbitStatus,
                        "kafka" to kafkaStatus,
                        "matching" to matchingStatus,
                    ),
                )
            }
        }
        generatedApiRoutes(app.handlers)
        sseRoutes(app.roleGuard, app.matchInterestService, app.personalityProfileService)
    }
}
