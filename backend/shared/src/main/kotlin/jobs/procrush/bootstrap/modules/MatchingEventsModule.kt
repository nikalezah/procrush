package jobs.procrush.bootstrap.modules

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.kafka.KafkaModule
import jobs.procrush.matching.kafka.MatchingEventPayloadFactory
import jobs.procrush.matching.kafka.MatchingEventPublisher

data class MatchingEventsModule(
    val kafkaModule: KafkaModule,
    val publisher: MatchingEventPublisher,
    val payloadFactory: MatchingEventPayloadFactory,
) {
    fun close() {
        publisher.flush()
        kafkaModule.close()
    }

    companion object {
        fun create(
            config: AppConfig,
            auth: AuthModule,
            matchingRepository: jobs.procrush.matching.repository.MatchingRepository,
        ): MatchingEventsModule {
            val kafka = KafkaModule.create(config.kafka)
            val publisher = MatchingEventPublisher(kafka.producer, config.kafka)
            val payloadFactory =
                MatchingEventPayloadFactory(
                    seekerRepository = auth.seekerRepository,
                    employerRepository = auth.employerRepository,
                    matchingRepository = matchingRepository,
                    referenceRepository = auth.referenceRepository,
                )
            return MatchingEventsModule(
                kafkaModule = kafka,
                publisher = publisher,
                payloadFactory = payloadFactory,
            )
        }
    }
}
