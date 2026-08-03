package db.migration

import jobs.procrush.matching.tables.MatchScoresTable
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class V2__Match_scores : ExposedMigration() {
    override fun migrate(context: Context) {
        exposedTransaction {
            SchemaUtils.create(MatchScoresTable)
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_match_scores_seeker
                ON match_scores (seeker_id, match_score DESC)
                """.trimIndent(),
            )
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_match_scores_job
                ON match_scores (job_profile_id, match_score DESC)
                """.trimIndent(),
            )
            execSqlResource("db/seed/match_scores.sql")
        }
    }
}
