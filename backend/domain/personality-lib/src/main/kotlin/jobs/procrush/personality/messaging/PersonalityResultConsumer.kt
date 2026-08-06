package jobs.procrush.personality.messaging

import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.DeliveryResult
import jobs.procrush.bootstrap.rabbitmq.InboundMessage
import jobs.procrush.bootstrap.rabbitmq.MessageConsumer
import jobs.procrush.shared.CorrelationIds
import jobs.procrush.observability.MdcContext
import jobs.procrush.observability.ObservabilityHolder
import jobs.procrush.observability.TracePropagation
import jobs.procrush.personality.service.PersonalityResultApplyService
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class PersonalityResultConsumer(
    private val messageConsumer: MessageConsumer,
    private val applyService: PersonalityResultApplyService,
    private val dedup: PersonalityResultDedup,
    private val rabbitMqConfig: RabbitMqConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(PersonalityResultConsumer::class.java)

    fun start() {
        if (messageConsumer.isRunning()) return
        messageConsumer.start(rabbitMqConfig.resultsQueue) { inbound -> processDelivery(inbound) }
        logger.info("Personality result consumer started on queue {}", rabbitMqConfig.resultsQueue)
    }

    fun stop() {
        if (!messageConsumer.isRunning()) return
        messageConsumer.stop()
        logger.info("Personality result consumer stopped")
    }

    fun isRunning(): Boolean = messageConsumer.isRunning()

    private fun processDelivery(inbound: InboundMessage): DeliveryResult {
        val messageId = inbound.messageId ?: UUID.randomUUID().toString()
        val headers = inbound.headers
        val correlationId = TracePropagation.requestIdFromMap(headers) ?: messageId
        return MdcContext.runWith(
            mapOf(
                CorrelationIds.REQUEST_ID to correlationId,
                CorrelationIds.MESSAGE_ID to messageId,
            ),
        ) {
            ObservabilityHolder.tracing.withPropagatedHeaders(headers, "personality.result.apply") {
                processDeliveryInternal(inbound.body, messageId, correlationId)
            }
        }
    }

    private fun processDeliveryInternal(
        body: ByteArray,
        messageId: String,
        correlationId: String,
    ): DeliveryResult {
        val result =
            runCatching {
                json.decodeFromString(PersonalityGenerationResult.serializer(), String(body, Charsets.UTF_8))
            }.getOrElse { error ->
                logger.error("Invalid personality result payload messageId={}", messageId, error)
                return DeliveryResult.Ack
            }

        MdcContext.put(CorrelationIds.SEEKER_ID, result.seekerId.toString())
        MdcContext.put(CorrelationIds.USER_ID, result.userId)
        MdcContext.put(CorrelationIds.REQUEST_ID, result.correlationId ?: correlationId)

        if (!dedup.tryMarkProcessing(messageId)) {
            logger.info("Duplicate personality result messageId={}, acking", messageId)
            return DeliveryResult.Ack
        }

        return try {
            applyService.apply(result)
            DeliveryResult.Ack
        } catch (error: Exception) {
            logger.error(
                "Failed to apply personality result seekerId={} messageId={}",
                result.seekerId,
                messageId,
                error,
            )
            DeliveryResult.NackToDlq
        } finally {
            dedup.release(messageId)
        }
    }
}
