package jobs.procrush.seeker.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SeekerExperienceTable : LongIdTable("seeker_experience") {
    val seekerId = reference("seeker_id", SeekersTable, onDelete = ReferenceOption.CASCADE)
    val companyName = varchar("company_name", 150)
    val position = varchar("position", 100)
    val description = text("description").nullable()
    val startDate = date("start_date")
    val endDate = date("end_date").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
