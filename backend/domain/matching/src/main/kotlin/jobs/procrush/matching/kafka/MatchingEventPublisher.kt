package jobs.procrush.matching.kafka

import jobs.procrush.bootstrap.kafka.KafkaStringPublisher
import jobs.procrush.matching.events.MatchingEventJson
import jobs.procrush.observability.AppMetrics
import jobs.procrush.observability.MdcContext
import jobs.procrush.observability.TracePropagation
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory

class MatchingEventPublisher(
    private val publisher: KafkaStringPublisher,
) {
    private val logger = LoggerFactory.getLogger(MatchingEventPublisher::class.java)

    fun publish(
        eventType: String,
        partitionKey: String,
        payload: JsonElement,
        correlationId: String? = MdcContext.currentRequestId(),
    ) {
        val body =
            MatchingEventJson.encodeEnvelope(
                eventType = eventType,
                payload = payload,
                correlationId = correlationId,
            )
        publisher.publish(
            key = partitionKey,
            body = body,
            configure = { TracePropagation.injectCurrent(it) },
        ) { metadata, error ->
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
                    metadata?.partition(),
                    metadata?.offset(),
                )
            }
        }
    }

    fun flush() {
        publisher.flush()
    }
}
