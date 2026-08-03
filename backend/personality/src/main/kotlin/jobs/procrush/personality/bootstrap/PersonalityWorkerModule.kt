package jobs.procrush.personality.bootstrap

import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.llm.LlmFactory
import jobs.procrush.personality.llm.PersonalityProfileValidator
import jobs.procrush.personality.llm.PersonalityPromptBuilder
import jobs.procrush.personality.messaging.PersonalityCommandConsumer
import jobs.procrush.personality.messaging.PersonalityCommandPublisher
import jobs.procrush.personality.messaging.PersonalityResultPublisher
import jobs.procrush.personality.service.PersonalityGenerationHandler

data class PersonalityWorkerModule(
    val consumer: PersonalityCommandConsumer,
) {
    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

    companion object {
        fun create(
            config: WorkerAppConfig,
            rabbitMq: RabbitMqModule,
        ): PersonalityWorkerModule {
            val handler =
                PersonalityGenerationHandler(
                    llmConfig = config.llm,
                    llmClient = LlmFactory.createClient(config.llm),
                    promptBuilder = PersonalityPromptBuilder(),
                    validator = PersonalityProfileValidator(),
                )
            val commandPublisher = PersonalityCommandPublisher(rabbitMq.publishChannel, rabbitMq.config)
            val resultPublisher = PersonalityResultPublisher(rabbitMq.publishChannel, rabbitMq.config)
            val consumer =
                PersonalityCommandConsumer(
                    rabbitMq = rabbitMq,
                    handler = handler,
                    commandPublisher = commandPublisher,
                    resultPublisher = resultPublisher,
                    rabbitMqConfig = rabbitMq.config,
                )
            return PersonalityWorkerModule(consumer = consumer)
        }
    }
}
