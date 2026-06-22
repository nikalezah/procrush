package jobs.procrush.bootstrap.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import jobs.procrush.bootstrap.config.RabbitMqConfig
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
abstract class RabbitMqTestSupport {
    companion object {
        @Container
        @JvmStatic
        val rabbitMqContainer: RabbitMQContainer =
            RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management-alpine"))
                .withExposedPorts(5672)

        private lateinit var sharedModule: RabbitMqModule

        @JvmStatic
        @org.junit.jupiter.api.BeforeAll
        fun startRabbitMq() {
            if (!rabbitMqContainer.isRunning) {
                rabbitMqContainer.start()
            }
            sharedModule = RabbitMqModule.create(rabbitMqConfig())
        }

        @JvmStatic
        @org.junit.jupiter.api.AfterAll
        fun stopRabbitMq() {
            if (::sharedModule.isInitialized) {
                sharedModule.close()
            }
            if (rabbitMqContainer.isRunning) {
                rabbitMqContainer.stop()
            }
        }

        fun rabbitMqModule(): RabbitMqModule = sharedModule

        fun rabbitMqConfig(): RabbitMqConfig {
            val factory = ConnectionFactory()
            factory.setUri(rabbitMqContainer.amqpUrl)
            return RabbitMqConfig(
                url = rabbitMqContainer.amqpUrl,
            )
        }
    }
}
