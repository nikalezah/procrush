package jobs.procrush.bootstrap.redis

import com.redis.testcontainers.RedisContainer
import jobs.procrush.bootstrap.config.RedisConfig
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
abstract class RedisTestSupport {
    companion object {
        @Container
        @JvmStatic
        val redisContainer: RedisContainer =
            RedisContainer(DockerImageName.parse("redis:8-alpine"))
                .withExposedPorts(6379)

        private lateinit var sharedClient: RedisClient

        @BeforeAll
        @JvmStatic
        fun startRedis() {
            if (!redisContainer.isRunning) {
                redisContainer.start()
            }
            sharedClient = createClient()
        }

        @AfterAll
        @JvmStatic
        fun stopRedis() {
            if (::sharedClient.isInitialized) {
                sharedClient.close()
            }
        }

        fun redisClient(): RedisClient = sharedClient

        fun redisConfig(): RedisConfig =
            RedisConfig(
                url = "redis://${redisContainer.host}:${redisContainer.firstMappedPort}",
                keyPrefix = "procrush:test:",
            )

        private fun createClient(): RedisClient = RedisClient.connect(redisConfig())
    }
}
