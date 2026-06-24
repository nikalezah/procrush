package jobs.procrush.bootstrap.redis

import io.lettuce.core.RedisURI
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import jobs.procrush.bootstrap.config.RedisConfig
import java.time.Duration
import io.lettuce.core.RedisClient as LettuceRedisClient

class RedisClient private constructor(
    private val lettuceClient: LettuceRedisClient,
    private val connection: StatefulRedisConnection<String, String>,
    val config: RedisConfig,
) {
    private val commands: RedisCommands<String, String> = connection.sync()

    fun ping(): String = commands.ping()

    fun get(key: String): String? = commands.get(key)

    fun setEx(
        key: String,
        seconds: Long,
        value: String,
    ) {
        commands.setex(key, seconds, value)
    }

    fun del(vararg keys: String): Long = commands.del(*keys)

    fun setNxEx(
        key: String,
        value: String,
        seconds: Long,
    ): Boolean = commands.set(key, value, SetArgs().nx().ex(seconds)) == "OK"

    fun releaseLock(
        key: String,
        token: String,
    ): Boolean {
        val script =
            """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """.trimIndent()
        val result = commands.eval<Long>(script, ScriptOutputType.INTEGER, arrayOf(key), token)
        return result == 1L
    }

    fun exists(key: String): Boolean = commands.exists(key) > 0

    fun publish(
        channel: String,
        message: String,
    ): Long = commands.publish(channel, message)

    fun pubSubConnection(): StatefulRedisPubSubConnection<String, String> = lettuceClient.connectPubSub()

    fun close() {
        connection.close()
        lettuceClient.shutdown(Duration.ofSeconds(2), Duration.ofSeconds(5))
    }

    companion object {
        fun connect(config: RedisConfig): RedisClient {
            val uri = RedisURI.create(config.url)
            val lettuceClient = LettuceRedisClient.create(uri)
            val connection = lettuceClient.connect()
            connection.setTimeout(Duration.ofSeconds(config.commandTimeoutSeconds))
            return RedisClient(lettuceClient, connection, config)
        }
    }
}
