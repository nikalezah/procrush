package jobs.procrush.observability

import io.opentelemetry.api.OpenTelemetry
import jobs.procrush.bootstrap.config.ObservabilityConfig

data class ObservabilityRuntime(
    val config: ObservabilityConfig,
    val tracing: Tracing,
    val openTelemetry: OpenTelemetry,
)

object ObservabilityHolder {
    lateinit var tracing: Tracing
        private set

    lateinit var runtime: ObservabilityRuntime
        private set

    fun initialize(defaultServiceName: String): ObservabilityRuntime {
        val config = ObservabilityConfig.fromEnvironment(defaultServiceName)
        val openTelemetry = OpenTelemetryFactory.create(config)
        val tracing = Tracing.create(openTelemetry, config.serviceName)
        AppMetrics.initialize(config)
        val runtime = ObservabilityRuntime(config, tracing, openTelemetry)
        ObservabilityHolder.tracing = tracing
        ObservabilityHolder.runtime = runtime
        return runtime
    }
}
