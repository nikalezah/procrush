package jobs.procrush.bootstrap.rabbitmq

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import jobs.procrush.bootstrap.config.RabbitMqConfig

class RabbitMqModule private constructor(
    private val connection: Connection,
    private val publishChannel: com.rabbitmq.client.Channel,
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
