package jobs.procrush.bootstrap

import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.bootstrap.config.WorkerAppConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun init(config: AppConfig, runMigrations: Boolean = true) {
        init(
            databaseUrl = config.databaseUrl,
            databaseUser = config.databaseUser,
            databasePassword = config.databasePassword,
            runMigrations = runMigrations,
        )
    }

    fun init(config: WorkerAppConfig, runMigrations: Boolean = true) {
        init(
            databaseUrl = config.databaseUrl,
            databaseUser = config.databaseUser,
            databasePassword = config.databasePassword,
            runMigrations = runMigrations,
        )
    }

    private fun init(
        databaseUrl: String,
        databaseUser: String,
        databasePassword: String,
        runMigrations: Boolean,
    ) {
        Database.connect(
            url = databaseUrl,
            driver = "org.postgresql.Driver",
            user = databaseUser,
            password = databasePassword,
        )
        if (runMigrations) {
            Flyway.configure()
                .dataSource(databaseUrl, databaseUser, databasePassword)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }
        transaction {
            // warm up connection
        }
    }
}
