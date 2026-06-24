package jobs.procrush.matching.kafka

import jobs.procrush.bootstrap.config.KafkaConfig
import jobs.procrush.matching.events.MatchingEventEnvelope
import jobs.procrush.matching.events.MatchingEventJson
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

class MatchingEventPublisher(
    private val producer: KafkaProducer<String, String>,
    private val config: KafkaConfig,
) {
    private val logger = LoggerFactory.getLogger(MatchingEventPublisher::class.java)

    fun publish(
        eventType: String,
        partitionKey: String,
        payload: kotlinx.serialization.json.JsonElement,
    ) {
        val envelope =
            MatchingEventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = eventType,
                occurredAt = OffsetDateTime.now().toString(),
                payload = payload,
            )
        val body = MatchingEventJson.json.encodeToString(MatchingEventEnvelope.serializer(), envelope)
        val record =
            ProducerRecord(
                config.matchingEventsTopic,
                partitionKey,
                body,
            )
        producer.send(record) { metadata, error ->
            if (error != null) {
                logger.error(
                    "Failed to publish matching event type={} key={}",
                    eventType,
                    partitionKey,
                    error,
                )
            } else {
                logger.info(
                    "Published matching event type={} key={} partition={} offset={}",
                    eventType,
                    partitionKey,
                    metadata.partition(),
                    metadata.offset(),
                )
            }
        }
    }

    fun flush() {
        producer.flush()
    }
}
