package jobs.procrush.matching.kafka

import jobs.procrush.bootstrap.config.KafkaConfig
import jobs.procrush.bootstrap.kafka.KafkaModule
import jobs.procrush.bootstrap.kafka.KafkaStringPublisher
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.seeker.repository.SeekerRepository

data class MatchingEventsRuntime(
    private val kafkaModule: KafkaModule,
    private val publisher: MatchingEventPublisher,
    val eventPort: MatchingEventPort,
) {
    fun close() {
        publisher.flush()
        kafkaModule.close()
    }

    companion object {
        fun create(
            kafka: KafkaConfig,
            seekerRepository: SeekerRepository,
            matchingRepository: MatchingRepository,
        ): MatchingEventsRuntime {
            val kafkaModule = KafkaModule.create(kafka)
            val publisher =
                MatchingEventPublisher(
                    KafkaStringPublisher(kafkaModule.producer, kafka.matchingEventsTopic),
                )
            val payloadFactory =
                MatchingEventPayloadFactory(
                    seekerRepository = seekerRepository,
                    matchingRepository = matchingRepository,
                )
            val eventPort = MatchingEventPortAdapter(publisher, payloadFactory)
            return MatchingEventsRuntime(
                kafkaModule = kafkaModule,
                publisher = publisher,
                eventPort = eventPort,
            )
        }
    }
}
