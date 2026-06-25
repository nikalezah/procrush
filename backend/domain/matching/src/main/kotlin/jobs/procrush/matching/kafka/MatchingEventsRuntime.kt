package jobs.procrush.matching.kafka

import jobs.procrush.bootstrap.config.KafkaConfig
import jobs.procrush.bootstrap.kafka.KafkaModule
import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.matching.port.MatchingEventPort
import jobs.procrush.matching.repository.MatchingRepository
import jobs.procrush.seeker.repository.SeekerRepository
import jobs.procrush.shared.repository.ReferenceRepository

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
            employerRepository: EmployerRepository,
            matchingRepository: MatchingRepository,
            referenceRepository: ReferenceRepository,
        ): MatchingEventsRuntime {
            val kafkaModule = KafkaModule.create(kafka)
            val publisher = MatchingEventPublisher(kafkaModule.producer, kafka)
            val payloadFactory =
                MatchingEventPayloadFactory(
                    seekerRepository = seekerRepository,
                    employerRepository = employerRepository,
                    matchingRepository = matchingRepository,
                    referenceRepository = referenceRepository,
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
