package jobs.procrush.auth

import jobs.procrush.config.AppConfig
import jobs.procrush.db.SessionRepository
import jobs.procrush.db.UserRepository
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

class SessionService(
    private val config: AppConfig,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val enrichUser: (AuthUserDto) -> AuthUserDto = { it },
) {
    private val secureRandom = SecureRandom()

    fun createSession(userId: UUID): String {
        val token = generateToken()
        val expiresAt = OffsetDateTime.now().plusDays(config.sessionDays)
        sessionRepository.create(userId, token, expiresAt)
        return token
    }

    fun resolveUser(token: String?) =
        token?.let { raw ->
            sessionRepository.findUserIdByToken(raw)?.let { userId ->
                userRepository.findById(userId)?.let(enrichUser)
            }
        }

    fun invalidate(token: String?) {
        if (token != null) {
            sessionRepository.deleteByToken(token)
        }
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
