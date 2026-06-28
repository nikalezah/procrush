package jobs.procrush.personality.messaging

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import jobs.procrush.bootstrap.config.RabbitMqConfig
import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.rabbitmq.RabbitMqModule
import jobs.procrush.bootstrap.redis.RedisDistributedLock
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.service.PersonalityGenerationHandler
import jobs.procrush.personality.service.PersonalityGenerationLockGuard
import jobs.procrush.personality.service.RedisPersonalityStatusNotifier
import jobs.procrush.seeker.repository.SeekerPersonalProfileRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.UUID

class PersonalityJobConsumer(
    private val rabbitMq: RabbitMqModule,
    private val handler: PersonalityGenerationHandler,
    private val publisher: PersonalityJobPublisher,
    private val profileRepository: SeekerPersonalProfileRepository,
    private val statusNotifier: RedisPersonalityStatusNotifier,
    private val lockGuard: PersonalityGenerationLockGuard,
    private val distributedLock: RedisDistributedLock,
    private val dedup: PersonalityMessageDedup,
    private val redisConfig: RedisConfig,
    private val rabbitMqConfig: RabbitMqConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(PersonalityJobConsumer::class.java)
    private var consumerTag: String? = null
    private var consumerChannel: com.rabbitmq.client.Channel? = null

    fun start() {
        if (consumerTag != null) return
        val channel = rabbitMq.createConsumerChannel()
        consumerChannel = channel
        consumerTag =
            channel.basicConsume(
                rabbitMqConfig.queue,
                false,
                object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String,
                        envelope: Envelope,
                        properties: AMQP.BasicProperties,
                        body: ByteArray,
                    ) {
                        processDelivery(channel, envelope.deliveryTag, properties, body)
                    }
                },
            )
        logger.info("Personality job consumer started on queue {}", rabbitMqConfig.queue)
    }

    fun stop() {
        val channel = consumerChannel ?: return
        consumerTag?.let { channel.basicCancel(it) }
        runCatching { channel.close() }
        consumerTag = null
        consumerChannel = null
        logger.info("Personality job consumer stopped")
    }

    private fun processDelivery(
        channel: com.rabbitmq.client.Channel,
        deliveryTag: Long,
        properties: AMQP.BasicProperties,
        body: ByteArray,
    ) {
        val messageId = properties.messageId ?: UUID.randomUUID().toString()
        val job =
            runCatching {
                json.decodeFromString(PersonalityGenerationJob.serializer(), String(body, Charsets.UTF_8))
            }.getOrElse { error ->
                logger.error("Invalid personality job payload messageId={}", messageId, error)
                channel.basicAck(deliveryTag, false)
                return
            }

        val userId =
            runCatching { UUID.fromString(job.userId) }.getOrElse { error ->
                logger.error("Invalid personality job userId messageId={}", messageId, error)
                channel.basicAck(deliveryTag, false)
                return
            }

        if (!dedup.tryMarkProcessing(messageId)) {
            logger.info("Duplicate personality job messageId={}, acking", messageId)
            channel.basicAck(deliveryTag, false)
            return
        }

        val lockHandle =
            distributedLock.tryAcquire(lockGuard.lockKey(job.seekerId), redisConfig.personalityLockTtlSeconds)
        if (lockHandle == null) {
            dedup.release(messageId)
            channel.basicNack(deliveryTag, false, true)
            return
        }

        try {
            if (handler.isAlreadyReady(job.seekerId)) {
                statusNotifier.notify(userId, PersonalityProfileStatus.READY)
                channel.basicAck(deliveryTag, false)
                return
            }

            profileRepository.markProcessing(job.seekerId)

            runBlocking {
                handler.generate(job.seekerId, userId)
            }

            statusNotifier.notify(userId, PersonalityProfileStatus.READY)
            channel.basicAck(deliveryTag, false)
        } catch (error: Exception) {
            logger.error(
                "Personality profile generation failed seekerId={} attempt={}",
                job.seekerId,
                job.attempt,
                error,
            )
            if (isTransient(error) && job.attempt < rabbitMqConfig.maxRetries) {
                publisher.enqueue(job.seekerId, userId, attempt = job.attempt + 1)
                channel.basicAck(deliveryTag, false)
            } else {
                profileRepository.markFailed(job.seekerId, handler.failureCode(error))
                statusNotifier.notify(userId, PersonalityProfileStatus.FAILED)
                channel.basicNack(deliveryTag, false, false)
            }
        } finally {
            lockHandle?.let { distributedLock.release(it) }
            dedup.release(messageId)
        }
    }

    private fun isTransient(error: Throwable): Boolean =
        error is IOException ||
            error is io.ktor.client.plugins.HttpRequestTimeoutException ||
            error.cause?.let { isTransient(it) } == true
}
