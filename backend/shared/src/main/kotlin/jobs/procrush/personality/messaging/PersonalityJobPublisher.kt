package jobs.procrush.personality.messaging

import com.rabbitmq.client.Channel
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqTopology
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
    ) {
        val messageId = UUID.randomUUID().toString()
        val job =
            PersonalityGenerationJob(
                seekerId = seekerId,
                userId = userId.toString(),
                enqueuedAt = OffsetDateTime.now().toString(),
                attempt = attempt,
            )
        val body = json.encodeToString(job)
        channel.basicPublish(
            config.exchange,
            config.routingKey,
            RabbitMqTopology.persistentJsonProperties(messageId),
            body.toByteArray(Charsets.UTF_8),
        )
        logger.info(
            "Enqueued personality generation seekerId={} userId={} attempt={} messageId={}",
            seekerId,
            userId,
            attempt,
            messageId,
        )
    }
}
