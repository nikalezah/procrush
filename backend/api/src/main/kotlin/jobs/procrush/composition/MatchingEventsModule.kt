package jobs.procrush.composition

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.config.KafkaConfig
import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.bootstrap.kafka.KafkaModule
import jobs.procrush.matching.kafka.MatchingEventPayloadFactory
import jobs.procrush.matching.kafka.MatchingEventPublisher
import jobs.procrush.matching.port.MatchingEventPort

data class MatchingEventsModule(
    val kafkaModule: KafkaModule,
    val publisher: MatchingEventPublisher,
    val payloadFactory: MatchingEventPayloadFactory,
    val eventPort: MatchingEventPort,
) {
    fun close() {
        publisher.flush()
        kafkaModule.close()
    }

    companion object {
        fun create(
            kafka: KafkaConfig,
            auth: AuthModule,
            matchingRepository: jobs.procrush.matching.repository.MatchingRepository,
        ): MatchingEventsModule {
            val kafkaModule = KafkaModule.create(kafka)
            val publisher = MatchingEventPublisher(kafkaModule.producer, kafka)
            val payloadFactory =
                MatchingEventPayloadFactory(
                    seekerRepository = auth.seekerRepository,
                    employerRepository = auth.employerRepository,
                    matchingRepository = matchingRepository,
                    referenceRepository = auth.referenceRepository,
                )
            val eventPort = MatchingEventPortAdapter(publisher, payloadFactory)
            return MatchingEventsModule(
                kafkaModule = kafkaModule,
                publisher = publisher,
                payloadFactory = payloadFactory,
                eventPort = eventPort,
            )
        }

        fun create(config: AppConfig, auth: AuthModule, matchingRepository: jobs.procrush.matching.repository.MatchingRepository) =
            create(config.kafka, auth, matchingRepository)

        fun create(config: WorkerAppConfig, auth: AuthModule, matchingRepository: jobs.procrush.matching.repository.MatchingRepository) =
            create(config.kafka, auth, matchingRepository)
    }
}
