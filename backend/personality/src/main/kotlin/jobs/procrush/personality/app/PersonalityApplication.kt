package jobs.procrush.personality.app

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.personality.bootstrap.WorkerContext
import jobs.procrush.personality.observability.DlqDepthPoller
import jobs.procrush.personality.observability.WorkerObservability
import jobs.procrush.personality.observability.configureHealthRoutes
import jobs.procrush.personality.observability.simpleCheck

fun main() {
    val config = WorkerAppConfig.fromEnvironment()
    val observability = WorkerObservability.initialize("personality")
    val context = WorkerContext.create(config)
    val dlqPoller =
        DlqDepthPoller(
            rabbitMqUrl = config.rabbitMq.url,
            queueName = config.rabbitMq.deadLetterQueue,
        ).also { it.start() }
    val server =
        embeddedServer(Netty, port = config.workerHealthPort, host = "::") {
            configureHealthRoutes(
                config = observability,
                readinessChecks =
                    listOf(
                        simpleCheck("rabbitmq") {
                            runCatching { context.rabbitMqModule.isConnected() }.getOrDefault(false)
                        },
                        simpleCheck("consumer") {
                            context.personalityCommandConsumer.isRunning()
                        },
                    ),
            )
        }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            dlqPoller.stop()
            context.close()
            server.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000)
        },
    )

    server.start(wait = true)
}
