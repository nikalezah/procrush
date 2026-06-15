package jobs.procrush.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val role = varchar("role", 20)
    val oauthProvider = varchar("oauth_provider", 20)
    val oauthSubject = varchar("oauth_subject", 255)
    val createdAt = timestampWithTimeZone("created_at")

    init {
        uniqueIndex(oauthProvider, oauthSubject)
    }
}
