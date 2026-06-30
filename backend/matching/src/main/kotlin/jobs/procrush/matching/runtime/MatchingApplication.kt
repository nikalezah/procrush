package jobs.procrush.matching.runtime

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.matching.runtime.bootstrap.MatchingServiceContext
import jobs.procrush.matching.runtime.route.matchingReadRoutes
import jobs.procrush.observability.HealthCheck
import jobs.procrush.observability.KafkaHealth
import jobs.procrush.observability.ObservabilityHolder
import jobs.procrush.observability.OpenTelemetryFactory
import jobs.procrush.observability.configureHealthRoutes
import jobs.procrush.observability.configureObservabilityPlugins
import jobs.procrush.observability.simpleCheck
import jobs.procrush.shared.toResponseBody
import org.slf4j.LoggerFactory

private val matchingStatusLogger = LoggerFactory.getLogger("jobs.procrush.matching.StatusPages")

fun main() {
    val observability = ObservabilityHolder.initialize("matching")
    val context = MatchingServiceContext.create()

    val server =
        embeddedServer(Netty, port = context.config.port, host = "::") {
            configureObservabilityPlugins(observability)
            install(ContentNegotiation) {
                json()
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    matchingStatusLogger.error("Unhandled matching error", cause)
                    call.respond(
                        io.ktor.http.HttpStatusCode.InternalServerError,
                        ErrorCode.UNKNOWN_ERROR.toResponseBody(),
                    )
                }
            }
            configureHealthRoutes(
                config = observability.config,
                readinessChecks =
                    listOf(
                        simpleCheck("redis") {
                            runCatching { context.redisModule.client.ping() }
                                .getOrNull()
                                ?.equals("PONG", ignoreCase = true) == true
                        },
                        HealthCheck {
                            KafkaHealth.check(context.config.kafka.bootstrapServers)
                        },
                        simpleCheck("kafka_consumer") {
                            context.eventConsumer.isRunning()
                        },
                        simpleCheck("postgres") {
                            runCatching {
                                context.matchResultsRepository.listForSeeker(-1)
                                true
                            }.getOrDefault(false)
                        },
                    ),
            )
            routing {
                matchingReadRoutes(context.matchResultsRepository, context.projectionRepository)
            }
        }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            context.close()
            OpenTelemetryFactory.shutdown()
            server.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000)
        },
    )

    server.start(wait = true)
}
