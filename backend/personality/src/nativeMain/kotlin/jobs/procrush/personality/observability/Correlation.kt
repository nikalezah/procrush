package jobs.procrush.personality.observability

import jobs.procrush.shared.CorrelationIds
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

data class CorrelationElement(
    val values: MutableMap<String, String>,
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<CorrelationElement>

    override val key: CoroutineContext.Key<*> get() = Key
}

object Correlation {
    fun currentThreadName(): String = "worker"

    suspend fun get(key: String): String? = currentValues()?.get(key)

    suspend fun currentRequestId(): String? = get(CorrelationIds.REQUEST_ID)

    suspend fun put(
        key: String,
        value: String?,
    ) {
        val map = currentValues() ?: return
        if (value.isNullOrBlank()) {
            map.remove(key)
        } else {
            map[key] = value
        }
    }

    suspend fun putAll(entries: Map<String, String?>) {
        entries.forEach { (key, value) -> put(key, value) }
    }

    suspend fun snapshot(): Map<String, String> = currentValues()?.toMap().orEmpty()

    fun <T> runWith(
        entries: Map<String, String?>,
        block: suspend () -> T,
    ): T {
        val map = mutableMapOf<String, String>()
        entries.forEach { (key, value) ->
            if (!value.isNullOrBlank()) {
                map[key] = value
            }
        }
        return runBlocking(CorrelationElement(map)) {
            block()
        }
    }

    fun requestIdFromHeaders(headers: Map<String, Any?>): String? =
        headers[CorrelationIds.HEADER_REQUEST_ID]?.toString()
            ?: headers["x-request-id"]?.toString()

    private suspend fun currentValues(): MutableMap<String, String>? =
        currentCoroutineContext()[CorrelationElement]?.values
}
