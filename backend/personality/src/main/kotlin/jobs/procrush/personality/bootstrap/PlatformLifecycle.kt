package jobs.procrush.personality.bootstrap

/**
 * JVM-backed process lifecycle helpers. Call sites use only this API so later
 * expect/actual porting does not churn shutdown consumers.
 */
object PlatformLifecycle {
    fun onShutdown(block: () -> Unit) {
        Runtime.getRuntime().addShutdownHook(Thread(block))
    }
}
