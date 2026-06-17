package jobs.procrush.db

import jobs.procrush.auth.SessionTokenHasher
import jobs.procrush.db.tables.SessionsTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID

class SessionRepository {
    fun create(userId: UUID, rawToken: String, expiresAt: OffsetDateTime) {
        transaction {
            SessionsTable.insert {
                it[SessionsTable.userId] = userId
                it[SessionsTable.tokenHash] = SessionTokenHasher.hash(rawToken)
                it[SessionsTable.expiresAt] = expiresAt
                it[SessionsTable.createdAt] = OffsetDateTime.now()
            }
        }
    }

    fun findUserIdByToken(rawToken: String): UUID? =
        transaction {
            val now = OffsetDateTime.now()
            SessionsTable
                .selectAll()
                .where {
                    (SessionsTable.tokenHash eq SessionTokenHasher.hash(rawToken)) and
                        (SessionsTable.expiresAt greaterEq now)
                }
                .firstOrNull()
                ?.get(SessionsTable.userId)
                ?.value
        }

    fun deleteByToken(rawToken: String) {
        transaction {
            SessionsTable.deleteWhere {
                SessionsTable.tokenHash eq SessionTokenHasher.hash(rawToken)
            }
        }
    }

    fun purgeExpired() {
        transaction {
            SessionsTable.deleteWhere {
                SessionsTable.expiresAt less OffsetDateTime.now()
            }
        }
    }
}
