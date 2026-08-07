package jobs.procrush.personality.observability

import jobs.procrush.bootstrap.config.DotEnv
import jobs.procrush.bootstrap.config.Env
import jobs.procrush.bootstrap.config.LogFormat

data class WorkerLogConfig(
    val serviceName: String,
    val logFormat: LogFormat,
    val appVersion: String,
    val environment: String,
) {
    companion object {
        fun fromEnvironment(
            defaultServiceName: String = "personality",
            dotEnv: Map<String, String> = DotEnv.load(),
        ): WorkerLogConfig {
            val logFormatRaw = Env.env("LOG_FORMAT", "text", dotEnv)
            val logFormat =
                when (logFormatRaw.lowercase()) {
                    "json" -> LogFormat.JSON
                    else -> LogFormat.TEXT
                }
            return WorkerLogConfig(
                serviceName = Env.env("SERVICE_NAME", defaultServiceName, dotEnv),
                logFormat = logFormat,
                appVersion = Env.env("APP_VERSION", Env.env("GIT_SHA", "dev", dotEnv), dotEnv),
                environment = Env.env("ENVIRONMENT", "local", dotEnv),
            )
        }
    }
}
