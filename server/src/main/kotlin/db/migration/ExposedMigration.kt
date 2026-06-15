package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

abstract class ExposedMigration : BaseJavaMigration() {
    protected fun exposedTransaction(block: Transaction.() -> Unit) {
        transaction { block() }
    }
}
