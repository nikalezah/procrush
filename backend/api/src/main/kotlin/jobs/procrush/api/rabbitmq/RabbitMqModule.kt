package jobs.procrush.api.rabbitmq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.MessageConsumer
import jobs.procrush.bootstrap.rabbitmq.MessagePublisher

class RabbitMqModule private constructor(
    private val connection: Connection,
    private val publishChannel: Channel,
    val publisher: MessagePublisher,
    val config: RabbitMqConfig,
) {
    fun createConsumer(): MessageConsumer = JvmMessageConsumer(connection, config)

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
            val publisher = JvmMessagePublisher(publishChannel)
            return RabbitMqModule(connection, publishChannel, publisher, config)
        }
    }
}
