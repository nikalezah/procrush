package jobs.procrush.bootstrap.redis

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.config.WorkerAppConfig

data class RedisModule(
    val client: RedisClient,
    val distributedLock: RedisDistributedLock,
    private val onCloseHooks: MutableList<AutoCloseable> = mutableListOf(),
) {
    fun registerOnClose(hook: AutoCloseable) {
        onCloseHooks += hook
    }

    fun close() {
        onCloseHooks.forEach { runCatching { it.close() } }
        client.close()
    }

    companion object {
        fun create(config: AppConfig): RedisModule = create(config.redis)

        fun create(config: WorkerAppConfig): RedisModule = create(config.redis)

        fun create(config: RedisConfig): RedisModule {
            val client = RedisClient.connect(config)
            val distributedLock = RedisDistributedLock(client)
            return RedisModule(client = client, distributedLock = distributedLock)
        }
    }
}
