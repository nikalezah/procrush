package jobs.procrush.personality.observability

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration

@Serializable
data class HealthCheckResultDto(
    val name: String,
    val status: String,
    val latencyMs: Long,
)

@Serializable
data class LiveHealthResponse(
    val status: String,
    val version: String,
    val uptimeSeconds: Long,
)

@Serializable
data class ReadyHealthResponse(
    val status: String,
    val version: String,
    val uptimeSeconds: Long,
    val checks: List<HealthCheckResultDto>,
)

@Serializable
data class LegacyHealthResponse(
    val status: String,
    val rabbitmq: String? = null,
    val consumer: String? = null,
)

data class HealthCheckResult(
    val name: String,
    val status: String,
    val latencyMs: Long,
)

data class HealthReport(
    val status: String,
    val version: String,
    val uptimeSeconds: Long,
    val checks: List<HealthCheckResult>,
) {
    fun httpStatus(): HttpStatusCode =
        if (status == "ok") {
            HttpStatusCode.OK
        } else {
            HttpStatusCode.ServiceUnavailable
        }
}

fun interface HealthCheck {
    fun run(): HealthCheckResult
}

private val healthJson =
    Json {
        encodeDefaults = false
        explicitNulls = false
    }

fun Application.configureHealthRoutes(
    config: WorkerLogConfig,
    readinessChecks: List<HealthCheck>,
) {
    val startedAt = System.currentTimeMillis()

    routing {
        get("/health/live") {
            val body =
                LiveHealthResponse(
                    status = "ok",
                    version = config.appVersion,
                    uptimeSeconds = uptimeSeconds(startedAt),
                )
            call.respondText(
                healthJson.encodeToString(body),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }
        get("/health/ready") {
            val report = buildHealthReport(config, startedAt, readinessChecks)
            val body =
                ReadyHealthResponse(
                    status = report.status,
                    version = report.version,
                    uptimeSeconds = report.uptimeSeconds,
                    checks =
                        report.checks.map { check ->
                            HealthCheckResultDto(
                                name = check.name,
                                status = check.status,
                                latencyMs = check.latencyMs,
                            )
                        },
                )
            call.respondText(
                healthJson.encodeToString(body),
                ContentType.Application.Json,
                report.httpStatus(),
            )
        }
        get("/health") {
            val report = buildHealthReport(config, startedAt, readinessChecks)
            val legacy =
                LegacyHealthResponse(
                    status = if (report.status == "ok") "ok" else "degraded",
                    rabbitmq = report.checks.find { it.name == "rabbitmq" }?.status,
                    consumer = report.checks.find { it.name == "consumer" }?.status,
                )
            call.respondText(
                healthJson.encodeToString(legacy),
                ContentType.Application.Json,
                report.httpStatus(),
            )
        }
        get("/metrics") {
            call.respondText(Metrics.scrape(), ContentType.Text.Plain)
        }
    }
}

private fun buildHealthReport(
    config: WorkerLogConfig,
    startedAt: Long,
    readinessChecks: List<HealthCheck>,
): HealthReport {
    val checks = readinessChecks.map { it.run() }
    val healthy = checks.all { it.status == "ok" }
    return HealthReport(
        status = if (healthy) "ok" else "degraded",
        version = config.appVersion,
        uptimeSeconds = uptimeSeconds(startedAt),
        checks = checks,
    )
}

private fun uptimeSeconds(startedAt: Long): Long = (System.currentTimeMillis() - startedAt) / 1000

fun ok(
    name: String,
    startedNanos: Long,
): HealthCheckResult =
    HealthCheckResult(
        name = name,
        status = "ok",
        latencyMs = elapsedMs(startedNanos),
    )

fun down(
    name: String,
    startedNanos: Long,
): HealthCheckResult =
    HealthCheckResult(
        name = name,
        status = "down",
        latencyMs = elapsedMs(startedNanos),
    )

fun simpleCheck(
    name: String,
    probe: () -> Boolean,
): HealthCheck =
    HealthCheck {
        val started = System.nanoTime()
        if (probe()) ok(name, started) else down(name, started)
    }

private fun elapsedMs(startedNanos: Long): Long = Duration.ofNanos(System.nanoTime() - startedNanos).toMillis()
