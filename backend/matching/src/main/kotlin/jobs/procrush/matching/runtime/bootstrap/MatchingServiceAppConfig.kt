package jobs.procrush.matching.runtime.bootstrap

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.config.DatabaseConfig
import jobs.procrush.bootstrap.config.DotEnv
import jobs.procrush.bootstrap.config.Env

data class MatchingServiceAppConfig(
    val port: Int,
    val matchingDatabase: DatabaseConfig,
    val appConfig: AppConfig,
) {
    val redis get() = appConfig.redis
    val kafka get() = appConfig.kafka

    companion object {
        fun fromEnvironment(): MatchingServiceAppConfig {
            val dotEnv = DotEnv.load()
            val appConfig = AppConfig.fromEnvironment()
            val matchingDatabase =
                DatabaseConfig.resolve(
                    databaseUrl =
                        Env.resolve("MATCHING_DATABASE_URL", dotEnv)
                            ?: "jdbc:postgresql://localhost:5433/procrush_matching",
                    databaseUser = Env.resolve("MATCHING_DATABASE_USER", dotEnv) ?: "procrush",
                    databasePassword = Env.resolve("MATCHING_DATABASE_PASSWORD", dotEnv) ?: "procrush",
                )
            return MatchingServiceAppConfig(
                port = Env.env("MATCHING_SERVICE_PORT", "8092", dotEnv).toIntOrNull() ?: 8092,
                matchingDatabase = matchingDatabase,
                appConfig = appConfig,
            )
        }
    }
}
