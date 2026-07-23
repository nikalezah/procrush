package jobs.procrush.matching.runtime.service

import jobs.procrush.bootstrap.config.KafkaConfig
import jobs.procrush.matching.events.MatchingEventEnvelope
import jobs.procrush.matching.events.MatchingEventJson
import jobs.procrush.observability.AppMetrics
import jobs.procrush.observability.MdcContext
import jobs.procrush.observability.TracePropagation
import kotlinx.serialization.json.JsonElement
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

class MatchResultsEventPublisher(
    private val producer: KafkaProducer<String, String>,
    private val config: KafkaConfig,
) {
    private val logger = LoggerFactory.getLogger(MatchResultsEventPublisher::class.java)

    fun publish(
        eventType: String,
        partitionKey: String,
        payload: JsonElement,
        correlationId: String? = MdcContext.currentRequestId(),
    ) {
        val envelope =
            MatchingEventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = eventType,
                occurredAt = OffsetDateTime.now().toString(),
                payload = payload,
                correlationId = correlationId,
            )
        val body = MatchingEventJson.json.encodeToString(MatchingEventEnvelope.serializer(), envelope)
        val record =
            ProducerRecord(
                config.matchingResultsTopic,
                partitionKey,
                body,
            )
        TracePropagation.injectCurrent(record)
        producer.send(record) { metadata, error ->
            if (error != null) {
                AppMetrics.kafkaPublishFailure()
                logger.error(
                    "Failed to publish matching event type={} key={} correlationId={}",
                    eventType,
                    partitionKey,
                    correlationId,
                    error,
                )
            } else {
                logger.info(
                    "Published matching event type={} key={} correlationId={} partition={} offset={}",
                    eventType,
                    partitionKey,
                    correlationId,
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
