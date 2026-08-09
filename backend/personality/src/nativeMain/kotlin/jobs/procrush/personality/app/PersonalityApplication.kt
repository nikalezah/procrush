package jobs.procrush.personality.app

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.personality.amqp.PersonalityAmqpModule
import jobs.procrush.personality.bootstrap.PersonalityWorkerRuntime
import jobs.procrush.personality.bootstrap.PlatformLifecycle
import jobs.procrush.personality.bootstrap.personalityKoinModules
import jobs.procrush.personality.messaging.PersonalityCommandConsumer
import jobs.procrush.personality.observability.DlqDepthPoller
import jobs.procrush.personality.observability.WorkerObservability
import jobs.procrush.personality.observability.configureHealthRoutes
import jobs.procrush.personality.observability.simpleCheck
import org.koin.dsl.koinApplication

fun main() {
    val config = WorkerAppConfig.fromEnvironment()
    val observability = WorkerObservability.initialize("personality")
    val koin = koinApplication { modules(personalityKoinModules(config)) }
    val runtime = koin.koin.get<PersonalityWorkerRuntime>()
    runtime.start()
    val rabbitMqModule = koin.koin.get<PersonalityAmqpModule>()
    val personalityCommandConsumer = koin.koin.get<PersonalityCommandConsumer>()
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
                            runCatching { rabbitMqModule.isConnected() }.getOrDefault(false)
                        },
                        simpleCheck("consumer") {
                            personalityCommandConsumer.isRunning()
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
        runtime.close()
        koin.close()
    }
}
