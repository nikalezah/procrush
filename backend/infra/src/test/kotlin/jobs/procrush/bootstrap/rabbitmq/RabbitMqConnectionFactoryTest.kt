package jobs.procrush.bootstrap.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class RabbitMqConnectionFactoryTest {
    @Test
    fun trailingSlashUsesDefaultVhost() {
        val factory = ConnectionFactory()
        RabbitMqConnectionFactory.configure(factory, "amqp://procrush:procrush@localhost:5672/")
        assertEquals("/", factory.virtualHost)
    }

    @Test
    fun encodedSlashUsesDefaultVhost() {
        val factory = ConnectionFactory()
        RabbitMqConnectionFactory.configure(factory, "amqp://procrush:procrush@localhost:5672/%2F")
        assertEquals("/", factory.virtualHost)
    }
}
