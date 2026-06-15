package jobs.procrush.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object SurveysTable : LongIdTable("surveys") {
    val code = varchar("code", 50)
    val name = varchar("name", 255)
    val description = text("description")
    val groupCode = varchar("group_code", 50)
    val sortOrder = integer("sort_order")
    val questions = text("questions")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
