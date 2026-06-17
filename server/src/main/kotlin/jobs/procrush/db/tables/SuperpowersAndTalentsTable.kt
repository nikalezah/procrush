package jobs.procrush.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SuperpowersAndTalentsTable : LongIdTable("superpowers_and_talents") {
    val name = varchar("name", 255).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
