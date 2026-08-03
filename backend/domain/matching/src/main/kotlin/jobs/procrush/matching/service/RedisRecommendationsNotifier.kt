package jobs.procrush.matching.service

import io.lettuce.core.pubsub.RedisPubSubAdapter
import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisClient
import jobs.procrush.matching.dto.RecommendationsUpdatedEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RedisRecommendationsNotifier(
    private val redis: RedisClient,
    private val config: RedisConfig,
    private val scope: CoroutineScope,
    private val debounceMs: Long = 400,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(RedisRecommendationsNotifier::class.java)
    private val localSubscribers =
        ConcurrentHashMap<UUID, MutableSet<Channel<RecommendationsUpdatedEventDto>>>()
    private val pending = ConcurrentHashMap<UUID, PendingNotify>()
    private val broadcastChannel = config.key("events", "recommendations")

    private var pubSubConnection = redis.pubSubConnection()
    private var started = false

    fun start() {
        if (started) return
        started = true
        val listener =
            object : RedisPubSubAdapter<String, String>() {
                override fun message(
                    channel: String,
                    message: String,
                ) {
                    if (channel != broadcastChannel) return
                    deliverLocal(message)
                }
            }
        pubSubConnection.addListener(listener)
        pubSubConnection.sync().subscribe(broadcastChannel)
        logger.info("Redis recommendations pub/sub listener started on channel {}", broadcastChannel)
    }

    fun subscribe(userId: UUID): Channel<RecommendationsUpdatedEventDto> {
        val channel = Channel<RecommendationsUpdatedEventDto>(Channel.BUFFERED)
        localSubscribers.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(channel)
        return channel
    }

    fun unsubscribe(userId: UUID, channel: Channel<RecommendationsUpdatedEventDto>) {
        localSubscribers[userId]?.remove(channel)
        channel.close()
        if (localSubscribers[userId]?.isEmpty() == true) {
            localSubscribers.remove(userId)
        }
    }

    fun scheduleNotify(userId: UUID, event: RecommendationsUpdatedEventDto) {
        pending.compute(userId) { _, existing ->
            existing?.job?.cancel()
            val job =
                scope.launch {
                    delay(debounceMs)
                    val latest = pending.remove(userId)?.event ?: event
                    publish(userId, latest)
                }
            PendingNotify(event = event, job = job)
        }
    }

    private fun publish(userId: UUID, event: RecommendationsUpdatedEventDto) {
        val envelope =
            RecommendationsEnvelope(
                userId = userId.toString(),
                event = event,
            )
        redis.publish(broadcastChannel, json.encodeToString(RecommendationsEnvelope.serializer(), envelope))
    }

    override fun close() {
        pending.values.forEach { it.job.cancel() }
        pending.clear()
        runCatching {
            pubSubConnection.sync().unsubscribe(broadcastChannel)
            pubSubConnection.close()
        }
        localSubscribers.values.flatten().forEach { it.close() }
        localSubscribers.clear()
        started = false
    }

    private fun deliverLocal(message: String) {
        val envelope =
            runCatching {
                json.decodeFromString(RecommendationsEnvelope.serializer(), message)
            }.getOrElse { error ->
                logger.warn("Failed to decode recommendations envelope", error)
                return
            }
        val userId =
            runCatching { UUID.fromString(envelope.userId) }.getOrElse { error ->
                logger.warn("Invalid recommendations userId {}", envelope.userId, error)
                return
            }
        localSubscribers[userId]?.forEach { subscriber ->
            scope.launch {
                subscriber.send(envelope.event)
            }
        }
    }

    private data class PendingNotify(
        val event: RecommendationsUpdatedEventDto,
        val job: Job,
    )

    @Serializable
    private data class RecommendationsEnvelope(
        val userId: String,
        val event: RecommendationsUpdatedEventDto,
    )
}
