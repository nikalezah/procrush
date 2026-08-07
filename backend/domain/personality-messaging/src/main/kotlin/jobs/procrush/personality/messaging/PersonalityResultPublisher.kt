package jobs.procrush.personality.messaging

import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.MessagePublisher
import jobs.procrush.bootstrap.rabbitmq.OutboundMessage
import jobs.procrush.shared.CorrelationIds
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PersonalityResultPublisher(
    private val publisher: MessagePublisher,
    private val config: RabbitMqConfig,
    private val log: MessagingLog,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    @OptIn(ExperimentalUuidApi::class)
    fun publish(
        result: PersonalityGenerationResult,
        correlationId: String? = result.correlationId,
        log: MessagingLog = this.log,
    ) {
        val messageId = Uuid.random().toString()
        val resolvedCorrelationId = correlationId ?: messageId
        val body = json.encodeToString(result)
        publisher.publish(
            OutboundMessage(
                exchange = config.exchange,
                routingKey = config.resultsRoutingKey,
                body = body.toByteArray(Charsets.UTF_8),
                messageId = messageId,
                headers = mapOf(CorrelationIds.HEADER_REQUEST_ID to resolvedCorrelationId),
            ),
        )
        log.info(
            "Published personality generation result seekerId={} status={} attempt={} messageId={} correlationId={}",
            result.seekerId,
            result.status,
            result.attempt,
            messageId,
            resolvedCorrelationId,
        )
    }
}
