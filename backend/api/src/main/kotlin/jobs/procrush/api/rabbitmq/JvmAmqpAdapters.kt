package jobs.procrush.api.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.DeliveryResult
import jobs.procrush.bootstrap.rabbitmq.InboundMessage
import jobs.procrush.bootstrap.rabbitmq.MessageConsumer
import jobs.procrush.bootstrap.rabbitmq.MessagePublisher
import jobs.procrush.bootstrap.rabbitmq.OutboundMessage

internal class JvmMessagePublisher(
    private val channel: Channel,
) : MessagePublisher {
    override fun publish(message: OutboundMessage) {
        channel.basicPublish(
            message.exchange,
            message.routingKey,
            persistentJsonProperties(message),
            message.body,
        )
    }

    private fun persistentJsonProperties(message: OutboundMessage): AMQP.BasicProperties {
        val headers: Map<String, Any>? = message.headers.ifEmpty { null }
        return AMQP.BasicProperties.Builder()
            .contentType(message.contentType)
            .deliveryMode(if (message.persistent) 2 else 1)
            .messageId(message.messageId)
            .headers(headers)
            .build()
    }
}

internal class JvmMessageConsumer(
    private val connection: Connection,
    private val config: RabbitMqConfig,
) : MessageConsumer {
    private var consumerTag: String? = null
    private var consumerChannel: Channel? = null

    override fun start(
        queue: String,
        handler: (InboundMessage) -> DeliveryResult,
    ) {
        if (consumerTag != null) return
        val channel = connection.createChannel()
        RabbitMqTopology.declare(channel, config)
        channel.basicQos(config.prefetch)
        consumerChannel = channel
        consumerTag =
            channel.basicConsume(
                queue,
                false,
                object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String,
                        envelope: Envelope,
                        properties: AMQP.BasicProperties,
                        body: ByteArray,
                    ) {
                        val inbound =
                            InboundMessage(
                                body = body,
                                messageId = properties.messageId,
                                headers = flattenHeaders(properties.headers),
                                deliveryTag = envelope.deliveryTag,
                            )
                        when (handler(inbound)) {
                            DeliveryResult.Ack -> channel.basicAck(envelope.deliveryTag, false)
                            DeliveryResult.NackToDlq -> channel.basicNack(envelope.deliveryTag, false, false)
                        }
                    }
                },
            )
    }

    override fun stop() {
        val channel = consumerChannel ?: return
        consumerTag?.let { channel.basicCancel(it) }
        runCatching { channel.close() }
        consumerTag = null
        consumerChannel = null
    }

    override fun isRunning(): Boolean = consumerTag != null

    private fun flattenHeaders(headers: Map<String, Any?>?): Map<String, String> =
        headers.orEmpty().mapValues { (_, value) -> value?.toString().orEmpty() }
}
