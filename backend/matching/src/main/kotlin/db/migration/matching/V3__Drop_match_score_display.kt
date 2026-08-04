package db.migration.matching

import db.migration.ExposedMigration
import org.flywaydb.core.api.migration.Context

class V3__Drop_match_score_display : ExposedMigration() {
    override fun migrate(context: Context) {
        exposedTransaction {
            exec("DELETE FROM match_results WHERE match_score <= 0")
            exec("ALTER TABLE match_results DROP COLUMN IF EXISTS match_score_display")
        }
    }
}
