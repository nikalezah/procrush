package jobs.procrush.matching.runtime

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import jobs.procrush.matching.runtime.bootstrap.MatchingServiceContext
import jobs.procrush.matching.runtime.route.matchingReadRoutes

fun main() {
    val context = MatchingServiceContext.create()

    val server =
        embeddedServer(Netty, port = context.config.port, host = "::") {
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/health") {
                    val redisStatus =
                        runCatching { context.redisModule.client.ping() }
                            .map { if (it.equals("PONG", ignoreCase = true)) "ok" else "down" }
                            .getOrElse { "down" }
                    val kafkaStatus = if (context.eventConsumer.isRunning()) "ok" else "down"
                    val postgresStatus =
                        runCatching {
                            context.matchResultsRepository.listForSeeker(-1)
                            "ok"
                        }.getOrElse { "down" }
                    val healthy = redisStatus == "ok" && kafkaStatus == "ok" && postgresStatus == "ok"
                    if (healthy) {
                        call.respond(
                            mapOf(
                                "status" to "ok",
                                "postgres" to "ok",
                                "kafka" to "ok",
                                "redis" to "ok",
                            ),
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            mapOf(
                                "status" to "degraded",
                                "postgres" to postgresStatus,
                                "kafka" to kafkaStatus,
                                "redis" to redisStatus,
                            ),
                        )
                    }
                }
                matchingReadRoutes(context.matchResultsRepository)
            }
        }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            context.close()
            server.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000)
        },
    )

    server.start(wait = true)
}
