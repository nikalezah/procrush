package jobs.procrush.config

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class DatabaseConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
) {
    companion object {
        fun resolve(
            databaseUrl: String?,
            databaseUser: String?,
            databasePassword: String?,
            defaults: DatabaseConfig = localDefaults(),
        ): DatabaseConfig {
            val rawUrl = databaseUrl?.takeIf { it.isNotBlank() } ?: defaults.jdbcUrl
            if (!isPostgresUri(rawUrl)) {
                return DatabaseConfig(
                    jdbcUrl = rawUrl,
                    user = databaseUser?.takeIf { it.isNotBlank() } ?: defaults.user,
                    password = databasePassword?.takeIf { it.isNotBlank() } ?: defaults.password,
                )
            }
            val parsed = parsePostgresUri(rawUrl)
            return DatabaseConfig(
                jdbcUrl = parsed.jdbcUrl,
                user = databaseUser?.takeIf { it.isNotBlank() } ?: parsed.user,
                password = databasePassword?.takeIf { it.isNotBlank() } ?: parsed.password,
            )
        }

        private fun localDefaults(): DatabaseConfig =
            DatabaseConfig(
                jdbcUrl = "jdbc:postgresql://localhost:5432/procrush",
                user = "procrush",
                password = "procrush",
            )

        private fun isPostgresUri(url: String): Boolean =
            url.startsWith("postgres://", ignoreCase = true) ||
                url.startsWith("postgresql://", ignoreCase = true)

        private fun parsePostgresUri(uriString: String): DatabaseConfig {
            val normalized =
                if (uriString.startsWith("postgres://", ignoreCase = true)) {
                    "postgresql://" + uriString.substring("postgres://".length)
                } else {
                    uriString
                }
            val uri = URI(normalized)
            val userInfo = uri.userInfo?.split(":", limit = 2) ?: emptyList()
            val user =
                userInfo.getOrNull(0)?.let { decode(it) }
                    ?: throw IllegalArgumentException("DATABASE_URL must include a username")
            val password =
                userInfo.getOrNull(1)?.let { decode(it) } ?: ""
            val host = uri.host ?: throw IllegalArgumentException("DATABASE_URL must include a host")
            val port = if (uri.port > 0) uri.port else 5432
            val path = uri.path.removePrefix("/").ifBlank { "postgres" }
            val query = uri.rawQuery?.takeIf { it.isNotBlank() }
            val jdbcBase = "jdbc:postgresql://$host:$port/$path"
            val jdbcUrl =
                when {
                    query == null -> "$jdbcBase?sslmode=require"
                    query.contains("sslmode=", ignoreCase = true) -> "$jdbcBase?$query"
                    else -> "$jdbcBase?$query&sslmode=require"
                }
            return DatabaseConfig(jdbcUrl = jdbcUrl, user = user, password = password)
        }

        private fun decode(value: String): String =
            URLDecoder.decode(value, StandardCharsets.UTF_8)
    }
}
