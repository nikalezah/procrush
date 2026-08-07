package jobs.procrush.personality.amqp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Live RabbitMQ boundary coverage: a body larger than negotiated frameMax must publish
 * and round-trip without closing the connection (the failure mode before body framing).
 */
class AmqpLargePublishIT {
    @Test
    fun publishesBodyLargerThanNegotiatedFrameMax() =
        runBlocking {
            val url =
                AmqpUrl.parse(
                    System.getenv("RABBITMQ_URL")
                        ?: "amqp://procrush:procrush@localhost:5672/%2F",
                )
            val connection = AmqpConnection.connect(url)
            try {
                assertTrue(connection.negotiatedFrameMax() > AmqpFrame.EMPTY_FRAME_SIZE)
                val maxPayload = AmqpFrame.maxBodyPayloadSize(connection.negotiatedFrameMax())
                val body = ByteArray(maxPayload + 8_936) { (it % 251).toByte() }
                assertTrue(body.size > maxPayload)

                val channel = connection.openChannel()
                val queue = "personality.frame-max.it.${System.nanoTime()}"
                channel.queueDeclare(queue, durable = false, exclusive = true, autoDelete = true)

                val received = CompletableDeferred<ByteArray>()
                channel.basicConsume(queue) { delivery ->
                    received.complete(delivery.body)
                    channel.basicAck(delivery.deliveryTag)
                }

                channel.basicPublish(
                    exchange = "",
                    routingKey = queue,
                    properties =
                        ContentProperties(
                            contentType = "application/octet-stream",
                            deliveryMode = 1,
                            messageId = "frame-max-it",
                        ),
                    body = body,
                )

                val got =
                    withTimeout(15.seconds) {
                        received.await()
                    }
                assertEquals(body.size, got.size)
                assertTrue(got.contentEquals(body))
                assertTrue(connection.isOpen())
                channel.close()
            } finally {
                connection.close()
            }
        }
}
