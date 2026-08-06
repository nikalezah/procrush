package jobs.procrush.personality.observability

import jobs.procrush.shared.CorrelationIds

object Correlation {
    private val values = ThreadLocal.withInitial { mutableMapOf<String, String>() }

    fun get(key: String): String? = values.get()[key]

    fun currentRequestId(): String? = get(CorrelationIds.REQUEST_ID)

    /** Isolated JVM island (alongside ThreadLocal) so Logger avoids direct Thread APIs. */
    fun currentThreadName(): String = Thread.currentThread().name

    fun put(
        key: String,
        value: String?,
    ) {
        val map = values.get()
        if (value.isNullOrBlank()) {
            map.remove(key)
        } else {
            map[key] = value
        }
    }

    fun putAll(entries: Map<String, String?>) {
        entries.forEach { (key, value) -> put(key, value) }
    }

    fun snapshot(): Map<String, String> = values.get().toMap()

    fun <T> runWith(
        entries: Map<String, String?>,
        block: () -> T,
    ): T {
        val previous = snapshot()
        putAll(entries)
        return try {
            block()
        } finally {
            values.get().clear()
            values.get().putAll(previous)
        }
    }

    fun requestIdFromHeaders(headers: Map<String, Any?>): String? =
        headers[CorrelationIds.HEADER_REQUEST_ID]?.toString()
            ?: headers["x-request-id"]?.toString()
}
