package jobs.procrush.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object SeekerEducationTable : LongIdTable("seeker_education") {
    val seekerId = reference("seeker_id", SeekersTable, onDelete = ReferenceOption.CASCADE)
    val institution = varchar("institution", 150)
    val degree = varchar("degree", 100).nullable()
    val specialization = varchar("specialization", 150)
    val endYear = integer("end_year")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
