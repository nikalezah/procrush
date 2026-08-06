package jobs.procrush.personality.messaging

import com.rabbitmq.client.Channel
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqTopology
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class PersonalityResultPublisher(
    private val channel: Channel,
    private val config: RabbitMqConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(PersonalityResultPublisher::class.java)

    fun publish(
        result: PersonalityGenerationResult,
        correlationId: String? = result.correlationId,
    ) {
        val messageId = UUID.randomUUID().toString()
        val resolvedCorrelationId = correlationId ?: messageId
        val body = json.encodeToString(result)
        channel.basicPublish(
            config.exchange,
            config.resultsRoutingKey,
            RabbitMqTopology.persistentJsonProperties(
                messageId = messageId,
                correlationId = resolvedCorrelationId,
                traceHeaders = emptyMap(),
            ),
            body.toByteArray(Charsets.UTF_8),
        )
        logger.info(
            "Published personality generation result seekerId={} status={} attempt={} messageId={} correlationId={}",
            result.seekerId,
            result.status,
            result.attempt,
            messageId,
            resolvedCorrelationId,
        )
    }
}
