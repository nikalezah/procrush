package jobs.procrush.matching.service

import io.lettuce.core.pubsub.RedisPubSubAdapter
import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisClient
import jobs.procrush.matching.dto.MatchInterestEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RedisMatchInterestNotifier(
    private val redis: RedisClient,
    private val config: RedisConfig,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(RedisMatchInterestNotifier::class.java)
    private val localSubscribers =
        ConcurrentHashMap<UUID, MutableSet<Channel<MatchInterestEventDto>>>()
    private val broadcastChannel = config.key("events", "match")

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
        logger.info("Redis match-interest pub/sub listener started on channel {}", broadcastChannel)
    }

    fun subscribe(userId: UUID): Channel<MatchInterestEventDto> {
        val channel = Channel<MatchInterestEventDto>(Channel.BUFFERED)
        localSubscribers.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(channel)
        return channel
    }

    fun unsubscribe(userId: UUID, channel: Channel<MatchInterestEventDto>) {
        localSubscribers[userId]?.remove(channel)
        channel.close()
        if (localSubscribers[userId]?.isEmpty() == true) {
            localSubscribers.remove(userId)
        }
    }

    fun notify(userId: UUID, event: MatchInterestEventDto) {
        val envelope =
            MatchInterestEnvelope(
                userId = userId.toString(),
                event = event,
            )
        redis.publish(broadcastChannel, json.encodeToString(MatchInterestEnvelope.serializer(), envelope))
    }

    override fun close() {
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
                json.decodeFromString(MatchInterestEnvelope.serializer(), message)
            }.getOrElse { error ->
                logger.warn("Failed to decode match-interest envelope", error)
                return
            }
        val userId =
            runCatching { UUID.fromString(envelope.userId) }.getOrElse { error ->
                logger.warn("Invalid match-interest userId {}", envelope.userId, error)
                return
            }
        localSubscribers[userId]?.forEach { subscriber ->
            scope.launch {
                subscriber.send(envelope.event)
            }
        }
    }

    @Serializable
    private data class MatchInterestEnvelope(
        val userId: String,
        val event: MatchInterestEventDto,
    )
}
