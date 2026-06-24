package jobs.procrush.shared.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object OccupationsTable : LongIdTable("occupations") {
    val parentId = long("parent_id").nullable()
    val name = varchar("name", 150)
    val isLeaf = bool("is_leaf")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
