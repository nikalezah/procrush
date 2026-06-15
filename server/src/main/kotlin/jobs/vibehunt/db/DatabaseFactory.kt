package jobs.procrush.db

import jobs.procrush.config.AppConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: AppConfig) {
        Database.connect(
            url = config.databaseUrl,
            driver = "org.postgresql.Driver",
            user = config.databaseUser,
            password = config.databasePassword,
        )
        Flyway.configure()
            .dataSource(
                config.databaseUrl,
                config.databaseUser,
                config.databasePassword,
            )
            .locations("classpath:db/migration")
            .load()
            .migrate()
        transaction {
            // warm up connection
        }
    }
}
