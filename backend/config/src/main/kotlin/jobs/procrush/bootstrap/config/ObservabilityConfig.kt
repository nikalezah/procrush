package jobs.procrush.bootstrap.config

enum class LogFormat {
    TEXT,
    JSON,
}

data class ObservabilityConfig(
    val serviceName: String,
    val logFormat: LogFormat,
    val otelEnabled: Boolean,
    val otelExporterEndpoint: String,
    val appVersion: String,
    val environment: String,
) {
    companion object {
        fun fromEnvironment(
            defaultServiceName: String,
            dotEnv: Map<String, String> = DotEnv.load(),
        ): ObservabilityConfig {
            val logFormatRaw = Env.env("LOG_FORMAT", "text", dotEnv)
            val logFormat =
                when (logFormatRaw.lowercase()) {
                    "json" -> LogFormat.JSON
                    else -> LogFormat.TEXT
                }
            return ObservabilityConfig(
                serviceName = Env.env("SERVICE_NAME", defaultServiceName, dotEnv),
                logFormat = logFormat,
                otelEnabled = Env.env("OTEL_ENABLED", "false", dotEnv).equals("true", ignoreCase = true),
                otelExporterEndpoint = Env.env("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317", dotEnv),
                appVersion = Env.env("APP_VERSION", Env.env("GIT_SHA", "dev", dotEnv), dotEnv),
                environment = Env.env("ENVIRONMENT", "local", dotEnv),
            )
        }
    }
}
