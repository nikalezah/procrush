package jobs.procrush.bootstrap

import jobs.procrush.bootstrap.config.AppConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun init(config: AppConfig, runMigrations: Boolean = true) {
        Database.connect(
            url = config.databaseUrl,
            driver = "org.postgresql.Driver",
            user = config.databaseUser,
            password = config.databasePassword,
        )
        if (runMigrations) {
            Flyway.configure()
                .dataSource(
                    config.databaseUrl,
                    config.databaseUser,
                    config.databasePassword,
                )
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }
        transaction {
            // warm up connection
        }
    }
}
