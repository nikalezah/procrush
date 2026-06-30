package jobs.procrush.observability

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import jobs.procrush.bootstrap.config.ObservabilityConfig

fun Application.configureMetrics(config: ObservabilityConfig): PrometheusMeterRegistry {
    val registry = AppMetrics.initialize(config)
    install(MicrometerMetrics) {
        this.registry = registry
    }
    routing {
        get("/metrics") {
            call.respondText(registry.scrape())
        }
    }
    return registry
}
