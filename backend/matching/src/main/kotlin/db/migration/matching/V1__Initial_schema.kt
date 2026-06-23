package db.migration.matching

import db.migration.ExposedMigration
import jobs.procrush.matching.runtime.tables.JobProfileSnapshotsTable
import jobs.procrush.matching.runtime.tables.MatchResultsTable
import jobs.procrush.matching.runtime.tables.SeekerSnapshotsTable
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class V1__Initial_schema : ExposedMigration() {
    override fun migrate(context: Context) {
        exposedTransaction {
            SchemaUtils.create(
                MatchResultsTable,
                SeekerSnapshotsTable,
                JobProfileSnapshotsTable,
            )
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_match_results_seeker
                ON match_results (seeker_id, match_score DESC);
                """.trimIndent(),
            )
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_match_results_job
                ON match_results (job_profile_id, match_score DESC);
                """.trimIndent(),
            )
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_seeker_snapshots_matching_eligible
                ON seeker_snapshots (matching_eligible);
                """.trimIndent(),
            )
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_job_profile_snapshots_occupation
                ON job_profile_snapshots (occupation_id);
                """.trimIndent(),
            )
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_job_profile_snapshots_active
                ON job_profile_snapshots (is_active);
                """.trimIndent(),
            )
            execSqlResource("db/seed/test_seed.sql")
        }
    }
}
