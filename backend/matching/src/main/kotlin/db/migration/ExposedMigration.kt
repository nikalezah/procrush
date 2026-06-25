package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

abstract class ExposedMigration : BaseJavaMigration() {
    protected fun exposedTransaction(block: JdbcTransaction.() -> Unit) {
        transaction { block() }
    }
}
