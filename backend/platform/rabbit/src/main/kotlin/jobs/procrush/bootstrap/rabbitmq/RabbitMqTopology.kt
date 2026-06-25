package jobs.procrush.bootstrap.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import jobs.procrush.bootstrap.config.RabbitMqConfig

object RabbitMqTopology {
    fun declare(channel: Channel, config: RabbitMqConfig) {
        channel.exchangeDeclare(config.exchange, BuiltinExchangeType.DIRECT, true)
        channel.exchangeDeclare(config.deadLetterExchange, BuiltinExchangeType.DIRECT, true)

        val queueArgs =
            mapOf(
                "x-dead-letter-exchange" to config.deadLetterExchange,
                "x-dead-letter-routing-key" to config.deadLetterRoutingKey,
            )
        channel.queueDeclare(config.queue, true, false, false, queueArgs)
        channel.queueDeclare(config.deadLetterQueue, true, false, false, null)
        channel.queueBind(config.queue, config.exchange, config.routingKey)
        channel.queueBind(config.deadLetterQueue, config.deadLetterExchange, config.deadLetterRoutingKey)
    }

    fun persistentJsonProperties(messageId: String): AMQP.BasicProperties =
        AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(2)
            .messageId(messageId)
            .build()
}
