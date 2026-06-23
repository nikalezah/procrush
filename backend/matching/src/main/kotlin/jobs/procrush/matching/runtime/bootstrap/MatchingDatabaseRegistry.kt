package jobs.procrush.matching.runtime.bootstrap

import jobs.procrush.bootstrap.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object MatchingDatabaseRegistry {
    lateinit var matching: Database
        private set

    fun init(
        matchingConfig: DatabaseConfig,
        runMatchingMigrations: Boolean = true,
    ) {
        matching =
            Database.connect(
                url = matchingConfig.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = matchingConfig.user,
                password = matchingConfig.password,
            )
        if (runMatchingMigrations) {
            Flyway.configure()
                .dataSource(
                    matchingConfig.jdbcUrl,
                    matchingConfig.user,
                    matchingConfig.password,
                )
                .locations("classpath:db/migration/matching")
                .load()
                .migrate()
        }
        transaction(matching) { }
    }
}
