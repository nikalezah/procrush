package jobs.procrush.seeker.tables

import jobs.procrush.auth.tables.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SeekersTable : LongIdTable("seekers") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val firstName = varchar("first_name", 50)
    val middleName = varchar("middle_name", 50).nullable()
    val lastName = varchar("last_name", 50)
    val phone = varchar("phone", 30).nullable()
    val telegram = varchar("telegram", 100).nullable()
    val linkedin = varchar("linkedin", 255).nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
}
