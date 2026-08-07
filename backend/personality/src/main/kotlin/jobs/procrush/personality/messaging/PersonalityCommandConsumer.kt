package jobs.procrush.personality.messaging

import io.ktor.client.plugins.HttpRequestTimeoutException
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.DeliveryResult
import jobs.procrush.bootstrap.rabbitmq.InboundMessage
import jobs.procrush.bootstrap.rabbitmq.MessageConsumer
import jobs.procrush.personality.observability.Correlation
import jobs.procrush.personality.observability.Logger
import jobs.procrush.personality.observability.Metrics
import jobs.procrush.personality.service.PersonalityGenerationHandler
import jobs.procrush.shared.CorrelationIds
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PersonalityCommandConsumer(
    private val messageConsumer: MessageConsumer,
    private val handler: PersonalityGenerationHandler,
    private val commandPublisher: PersonalityCommandPublisher,
    private val resultPublisher: PersonalityResultPublisher,
    private val rabbitMqConfig: RabbitMqConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = Logger.get(PersonalityCommandConsumer::class)

    fun start() {
        if (messageConsumer.isRunning()) return
        messageConsumer.start(rabbitMqConfig.queue) { inbound -> processDelivery(inbound) }
        Metrics.setPersonalityConsumerRunning(true)
        runBlocking {
            logger.info("Personality command consumer started on queue {}", rabbitMqConfig.queue)
        }
    }

    fun stop() {
        if (!messageConsumer.isRunning()) return
        messageConsumer.stop()
        Metrics.setPersonalityConsumerRunning(false)
        runBlocking {
            logger.info("Personality command consumer stopped")
        }
    }

    fun isRunning(): Boolean = messageConsumer.isRunning()

    @OptIn(ExperimentalUuidApi::class)
    private fun processDelivery(inbound: InboundMessage): DeliveryResult {
        val messageId = inbound.messageId ?: Uuid.random().toString()
        val correlationId = Correlation.requestIdFromHeaders(inbound.headers) ?: messageId
        return Correlation.runWith(
            mapOf(
                CorrelationIds.REQUEST_ID to correlationId,
                CorrelationIds.MESSAGE_ID to messageId,
            ),
        ) {
            processDeliveryInternal(inbound.body, messageId, correlationId)
        }
    }

    private suspend fun processDeliveryInternal(
        body: ByteArray,
        messageId: String,
        correlationId: String,
    ): DeliveryResult {
        val command =
            runCatching {
                json.decodeFromString(PersonalityGenerationCommand.serializer(), String(body, Charsets.UTF_8))
            }.getOrElse { error ->
                logger.error("Invalid personality command payload messageId={}", messageId, error)
                return DeliveryResult.Ack
            }

        Correlation.put(CorrelationIds.SEEKER_ID, command.seekerId.toString())
        Correlation.put(CorrelationIds.USER_ID, command.userId)
        Correlation.put(CorrelationIds.REQUEST_ID, command.correlationId ?: correlationId)

        return try {
            val result =
                runBlocking(currentCoroutineContext()) {
                    handler.generate(command)
                }.copy(commandMessageId = messageId)
            resultPublisher.publish(result, correlationId = command.correlationId ?: correlationId)
            Metrics.personalityJobProcessed("success")
            DeliveryResult.Ack
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
                Metrics.personalityJobProcessed("retry")
                DeliveryResult.Ack
            } else {
                resultPublisher.publish(
                    handler.failureResult(command, error, messageId),
                    correlationId = command.correlationId ?: correlationId,
                )
                Metrics.personalityJobDlq()
                Metrics.personalityJobProcessed("dlq")
                DeliveryResult.NackToDlq
            }
        }
    }

    private fun isTransient(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is HttpRequestTimeoutException) return true
            when (current::class.simpleName) {
                "IOException", "SocketException", "SocketTimeoutException",
                "ConnectException", "ClosedChannelException", "EOFException",
                "UnknownHostException", "HttpRequestTimeoutException",
                -> return true
            }
            current = current.cause
        }
        return false
    }
}
