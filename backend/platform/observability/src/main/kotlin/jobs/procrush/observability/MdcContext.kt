package jobs.procrush.observability

import org.slf4j.MDC

object MdcContext {
    fun currentRequestId(): String? = MDC.get(CorrelationIds.REQUEST_ID)

    fun put(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            MDC.remove(key)
        } else {
            MDC.put(key, value)
        }
    }

    fun putAll(values: Map<String, String?>) {
        values.forEach { (key, value) -> put(key, value) }
    }

    fun clearKeys(vararg keys: String) {
        keys.forEach { MDC.remove(it) }
    }

    fun <T> runWith(
        values: Map<String, String?>,
        block: () -> T,
    ): T {
        val previous = MDC.getCopyOfContextMap()
        putAll(values)
        return try {
            block()
        } finally {
            if (previous == null) {
                MDC.clear()
            } else {
                MDC.setContextMap(previous)
            }
        }
    }

    fun <T> runWithRequestId(
        requestId: String,
        block: () -> T,
    ): T = runWith(mapOf(CorrelationIds.REQUEST_ID to requestId), block)
}
