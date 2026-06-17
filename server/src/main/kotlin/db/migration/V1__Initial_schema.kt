package db.migration

import jobs.procrush.auth.UserRole
import jobs.procrush.db.tables.EmployerJobProfilesTable
import jobs.procrush.db.tables.EmployersTable
import jobs.procrush.db.tables.GlossaryTermsTable
import jobs.procrush.db.tables.JobProfileSkillsTable
import jobs.procrush.db.tables.OccupationsTable
import jobs.procrush.db.tables.SeekerDesiredPositionsTable
import jobs.procrush.db.tables.SeekerEducationTable
import jobs.procrush.db.tables.SeekerExperienceTable
import jobs.procrush.db.tables.SeekerPersonalProfilesTable
import jobs.procrush.db.tables.SeekerSkillsTable
import jobs.procrush.db.tables.SeekerSuperpowersAndTalentsTable
import jobs.procrush.db.tables.SeekersTable
import jobs.procrush.db.tables.SessionsTable
import jobs.procrush.db.tables.SkillsTable
import jobs.procrush.db.tables.SuperpowersAndTalentsTable
import jobs.procrush.db.tables.SurveyKeysTable
import jobs.procrush.db.tables.SurveyResultsTable
import jobs.procrush.db.tables.SurveysTable
import jobs.procrush.db.tables.UsersTable
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class V1__Initial_schema : ExposedMigration() {
    override fun migrate(context: Context) {
        exposedTransaction {
            SchemaUtils.create(
                UsersTable,
                SessionsTable,
                SkillsTable,
                OccupationsTable,
                SeekersTable,
                SeekerExperienceTable,
                SeekerEducationTable,
                SeekerSkillsTable,
                SeekerDesiredPositionsTable,
                SeekerPersonalProfilesTable,
                SuperpowersAndTalentsTable,
                SeekerSuperpowersAndTalentsTable,
                EmployersTable,
                EmployerJobProfilesTable,
                JobProfileSkillsTable,
                SurveysTable,
                SurveyResultsTable,
                SurveyKeysTable,
                GlossaryTermsTable,
            )
            val roles = UserRole.entries.joinToString(", ") { "'${it.name}'" }
            exec(
                """
                ALTER TABLE users ADD CONSTRAINT users_role_check
                CHECK (role IN ($roles));
                """.trimIndent(),
            )
            exec("CREATE INDEX IF NOT EXISTS idx_occupations_parent ON occupations (parent_id)")
            exec(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS idx_survey_results_seeker_survey_completed
                ON survey_results (seeker_id, survey_id)
                WHERE completed_at IS NOT NULL;
                """.trimIndent(),
            )
            execSqlResource("db/seed/init_inserts.sql")
        }
    }
}
