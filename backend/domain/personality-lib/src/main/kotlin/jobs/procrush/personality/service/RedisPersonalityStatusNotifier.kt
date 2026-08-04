package jobs.procrush.personality.service

import io.lettuce.core.pubsub.RedisPubSubAdapter
import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisClient
import jobs.procrush.personality.dto.PersonalityProfileStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RedisPersonalityStatusNotifier(
    private val redis: RedisClient,
    private val config: RedisConfig,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val logger = LoggerFactory.getLogger(RedisPersonalityStatusNotifier::class.java)
    private val localSubscribers =
        ConcurrentHashMap<UUID, MutableSet<Channel<PersonalityProfileStatus>>>()
    private val broadcastChannel = config.key("events", "personality")

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
        logger.info("Redis personality-status pub/sub listener started on channel {}", broadcastChannel)
    }

    fun subscribe(userId: UUID): Channel<PersonalityProfileStatus> {
        val channel = Channel<PersonalityProfileStatus>(Channel.BUFFERED)
        localSubscribers.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(channel)
        return channel
    }

    fun unsubscribe(userId: UUID, channel: Channel<PersonalityProfileStatus>) {
        localSubscribers[userId]?.remove(channel)
        channel.close()
        if (localSubscribers[userId]?.isEmpty() == true) {
            localSubscribers.remove(userId)
        }
    }

    fun notify(userId: UUID, status: PersonalityProfileStatus) {
        val envelope =
            PersonalityStatusEnvelope(
                userId = userId.toString(),
                status = status.name,
            )
        redis.publish(broadcastChannel, json.encodeToString(PersonalityStatusEnvelope.serializer(), envelope))
    }

    fun close() {
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
                json.decodeFromString(PersonalityStatusEnvelope.serializer(), message)
            }.getOrElse { error ->
                logger.warn("Failed to decode personality-status envelope", error)
                return
            }
        val userId =
            runCatching { UUID.fromString(envelope.userId) }.getOrElse { error ->
                logger.warn("Invalid personality-status userId {}", envelope.userId, error)
                return
            }
        val status =
            runCatching { PersonalityProfileStatus.valueOf(envelope.status) }.getOrElse { error ->
                logger.warn("Invalid personality-status value {}", envelope.status, error)
                return
            }
        localSubscribers[userId]?.forEach { subscriber ->
            scope.launch {
                subscriber.send(status)
            }
        }
    }

    @Serializable
    private data class PersonalityStatusEnvelope(
        val userId: String,
        val status: String,
    )
}
