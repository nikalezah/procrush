package jobs.procrush.personality.messaging

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.personality.observability.Correlation
import jobs.procrush.personality.observability.Logger
import jobs.procrush.personality.observability.Metrics
import jobs.procrush.personality.service.PersonalityGenerationHandler
import jobs.procrush.shared.CorrelationIds
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID

class PersonalityCommandConsumer(
    private val rabbitMq: RabbitMqModule,
    private val handler: PersonalityGenerationHandler,
    private val commandPublisher: PersonalityCommandPublisher,
    private val resultPublisher: PersonalityResultPublisher,
    private val rabbitMqConfig: RabbitMqConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = Logger.get(PersonalityCommandConsumer::class.java)
    private var consumerTag: String? = null
    private var consumerChannel: com.rabbitmq.client.Channel? = null

    fun start() {
        if (consumerTag != null) return
        val channel = rabbitMq.createConsumerChannel()
        consumerChannel = channel
        consumerTag =
            channel.basicConsume(
                rabbitMqConfig.queue,
                false,
                object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String,
                        envelope: Envelope,
                        properties: AMQP.BasicProperties,
                        body: ByteArray,
                    ) {
                        processDelivery(channel, envelope.deliveryTag, properties, body)
                    }
                },
            )
        Metrics.setPersonalityConsumerRunning(true)
        logger.info("Personality command consumer started on queue {}", rabbitMqConfig.queue)
    }

    fun stop() {
        val channel = consumerChannel ?: return
        consumerTag?.let { channel.basicCancel(it) }
        runCatching { channel.close() }
        consumerTag = null
        consumerChannel = null
        Metrics.setPersonalityConsumerRunning(false)
        logger.info("Personality command consumer stopped")
    }

    fun isRunning(): Boolean = consumerTag != null

    private fun processDelivery(
        channel: com.rabbitmq.client.Channel,
        deliveryTag: Long,
        properties: AMQP.BasicProperties,
        body: ByteArray,
    ) {
        val messageId = properties.messageId ?: UUID.randomUUID().toString()
        val headers = properties.headers.orEmpty()
        val correlationId = Correlation.requestIdFromHeaders(headers) ?: messageId
        Correlation.runWith(
            mapOf(
                CorrelationIds.REQUEST_ID to correlationId,
                CorrelationIds.MESSAGE_ID to messageId,
            ),
        ) {
            processDeliveryInternal(channel, deliveryTag, body, messageId, correlationId)
        }
    }

    private fun processDeliveryInternal(
        channel: com.rabbitmq.client.Channel,
        deliveryTag: Long,
        body: ByteArray,
        messageId: String,
        correlationId: String,
    ) {
        val command =
            runCatching {
                json.decodeFromString(PersonalityGenerationCommand.serializer(), String(body, Charsets.UTF_8))
            }.getOrElse { error ->
                logger.error("Invalid personality command payload messageId={}", messageId, error)
                channel.basicAck(deliveryTag, false)
                return
            }

        Correlation.put(CorrelationIds.SEEKER_ID, command.seekerId.toString())
        Correlation.put(CorrelationIds.USER_ID, command.userId)
        Correlation.put(CorrelationIds.REQUEST_ID, command.correlationId ?: correlationId)

        try {
            val result =
                runBlocking {
                    handler.generate(command)
                }.copy(commandMessageId = messageId)
            resultPublisher.publish(result, correlationId = command.correlationId ?: correlationId)
            channel.basicAck(deliveryTag, false)
            Metrics.personalityJobProcessed("success")
        } catch (error: Exception) {
            logger.error(
                "Personality profile generation failed seekerId={} attempt={}",
                command.seekerId,
                command.attempt,
                error,
            )
            if (isTransient(error) && command.attempt < rabbitMqConfig.maxRetries) {
                commandPublisher.enqueue(
                    command.copy(attempt = command.attempt + 1),
                    correlationId = command.correlationId ?: correlationId,
                )
                channel.basicAck(deliveryTag, false)
                Metrics.personalityJobProcessed("retry")
            } else {
                resultPublisher.publish(
                    handler.failureResult(command, error, messageId),
                    correlationId = command.correlationId ?: correlationId,
                )
                channel.basicNack(deliveryTag, false, false)
                Metrics.personalityJobDlq()
                Metrics.personalityJobProcessed("dlq")
            }
        }
    }

    private fun isTransient(error: Throwable): Boolean =
        error is IOException ||
            error is io.ktor.client.plugins.HttpRequestTimeoutException ||
            error.cause?.let { isTransient(it) } == true
}
