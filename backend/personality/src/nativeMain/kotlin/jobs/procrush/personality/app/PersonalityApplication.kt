package jobs.procrush.personality.app

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.personality.bootstrap.PlatformLifecycle
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
        embeddedServer(CIO, port = config.workerHealthPort, host = "::") {
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

    try {
        server.start(wait = false)
        // Install after engine start so runtime/CIO hooks do not replace ours.
        PlatformLifecycle.onShutdown {
            server.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000)
        }
        PlatformLifecycle.awaitShutdown()
    } finally {
        dlqPoller.stop()
        context.close()
    }
}
