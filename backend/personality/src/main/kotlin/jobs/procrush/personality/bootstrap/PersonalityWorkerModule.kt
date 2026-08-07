package jobs.procrush.personality.bootstrap

import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.llm.LlmFactory
import jobs.procrush.personality.amqp.PersonalityAmqpModule
import jobs.procrush.personality.llm.PersonalityProfileValidator
import jobs.procrush.personality.llm.PersonalityPromptBuilder
import jobs.procrush.personality.messaging.MessagingLog
import jobs.procrush.personality.messaging.PersonalityCommandConsumer
import jobs.procrush.personality.messaging.PersonalityCommandPublisher
import jobs.procrush.personality.messaging.PersonalityResultPublisher
import jobs.procrush.personality.observability.Logger
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
            rabbitMq: PersonalityAmqpModule,
        ): PersonalityWorkerModule {
            val handler =
                PersonalityGenerationHandler(
                    llmConfig = config.llm,
                    llmClient = LlmFactory.createClient(config.llm),
                    promptBuilder = PersonalityPromptBuilder(),
                    validator = PersonalityProfileValidator(),
                )
            val commandLogger = Logger.get(PersonalityCommandPublisher::class)
            val resultLogger = Logger.get(PersonalityResultPublisher::class)
            val commandPublisher =
                PersonalityCommandPublisher(
                    rabbitMq.publisher,
                    rabbitMq.config,
                    MessagingLog { message, args -> Logger.infoBlocking(commandLogger, message, *args) },
                )
            val resultPublisher =
                PersonalityResultPublisher(
                    rabbitMq.publisher,
                    rabbitMq.config,
                    MessagingLog { message, args -> Logger.infoBlocking(resultLogger, message, *args) },
                )
            val consumer =
                PersonalityCommandConsumer(
                    messageConsumer = rabbitMq.createConsumer(),
                    handler = handler,
                    commandPublisher = commandPublisher,
                    resultPublisher = resultPublisher,
                    rabbitMqConfig = rabbitMq.config,
                )
            return PersonalityWorkerModule(consumer = consumer)
        }
    }
}
