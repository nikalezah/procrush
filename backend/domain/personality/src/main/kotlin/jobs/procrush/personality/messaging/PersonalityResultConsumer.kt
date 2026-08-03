package jobs.procrush.personality.messaging

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.observability.CorrelationIds
import jobs.procrush.observability.MdcContext
import jobs.procrush.observability.ObservabilityHolder
import jobs.procrush.observability.TracePropagation
import jobs.procrush.personality.service.PersonalityResultApplyService
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class PersonalityResultConsumer(
    private val rabbitMq: RabbitMqModule,
    private val applyService: PersonalityResultApplyService,
    private val dedup: PersonalityResultDedup,
    private val rabbitMqConfig: RabbitMqConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(PersonalityResultConsumer::class.java)
    private var consumerTag: String? = null
    private var consumerChannel: com.rabbitmq.client.Channel? = null

    fun start() {
        if (consumerTag != null) return
        val channel = rabbitMq.createConsumerChannel()
        consumerChannel = channel
        consumerTag =
            channel.basicConsume(
                rabbitMqConfig.resultsQueue,
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
        logger.info("Personality result consumer started on queue {}", rabbitMqConfig.resultsQueue)
    }

    fun stop() {
        val channel = consumerChannel ?: return
        consumerTag?.let { channel.basicCancel(it) }
        runCatching { channel.close() }
        consumerTag = null
        consumerChannel = null
        logger.info("Personality result consumer stopped")
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
        val correlationId = TracePropagation.requestIdFromMap(headers) ?: messageId
        MdcContext.runWith(
            mapOf(
                CorrelationIds.REQUEST_ID to correlationId,
                CorrelationIds.MESSAGE_ID to messageId,
            ),
        ) {
            ObservabilityHolder.tracing.withPropagatedHeaders(headers, "personality.result.apply") {
                processDeliveryInternal(channel, deliveryTag, body, messageId, correlationId)
            }
        }
    }

    private fun processDeliveryInternal(
        channel: com.rabbitmq.client.Channel,
        deliveryTag: Long,
        body: ByteArray,
        messageId: String,
        correlationId: String,
    ) {
        val result =
            runCatching {
                json.decodeFromString(PersonalityGenerationResult.serializer(), String(body, Charsets.UTF_8))
            }.getOrElse { error ->
                logger.error("Invalid personality result payload messageId={}", messageId, error)
                channel.basicAck(deliveryTag, false)
                return
            }

        MdcContext.put(CorrelationIds.SEEKER_ID, result.seekerId.toString())
        MdcContext.put(CorrelationIds.USER_ID, result.userId)
        MdcContext.put(CorrelationIds.REQUEST_ID, result.correlationId ?: correlationId)

        if (!dedup.tryMarkProcessing(messageId)) {
            logger.info("Duplicate personality result messageId={}, acking", messageId)
            channel.basicAck(deliveryTag, false)
            return
        }

        try {
            applyService.apply(result)
            channel.basicAck(deliveryTag, false)
        } catch (error: Exception) {
            logger.error(
                "Failed to apply personality result seekerId={} messageId={}",
                result.seekerId,
                messageId,
                error,
            )
            channel.basicNack(deliveryTag, false, false)
        } finally {
            dedup.release(messageId)
        }
    }
}
