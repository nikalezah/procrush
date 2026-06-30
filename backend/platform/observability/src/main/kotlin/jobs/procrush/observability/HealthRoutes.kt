package jobs.procrush.observability

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import jobs.procrush.bootstrap.config.ObservabilityConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit

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
    val redis: String? = null,
    val rabbitmq: String? = null,
    val kafka: String? = null,
    val matching: String? = null,
    val postgres: String? = null,
    val kafka_consumer: String? = null,
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

object KafkaHealth {
    fun check(bootstrapServers: String): HealthCheckResult {
        val started = System.nanoTime()
        return runCatching {
            val props =
                Properties().apply {
                    put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                    put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000")
                    put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "3000")
                }
            AdminClient.create(props).use { admin ->
                admin.listTopics().names().get(3, TimeUnit.SECONDS)
            }
            ok("kafka", started)
        }.getOrElse {
            down("kafka", started)
        }
    }
}

private val healthJson =
    Json {
        encodeDefaults = false
        explicitNulls = false
    }

fun Application.configureHealthRoutes(
    config: ObservabilityConfig,
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
                    redis = report.checks.find { it.name == "redis" }?.status,
                    rabbitmq = report.checks.find { it.name == "rabbitmq" }?.status,
                    kafka = report.checks.find { it.name == "kafka" }?.status,
                    matching = report.checks.find { it.name == "matching" }?.status,
                    postgres = report.checks.find { it.name == "postgres" }?.status,
                    kafka_consumer = report.checks.find { it.name == "kafka_consumer" }?.status,
                    consumer = report.checks.find { it.name == "consumer" }?.status,
                )
            call.respondText(
                healthJson.encodeToString(legacy),
                ContentType.Application.Json,
                report.httpStatus(),
            )
        }
    }
}

private fun buildHealthReport(
    config: ObservabilityConfig,
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
