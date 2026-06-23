package jobs.procrush.matching.runtime.bootstrap

import jobs.procrush.bootstrap.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object MatchingDatabaseRegistry {
    lateinit var main: Database
        private set
    lateinit var matching: Database
        private set

    fun init(
        mainConfig: DatabaseConfig,
        matchingConfig: DatabaseConfig,
        runMatchingMigrations: Boolean = true,
    ) {
        // Main DB is read by MatchingRepository; matching DB stores computed results.
        main =
            Database.connect(
                url = mainConfig.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = mainConfig.user,
                password = mainConfig.password,
            )
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
        transaction(main) { }
    }
}
