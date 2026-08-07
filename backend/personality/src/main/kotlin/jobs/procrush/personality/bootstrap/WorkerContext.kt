package jobs.procrush.personality.bootstrap

import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.personality.amqp.PersonalityAmqpModule
import jobs.procrush.personality.messaging.PersonalityCommandConsumer

data class WorkerContext(
    val config: WorkerAppConfig,
    val rabbitMqModule: PersonalityAmqpModule,
    val personalityCommandConsumer: PersonalityCommandConsumer,
    private val workerModule: PersonalityWorkerModule,
) {
    fun close() {
        workerModule.stop()
        rabbitMqModule.close()
    }

    companion object {
        fun create(config: WorkerAppConfig): WorkerContext {
            val rabbitMq = PersonalityAmqpModule.create(config.rabbitMq)
            val workerModule =
                PersonalityWorkerModule.create(
                    config = config,
                    rabbitMq = rabbitMq,
                )
            workerModule.start()
            return WorkerContext(
                config = config,
                rabbitMqModule = rabbitMq,
                personalityCommandConsumer = workerModule.consumer,
                workerModule = workerModule,
            )
        }
    }
}
