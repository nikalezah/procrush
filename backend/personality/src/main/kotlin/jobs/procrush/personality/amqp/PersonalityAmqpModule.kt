package jobs.procrush.personality.amqp

import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.rabbitmq.MessageConsumer
import jobs.procrush.bootstrap.rabbitmq.MessagePublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

/**
 * Personality-local AMQP facade (same surface as the API [jobs.procrush.api.rabbitmq.RabbitMqModule]).
 * Transport is a thin AMQP 0-9-1 client on ktor-network — no `amqp-client`.
 */
class PersonalityAmqpModule private constructor(
    private val connection: AmqpConnection,
    private val publishChannel: AmqpChannel,
    private val scope: CoroutineScope,
    val publisher: MessagePublisher,
    val config: RabbitMqConfig,
) {
    fun createConsumer(): MessageConsumer = KtorMessageConsumer(connection, config, scope)

    fun isConnected(): Boolean = connection.isOpen()

    fun close() {
        runBlocking {
            runCatching { publishChannel.close() }
            runCatching { connection.close() }
        }
        scope.cancel()
    }

    companion object {
        fun create(config: RabbitMqConfig): PersonalityAmqpModule =
            runBlocking {
                val url = AmqpUrl.parse(config.url)
                val connection = AmqpConnection.connect(url)
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                val publishChannel = connection.openChannel()
                AmqpTopology.declare(publishChannel, config)
                val publisher = KtorMessagePublisher(publishChannel, scope)
                PersonalityAmqpModule(
                    connection = connection,
                    publishChannel = publishChannel,
                    scope = scope,
                    publisher = publisher,
                    config = config,
                )
            }
    }
}
