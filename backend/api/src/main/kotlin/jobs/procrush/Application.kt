package jobs.procrush

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import jobs.procrush.api.handler.ApiHandlers
import jobs.procrush.api.rabbitmq.RabbitMqModule
import jobs.procrush.api.route.generatedApiRoutes
import jobs.procrush.api.route.sseRoutes
import jobs.procrush.auth.RoleGuard
import jobs.procrush.bootstrap.DatabaseFactory
import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.plugins.configureCallLogging
import jobs.procrush.bootstrap.plugins.configureCors
import jobs.procrush.bootstrap.plugins.configureSerialization
import jobs.procrush.bootstrap.plugins.configureStatusPages
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.composition.ApiRuntime
import jobs.procrush.composition.apiKoinModules
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.matching.service.RecommendationsEventService
import jobs.procrush.observability.DlqDepthPoller
import jobs.procrush.observability.HealthCheck
import jobs.procrush.observability.KafkaHealth
import jobs.procrush.observability.OpenTelemetryFactory
import jobs.procrush.observability.bootstrapObservability
import jobs.procrush.observability.configureHealthRoutes
import jobs.procrush.observability.simpleCheck
import jobs.procrush.personality.service.PersonalityProfileService
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "::", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val config = AppConfig.fromEnvironment()
    val observability = bootstrapObservability("api")
    DatabaseFactory.init(config)
    install(Koin) {
        modules(apiKoinModules(config))
    }
    val runtime = get<ApiRuntime>()
    runtime.start()
    val redisModule = get<RedisModule>()
    val rabbitMqModule = get<RabbitMqModule>()
    val handlers = get<ApiHandlers>()
    val roleGuard = get<RoleGuard>()
    val matchInterestService = get<MatchInterestService>()
    val recommendationsEventService = get<RecommendationsEventService>()
    val personalityProfileService = get<PersonalityProfileService>()
    val dlqPoller =
        DlqDepthPoller(
            rabbitMqUrl = config.rabbitMq.url,
            queueName = config.rabbitMq.deadLetterQueue,
        ).also { it.start() }
    monitor.subscribe(ApplicationStopped) {
        dlqPoller.stop()
        runtime.close()
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
                    runCatching { redisModule.client.ping() }
                        .getOrNull()
                        ?.equals("PONG", ignoreCase = true) == true
                },
                simpleCheck("rabbitmq") {
                    runCatching { rabbitMqModule.isConnected() }.getOrDefault(false)
                },
                HealthCheck {
                    KafkaHealth.check(config.kafka.bootstrapServers)
                },
            ),
    )

    routing {
        get("/") {
            call.respondText("ProCrush API")
        }
        generatedApiRoutes(handlers)
        sseRoutes(
            roleGuard,
            matchInterestService,
            recommendationsEventService,
            personalityProfileService,
        )
    }
}
