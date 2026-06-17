package jobs.procrush.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object EmployerJobProfilesTable : LongIdTable("employer_job_profiles") {
    val employerId = reference("employer_id", EmployersTable, onDelete = ReferenceOption.CASCADE)
    val occupationId = reference("occupation_id", OccupationsTable, onDelete = ReferenceOption.RESTRICT)
    val description = text("description").nullable()
    val requiredPersonality = text("required_personality").nullable()
    val isActive = bool("is_active")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
