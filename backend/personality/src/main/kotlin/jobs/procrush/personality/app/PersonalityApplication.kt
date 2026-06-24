package jobs.procrush.personality.app

import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.personality.bootstrap.WorkerContext

fun main() {
    val config = WorkerAppConfig.fromEnvironment()
    val context = WorkerContext.create(config)
    val server =
        embeddedServer(Netty, port = config.workerHealthPort, host = "::") {
            routing {
                get("/health") {
                    val redisStatus =
                        runCatching { context.redisModule.client.ping() }
                            .map { if (it.equals("PONG", ignoreCase = true)) "ok" else "down" }
                            .getOrElse { "down" }
                    val rabbitStatus =
                        runCatching { if (context.rabbitMqModule.isConnected()) "ok" else "down" }
                            .getOrElse { "down" }
                    val healthy = redisStatus == "ok" && rabbitStatus == "ok"
                    if (healthy) {
                        call.respond(mapOf("status" to "ok", "redis" to "ok", "rabbitmq" to "ok"))
                    } else {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            mapOf("status" to "degraded", "redis" to redisStatus, "rabbitmq" to rabbitStatus),
                        )
                    }
                }
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
