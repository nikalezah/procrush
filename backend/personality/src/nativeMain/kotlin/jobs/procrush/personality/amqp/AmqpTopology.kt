package jobs.procrush.personality.amqp

import jobs.procrush.bootstrap.config.RabbitMqConfig

internal object AmqpTopology {
    suspend fun declare(
        channel: AmqpChannel,
        config: RabbitMqConfig,
    ) {
        channel.exchangeDeclare(config.exchange, type = "direct", durable = true)
        channel.exchangeDeclare(config.deadLetterExchange, type = "direct", durable = true)

        val commandQueueArgs =
            mapOf(
                "x-dead-letter-exchange" to AmqpField.LongString(config.deadLetterExchange),
                "x-dead-letter-routing-key" to AmqpField.LongString(config.deadLetterRoutingKey),
            )
        channel.queueDeclare(config.queue, durable = true, arguments = commandQueueArgs)
        channel.queueDeclare(config.deadLetterQueue, durable = true)
        channel.queueBind(config.queue, config.exchange, config.routingKey)
        channel.queueBind(config.deadLetterQueue, config.deadLetterExchange, config.deadLetterRoutingKey)

        val resultsQueueArgs =
            mapOf(
                "x-dead-letter-exchange" to AmqpField.LongString(config.deadLetterExchange),
                "x-dead-letter-routing-key" to AmqpField.LongString(config.resultsDeadLetterRoutingKey),
            )
        channel.queueDeclare(config.resultsQueue, durable = true, arguments = resultsQueueArgs)
        channel.queueDeclare(config.resultsDeadLetterQueue, durable = true)
        channel.queueBind(config.resultsQueue, config.exchange, config.resultsRoutingKey)
        channel.queueBind(
            config.resultsDeadLetterQueue,
            config.deadLetterExchange,
            config.resultsDeadLetterRoutingKey,
        )
    }
}
