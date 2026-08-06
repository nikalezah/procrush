package jobs.procrush.bootstrap.rabbitmq

data class OutboundMessage(
    val exchange: String,
    val routingKey: String,
    val body: ByteArray,
    val messageId: String,
    val headers: Map<String, String> = emptyMap(),
    val contentType: String = "application/json",
    val persistent: Boolean = true,
)

data class InboundMessage(
    val body: ByteArray,
    val messageId: String?,
    val headers: Map<String, String>,
    val deliveryTag: Long,
)

sealed interface DeliveryResult {
    data object Ack : DeliveryResult

    data object NackToDlq : DeliveryResult // basicNack(requeue = false)
}

interface MessagePublisher {
    fun publish(message: OutboundMessage)
}

interface MessageConsumer {
    fun start(
        queue: String,
        handler: (InboundMessage) -> DeliveryResult,
    )

    fun stop()

    fun isRunning(): Boolean
}
