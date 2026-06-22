package jobs.procrush.auth.repository

import java.time.OffsetDateTime
import java.util.UUID

interface SessionStore {
    fun create(userId: UUID, rawToken: String, expiresAt: OffsetDateTime)

    fun findUserIdByToken(rawToken: String): UUID?

    fun deleteByToken(rawToken: String)

    fun purgeExpired()
}
