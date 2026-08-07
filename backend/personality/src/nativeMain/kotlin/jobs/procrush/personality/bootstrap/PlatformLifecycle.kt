package jobs.procrush.personality.bootstrap

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import platform.posix.SIGINT
import platform.posix.SIGTERM
import platform.posix.signal
import platform.posix.usleep
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Native process lifecycle helpers (POSIX signals).
 * Personality has no JVM target — this is the only implementation.
 *
 * Call [onShutdown] to register cleanup, then [awaitShutdown] **after**
 * `server.start(wait = false)` so engine/runtime signal hooks do not replace ours.
 * Signal handlers only flip an atomic flag.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)
object PlatformLifecycle {
    private var hook: (() -> Unit)? = null

    fun onShutdown(block: () -> Unit) {
        hook = block
        installSignalHandlers()
    }

    /** Blocks until SIGINT/SIGTERM, then runs the registered shutdown hook. */
    fun awaitShutdown() {
        var spins = 0
        while (shutdownFlag.load() == 0) {
            // Re-arm periodically in case runtime/engine overwrites handlers.
            if (spins % 20 == 0) {
                installSignalHandlers()
            }
            spins++
            usleep(50_000u)
        }
        val block = hook
        hook = null
        block?.invoke()
    }

    private fun installSignalHandlers() {
        signal(SIGINT, staticCFunction(::handlePosixSignal))
        signal(SIGTERM, staticCFunction(::handlePosixSignal))
    }
}

@OptIn(ExperimentalAtomicApi::class)
private val shutdownFlag = AtomicInt(0)

@OptIn(ExperimentalAtomicApi::class)
private fun handlePosixSignal(@Suppress("UNUSED_PARAMETER") signal: Int) {
    shutdownFlag.store(1)
}
