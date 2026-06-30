package jobs.procrush.auth.repository

import jobs.procrush.auth.service.SessionTokenHasher
import jobs.procrush.bootstrap.config.RedisConfig
import jobs.procrush.bootstrap.redis.RedisClient
import jobs.procrush.observability.AppMetrics
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

class CachingSessionRepository(
    private val delegate: SessionRepository,
    private val redis: RedisClient,
    private val config: RedisConfig,
) : SessionStore {
    private val logger = LoggerFactory.getLogger(CachingSessionRepository::class.java)

    override fun create(userId: UUID, rawToken: String, expiresAt: OffsetDateTime) {
        delegate.create(userId, rawToken, expiresAt)
        cacheToken(rawToken, userId, expiresAt)
    }

    override fun findUserIdByToken(rawToken: String): UUID? {
        val hash = SessionTokenHasher.hash(rawToken)
        val cacheKey = sessionKey(hash)
        redis.get(cacheKey)?.let { cached ->
            AppMetrics.redisCacheHit("session")
            logger.debug("session cache HIT")
            return runCatching { UUID.fromString(cached) }.getOrNull()
        }
        AppMetrics.redisCacheMiss("session")
        logger.debug("session cache MISS")
        val userId = delegate.findUserIdByToken(rawToken) ?: return null
        val expiresAt = delegate.findExpiresAtByTokenHash(hash)
        if (expiresAt != null) {
            cacheUserId(cacheKey, userId, expiresAt)
        }
        return userId
    }

    override fun deleteByToken(rawToken: String) {
        val hash = SessionTokenHasher.hash(rawToken)
        delegate.deleteByToken(rawToken)
        redis.del(sessionKey(hash))
    }

    override fun purgeExpired() {
        delegate.purgeExpired()
    }

    private fun cacheToken(rawToken: String, userId: UUID, expiresAt: OffsetDateTime) {
        cacheUserId(sessionKey(SessionTokenHasher.hash(rawToken)), userId, expiresAt)
    }

    private fun cacheUserId(cacheKey: String, userId: UUID, expiresAt: OffsetDateTime) {
        val ttlSeconds = expiresAt.toEpochSecond() - OffsetDateTime.now().toEpochSecond()
        if (ttlSeconds <= 0) return
        redis.setEx(cacheKey, ttlSeconds, userId.toString())
    }

    private fun sessionKey(tokenHash: String): String = config.key("session", tokenHash)
}
