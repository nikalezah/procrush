package jobs.procrush.bootstrap.rabbitmq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import jobs.procrush.bootstrap.config.RabbitMqConfig

class RabbitMqModule private constructor(
    private val connection: Connection,
    val publishChannel: Channel,
    val config: RabbitMqConfig,
) {
    fun createConsumerChannel(): Channel {
        val channel = connection.createChannel()
        RabbitMqTopology.declare(channel, config)
        channel.basicQos(config.prefetch)
        return channel
    }

    fun isConnected(): Boolean = connection.isOpen

    fun close() {
        runCatching { publishChannel.close() }
        runCatching { connection.close() }
    }

    companion object {
        fun create(config: RabbitMqConfig): RabbitMqModule {
            val factory = ConnectionFactory()
            RabbitMqConnectionFactory.configure(factory, config.url)
            val connection = factory.newConnection("procrush")
            val publishChannel = connection.createChannel()
            RabbitMqTopology.declare(publishChannel, config)
            return RabbitMqModule(connection, publishChannel, config)
        }
    }
}
