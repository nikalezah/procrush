package jobs.procrush.employer.tables

import jobs.procrush.shared.tables.SkillsTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object JobProfileSkillsTable : Table("job_profile_skills") {
    val jobProfileId = reference("job_profile_id", EmployerJobProfilesTable, onDelete = ReferenceOption.CASCADE)
    val skillId = reference("skill_id", SkillsTable, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(jobProfileId, skillId)
}
