package jobs.procrush.personality.app

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.observability.DlqDepthPoller
import jobs.procrush.observability.ObservabilityHolder
import jobs.procrush.observability.OpenTelemetryFactory
import jobs.procrush.observability.configureHealthRoutes
import jobs.procrush.observability.configureObservabilityPlugins
import jobs.procrush.observability.simpleCheck
import jobs.procrush.personality.bootstrap.WorkerContext

fun main() {
    val config = WorkerAppConfig.fromEnvironment()
    val observability = ObservabilityHolder.initialize("personality")
    val context = WorkerContext.create(config)
    val dlqPoller =
        DlqDepthPoller(
            rabbitMqUrl = config.rabbitMq.url,
            queueName = config.rabbitMq.deadLetterQueue,
        ).also { it.start() }
    val server =
        embeddedServer(Netty, port = config.workerHealthPort, host = "::") {
            configureObservabilityPlugins(observability)
            configureHealthRoutes(
                config = observability.config,
                readinessChecks =
                    listOf(
                        simpleCheck("redis") {
                            runCatching { context.redisModule.client.ping() }
                                .getOrNull()
                                ?.equals("PONG", ignoreCase = true) == true
                        },
                        simpleCheck("rabbitmq") {
                            runCatching { context.rabbitMqModule.isConnected() }.getOrDefault(false)
                        },
                        simpleCheck("consumer") {
                            context.personalityJobConsumer.isRunning()
                        },
                    ),
            )
            routing { }
        }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            dlqPoller.stop()
            context.close()
            OpenTelemetryFactory.shutdown()
            server.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000)
        },
    )

    server.start(wait = true)
}
