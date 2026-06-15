package jobs.procrush.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object GlossaryTermsTable : LongIdTable("glossary_terms") {
    val term = varchar("term", 150)
    val definition = text("definition")
    val description = text("description")
    val createdAt = timestampWithTimeZone("created_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()
}
