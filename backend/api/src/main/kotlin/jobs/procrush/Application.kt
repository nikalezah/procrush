package jobs.procrush

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
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
import jobs.procrush.observability.DlqDepthPoller
import jobs.procrush.observability.HealthCheck
import jobs.procrush.observability.KafkaHealth
import jobs.procrush.observability.OpenTelemetryFactory
import jobs.procrush.observability.bootstrapObservability
import jobs.procrush.observability.configureHealthRoutes
import jobs.procrush.observability.simpleCheck

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "::", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val config = AppConfig.fromEnvironment()
    val observability = bootstrapObservability("api")
    DatabaseFactory.init(config)
    val app = AppContext.create(config)
    val dlqPoller =
        DlqDepthPoller(
            rabbitMqUrl = config.rabbitMq.url,
            queueName = config.rabbitMq.deadLetterQueue,
        ).also { it.start() }
    monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        dlqPoller.stop()
        app.close()
        OpenTelemetryFactory.shutdown()
    }

    configureSerialization()
    configureStatusPages()
    configureCallLogging()
    configureCors(config)
    install(SSE)

    configureHealthRoutes(
        config = observability.config,
        readinessChecks =
            listOf(
                simpleCheck("redis") {
                    runCatching { app.redisModule.client.ping() }
                        .getOrNull()
                        ?.equals("PONG", ignoreCase = true) == true
                },
                simpleCheck("rabbitmq") {
                    runCatching { app.rabbitMqModule.isConnected() }.getOrDefault(false)
                },
                HealthCheck {
                    KafkaHealth.check(app.config.kafka.bootstrapServers)
                },
                simpleCheck("matching") {
                    runCatching { app.checkMatchingServiceHealthBlocking() }.getOrDefault(false)
                },
            ),
    )

    routing {
        get("/") {
            call.respondText("ProCrush API")
        }
        generatedApiRoutes(app.handlers)
        sseRoutes(app.roleGuard, app.matchInterestService, app.personalityProfileService)
    }
}
