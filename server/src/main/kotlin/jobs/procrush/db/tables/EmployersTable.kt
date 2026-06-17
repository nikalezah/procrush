package jobs.procrush.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object EmployersTable : LongIdTable("employers") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val website = varchar("website", 255).nullable()
    val phone = varchar("phone", 30).nullable()
    val emailContact = varchar("email_contact", 150).nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
}
