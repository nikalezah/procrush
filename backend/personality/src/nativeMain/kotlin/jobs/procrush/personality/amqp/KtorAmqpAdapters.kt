package jobs.procrush.personality.amqp

import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.DeliveryResult
import jobs.procrush.bootstrap.rabbitmq.InboundMessage
import jobs.procrush.bootstrap.rabbitmq.MessageConsumer
import jobs.procrush.bootstrap.rabbitmq.MessagePublisher
import jobs.procrush.bootstrap.rabbitmq.OutboundMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

internal class KtorMessagePublisher(
    private val channel: AmqpChannel,
    private val scope: CoroutineScope,
) : MessagePublisher {
    override fun publish(message: OutboundMessage) {
        runBlocking(scope.coroutineContext) {
            channel.basicPublish(
                exchange = message.exchange,
                routingKey = message.routingKey,
                properties =
                    ContentProperties(
                        contentType = message.contentType,
                        deliveryMode = if (message.persistent) 2 else 1,
                        headers = message.headers,
                        messageId = message.messageId,
                    ),
                body = message.body,
            )
        }
    }
}

internal class KtorMessageConsumer(
    private val connection: AmqpConnection,
    private val config: RabbitMqConfig,
    private val scope: CoroutineScope,
) : MessageConsumer {
    private var consumerChannel: AmqpChannel? = null
    private var consumerTag: String? = null

    override fun start(
        queue: String,
        handler: (InboundMessage) -> DeliveryResult,
    ) {
        if (consumerTag != null) return
        runBlocking(scope.coroutineContext) {
            val channel = connection.openChannel()
            AmqpTopology.declare(channel, config)
            channel.basicQos(config.prefetch)
            consumerChannel = channel
            consumerTag =
                channel.basicConsume(queue) { delivery ->
                    // Run user handler off the connection reader path via this consume callback,
                    // which is invoked from the reader; dispatch work on the module scope.
                    val inbound =
                        InboundMessage(
                            body = delivery.body,
                            messageId = delivery.properties.messageId,
                            headers = delivery.properties.headers,
                            deliveryTag = delivery.deliveryTag,
                        )
                    val result =
                        runCatching { handler(inbound) }
                            .getOrElse { DeliveryResult.NackToDlq }
                    when (result) {
                        DeliveryResult.Ack -> channel.basicAck(delivery.deliveryTag)
                        DeliveryResult.NackToDlq -> channel.basicNack(delivery.deliveryTag, requeue = false)
                    }
                }
        }
    }

    override fun stop() {
        val channel = consumerChannel ?: return
        val tag = consumerTag
        runBlocking(scope.coroutineContext) {
            if (tag != null) {
                runCatching { channel.basicCancel(tag) }
            }
            runCatching { channel.close() }
        }
        consumerTag = null
        consumerChannel = null
    }

    override fun isRunning(): Boolean = consumerTag != null
}
