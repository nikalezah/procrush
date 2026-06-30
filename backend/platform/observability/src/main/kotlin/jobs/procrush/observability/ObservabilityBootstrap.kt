package jobs.procrush.observability

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call

fun Application.configureObservabilityPlugins(runtime: ObservabilityRuntime) {
    configureRequestContext()
    configureMetrics(runtime.config)
    configureHttpTracing(runtime.tracing)
}

fun Application.bootstrapObservability(defaultServiceName: String): ObservabilityRuntime {
    val runtime = ObservabilityHolder.initialize(defaultServiceName)
    configureObservabilityPlugins(runtime)
    return runtime
}

fun Application.configureHttpTracing(tracing: Tracing) {
    intercept(ApplicationCallPipeline.Call) {
        val parentContext =
            TracePropagation.extractFromMap(
                call.request.headers.names().associateWith { name -> call.request.headers[name] },
            )
        val span = tracing.startSpan("http.request", parentContext)
        try {
            proceed()
        } finally {
            tracing.endSpan(span)
        }
    }
}
