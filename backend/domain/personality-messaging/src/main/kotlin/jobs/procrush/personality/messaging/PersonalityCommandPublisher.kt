package jobs.procrush.personality.messaging

import com.rabbitmq.client.Channel
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqTopology
import jobs.procrush.shared.dto.SuperpowerAndTalentDto
import jobs.procrush.survey.dto.SurveyLlmContextDto
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

class PersonalityCommandPublisher(
    private val channel: Channel,
    private val config: RabbitMqConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(PersonalityCommandPublisher::class.java)

    fun enqueue(
        seekerId: Long,
        userId: UUID,
        surveyContext: SurveyLlmContextDto,
        catalog: List<SuperpowerAndTalentDto>,
        attempt: Int = 1,
        correlationId: String? = null,
    ) {
        val messageId = UUID.randomUUID().toString()
        val resolvedCorrelationId = correlationId ?: messageId
        val command =
            PersonalityGenerationCommand(
                seekerId = seekerId,
                userId = userId.toString(),
                surveyContext = surveyContext,
                catalog = catalog,
                enqueuedAt = OffsetDateTime.now().toString(),
                attempt = attempt,
                correlationId = resolvedCorrelationId,
            )
        publish(command, messageId, resolvedCorrelationId)
    }

    fun enqueue(
        command: PersonalityGenerationCommand,
        correlationId: String? = command.correlationId,
    ) {
        val messageId = UUID.randomUUID().toString()
        val resolvedCorrelationId = correlationId ?: messageId
        publish(command, messageId, resolvedCorrelationId)
    }

    private fun publish(
        command: PersonalityGenerationCommand,
        messageId: String,
        correlationId: String,
    ) {
        val body = json.encodeToString(command)
        channel.basicPublish(
            config.exchange,
            config.routingKey,
            RabbitMqTopology.persistentJsonProperties(
                messageId = messageId,
                correlationId = correlationId,
                traceHeaders = emptyMap(),
            ),
            body.toByteArray(Charsets.UTF_8),
        )
        logger.info(
            "Enqueued personality generation command seekerId={} userId={} attempt={} messageId={} correlationId={}",
            command.seekerId,
            command.userId,
            command.attempt,
            messageId,
            correlationId,
        )
    }
}
