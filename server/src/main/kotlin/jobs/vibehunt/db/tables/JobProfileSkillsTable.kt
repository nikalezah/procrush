package jobs.procrush.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object JobProfileSkillsTable : Table("job_profile_skills") {
    val jobProfileId = reference("job_profile_id", EmployerJobProfilesTable, onDelete = ReferenceOption.CASCADE)
    val skillId = reference("skill_id", SkillsTable, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(jobProfileId, skillId)
}
