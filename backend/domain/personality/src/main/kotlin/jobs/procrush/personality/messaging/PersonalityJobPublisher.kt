package jobs.procrush.personality.messaging

import com.rabbitmq.client.Channel
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqTopology
import jobs.procrush.observability.MdcContext
import jobs.procrush.observability.TracePropagation
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

class PersonalityJobPublisher(
    private val channel: Channel,
    private val config: RabbitMqConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(PersonalityJobPublisher::class.java)

    fun enqueue(
        seekerId: Long,
        userId: UUID,
        attempt: Int = 1,
        correlationId: String? = MdcContext.currentRequestId(),
    ) {
        val messageId = UUID.randomUUID().toString()
        val resolvedCorrelationId = correlationId ?: messageId
        val traceHeaders = mutableMapOf<String, String>()
        TracePropagation.injectIntoMap(traceHeaders)
        val job =
            PersonalityGenerationJob(
                seekerId = seekerId,
                userId = userId.toString(),
                enqueuedAt = OffsetDateTime.now().toString(),
                attempt = attempt,
                correlationId = resolvedCorrelationId,
            )
        val body = json.encodeToString(job)
        channel.basicPublish(
            config.exchange,
            config.routingKey,
            RabbitMqTopology.persistentJsonProperties(
                messageId = messageId,
                correlationId = resolvedCorrelationId,
                traceHeaders = traceHeaders,
            ),
            body.toByteArray(Charsets.UTF_8),
        )
        logger.info(
            "Enqueued personality generation seekerId={} userId={} attempt={} messageId={} correlationId={}",
            seekerId,
            userId,
            attempt,
            messageId,
            resolvedCorrelationId,
        )
    }
}
