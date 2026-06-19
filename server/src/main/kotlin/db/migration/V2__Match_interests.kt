package db.migration

import jobs.procrush.matching.tables.JobMatchInterestsTable
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class V2__Match_interests : ExposedMigration() {
    override fun migrate(context: Context) {
        exposedTransaction {
            SchemaUtils.create(JobMatchInterestsTable)
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_job_match_interests_seeker
                ON job_match_interests (seeker_id);
                """.trimIndent(),
            )
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_job_match_interests_job_profile
                ON job_match_interests (job_profile_id);
                """.trimIndent(),
            )
        }
    }
}
