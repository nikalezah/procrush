package db.migration.matching

import db.migration.ExposedMigration
import org.flywaydb.core.api.migration.Context

class V2__Strip_display_fields : ExposedMigration() {
    override fun migrate(context: Context) {
        exposedTransaction {
            exec(
                """
                ALTER TABLE seeker_snapshots
                    DROP COLUMN IF EXISTS skill_names_json,
                    DROP COLUMN IF EXISTS first_name,
                    DROP COLUMN IF EXISTS last_name
                """.trimIndent(),
            )
            exec(
                """
                ALTER TABLE job_profile_snapshots
                    DROP COLUMN IF EXISTS company_name,
                    DROP COLUMN IF EXISTS occupation_name,
                    DROP COLUMN IF EXISTS description
                """.trimIndent(),
            )
            exec(
                """
                ALTER TABLE match_results
                    DROP COLUMN IF EXISTS occupation_id,
                    DROP COLUMN IF EXISTS company_name,
                    DROP COLUMN IF EXISTS position_name,
                    DROP COLUMN IF EXISTS job_description,
                    DROP COLUMN IF EXISTS seeker_first_name,
                    DROP COLUMN IF EXISTS seeker_last_name,
                    DROP COLUMN IF EXISTS seeker_skills_json
                """.trimIndent(),
            )
        }
    }
}
