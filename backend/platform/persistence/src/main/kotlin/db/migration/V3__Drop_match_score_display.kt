package db.migration

import org.flywaydb.core.api.migration.Context

class V3__Drop_match_score_display : ExposedMigration() {
    override fun migrate(context: Context) {
        exposedTransaction {
            exec("DELETE FROM match_scores WHERE match_score <= 0")
            exec("ALTER TABLE match_scores DROP COLUMN IF EXISTS match_score_display")
        }
    }
}
