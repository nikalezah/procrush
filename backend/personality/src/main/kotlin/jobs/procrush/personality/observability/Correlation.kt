package jobs.procrush.personality.observability

import jobs.procrush.shared.CorrelationIds
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Delivery-scoped correlation. Implements [ThreadContextElement] so sync call sites
 * (e.g. MessagingLog → Logger.infoBlocking) can reinstall this element into a nested
 * `runBlocking` without a process-global active-delivery pointer.
 */
data class CorrelationElement(
    val values: MutableMap<String, String>,
) : ThreadContextElement<CorrelationElement?> {
    companion object Key : CoroutineContext.Key<CorrelationElement>

    override val key: CoroutineContext.Key<*> get() = Key

    override fun updateThreadContext(context: CoroutineContext): CorrelationElement? {
        val previous = correlationThreadBound.get()
        correlationThreadBound.set(this)
        return previous
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: CorrelationElement?,
    ) {
        correlationThreadBound.set(oldState)
    }
}

/** Per-thread install of the coroutine's [CorrelationElement] while that coroutine is running. */
private val correlationThreadBound = ThreadLocal<CorrelationElement?>()

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

    /**
     * Context for nested `runBlocking` from sync call sites under an active delivery.
     * Reads the thread-bound element installed by [CorrelationElement] as a
     * [ThreadContextElement] — delivery-local, not a shared mutable slot.
     */
    fun syncBridgeContext(): CoroutineContext = correlationThreadBound.get() ?: EmptyCoroutineContext

    fun requestIdFromHeaders(headers: Map<String, Any?>): String? =
        headers[CorrelationIds.HEADER_REQUEST_ID]?.toString()
            ?: headers["x-request-id"]?.toString()

    private suspend fun currentValues(): MutableMap<String, String>? =
        currentCoroutineContext()[CorrelationElement]?.values
}
