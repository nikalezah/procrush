package jobs.procrush.personality.messaging

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import jobs.procrush.bootstrap.rabbitmq.RabbitMqTestSupport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
class PersonalityJobPublisherTest : RabbitMqTestSupport() {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun publishDeliversJobToQueue() {
        val module = rabbitMqModule()
        val config = rabbitMqConfig()
        val publisher = PersonalityJobPublisher(module.publishChannel, config)
        val seekerId = 42L
        val userId = UUID.randomUUID()

        val received = CompletableDeferred<PersonalityGenerationJob>()
        val consumerChannel = module.createConsumerChannel()
        val consumerTag =
            consumerChannel.basicConsume(
                config.queue,
                false,
                object : DefaultConsumer(consumerChannel) {
                    override fun handleDelivery(
                        consumerTag: String,
                        envelope: Envelope,
                        properties: AMQP.BasicProperties,
                        body: ByteArray,
                    ) {
                        val job = json.decodeFromString(PersonalityGenerationJob.serializer(), String(body, Charsets.UTF_8))
                        received.complete(job)
                        consumerChannel.basicAck(envelope.deliveryTag, false)
                    }
                },
            )

        try {
            publisher.enqueue(seekerId, userId, attempt = 1)
            val job =
                runBlocking {
                    withTimeout(10_000) { received.await() }
                }
            assertEquals(seekerId, job.seekerId)
            assertEquals(userId.toString(), job.userId)
            assertEquals(1, job.attempt)
            assertTrue(job.enqueuedAt.isNotBlank())
        } finally {
            consumerChannel.basicCancel(consumerTag)
            consumerChannel.close()
        }
    }
}
