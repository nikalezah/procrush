package db.migration

import jobs.procrush.auth.UserRole
import jobs.procrush.auth.tables.SessionsTable
import jobs.procrush.auth.tables.UsersTable
import jobs.procrush.employer.tables.EmployerJobProfilesTable
import jobs.procrush.employer.tables.EmployersTable
import jobs.procrush.employer.tables.JobProfileSkillsTable
import jobs.procrush.matching.tables.JobMatchInterestsTable
import jobs.procrush.seeker.tables.SeekerDesiredPositionsTable
import jobs.procrush.seeker.tables.SeekerEducationTable
import jobs.procrush.seeker.tables.SeekerExperienceTable
import jobs.procrush.seeker.tables.SeekerPersonalProfilesTable
import jobs.procrush.seeker.tables.SeekerSkillsTable
import jobs.procrush.seeker.tables.SeekerSuperpowersAndTalentsTable
import jobs.procrush.seeker.tables.SeekersTable
import jobs.procrush.shared.tables.GlossaryTermsTable
import jobs.procrush.shared.tables.OccupationsTable
import jobs.procrush.shared.tables.SkillsTable
import jobs.procrush.shared.tables.SuperpowersAndTalentsTable
import jobs.procrush.survey.tables.SurveyKeysTable
import jobs.procrush.survey.tables.SurveyResultsTable
import jobs.procrush.survey.tables.SurveysTable
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
                JobMatchInterestsTable,
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
            execSqlResource("db/seed/init_inserts.sql")
        }
    }
}
