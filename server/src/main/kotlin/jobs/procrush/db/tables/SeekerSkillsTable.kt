package jobs.procrush.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SeekerSkillsTable : Table("seeker_skills") {
    val seekerId = reference("seeker_id", SeekersTable, onDelete = ReferenceOption.CASCADE)
    val skillId = reference("skill_id", SkillsTable, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(seekerId, skillId)
}
