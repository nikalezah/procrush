package jobs.procrush.personality.observability

object WorkerObservability {
    lateinit var config: WorkerLogConfig
        private set

    fun initialize(defaultServiceName: String = "personality"): WorkerLogConfig {
        val loaded = WorkerLogConfig.fromEnvironment(defaultServiceName)
        config = loaded
        Metrics.initialize(loaded)
        return loaded
    }
}
