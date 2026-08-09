package jobs.procrush.matching.runtime

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import jobs.procrush.bootstrap.redis.RedisModule
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.matching.runtime.bootstrap.MatchingDatabaseRegistry
import jobs.procrush.matching.runtime.bootstrap.MatchingRuntime
import jobs.procrush.matching.runtime.bootstrap.MatchingServiceAppConfig
import jobs.procrush.matching.runtime.bootstrap.matchingKoinModules
import jobs.procrush.matching.runtime.messaging.MatchingEventConsumer
import jobs.procrush.matching.runtime.repository.MatchResultsRepository
import jobs.procrush.matching.runtime.route.matchingReadRoutes
import jobs.procrush.observability.HealthCheck
import jobs.procrush.observability.KafkaHealth
import jobs.procrush.observability.ObservabilityHolder
import jobs.procrush.observability.OpenTelemetryFactory
import jobs.procrush.observability.configureHealthRoutes
import jobs.procrush.observability.configureObservabilityPlugins
import jobs.procrush.observability.simpleCheck
import jobs.procrush.shared.toResponseBody
import org.koin.dsl.koinApplication
import org.slf4j.LoggerFactory

private val matchingStatusLogger = LoggerFactory.getLogger("jobs.procrush.matching.StatusPages")

fun main() {
    val observability = ObservabilityHolder.initialize("matching")
    val config = MatchingServiceAppConfig.fromEnvironment()
    MatchingDatabaseRegistry.init(matchingConfig = config.matchingDatabase)
    val koin = koinApplication { modules(matchingKoinModules(config)) }
    val runtime = koin.koin.get<MatchingRuntime>()
    runtime.start()
    val redisModule = koin.koin.get<RedisModule>()
    val eventConsumer = koin.koin.get<MatchingEventConsumer>()
    val matchResultsRepository = koin.koin.get<MatchResultsRepository>()

    val server =
        embeddedServer(Netty, port = config.port, host = "::") {
            configureObservabilityPlugins(observability)
            install(ContentNegotiation) {
                json()
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    matchingStatusLogger.error("Unhandled matching error", cause)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorCode.UNKNOWN_ERROR.toResponseBody(),
                    )
                }
            }
            configureHealthRoutes(
                config = observability.config,
                readinessChecks =
                    listOf(
                        simpleCheck("redis") {
                            runCatching { redisModule.client.ping() }
                                .getOrNull()
                                ?.equals("PONG", ignoreCase = true) == true
                        },
                        HealthCheck {
                            KafkaHealth.check(config.kafka.bootstrapServers)
                        },
                        simpleCheck("kafka_consumer") {
                            eventConsumer.isRunning()
                        },
                        simpleCheck("postgres") {
                            runCatching {
                                matchResultsRepository.listForSeeker(-1)
                                true
                            }.getOrDefault(false)
                        },
                    ),
            )
            routing {
                matchingReadRoutes()
            }
        }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            runtime.close()
            koin.close()
            OpenTelemetryFactory.shutdown()
            server.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000)
        },
    )

    server.start(wait = true)
}
