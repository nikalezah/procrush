package jobs.procrush.auth.service

import jobs.procrush.i18n.ErrorCode
import jobs.procrush.shared.raise

object EmailNormalizer {
    fun normalize(email: String): String {
        val normalized = email.trim().lowercase()
        if (!normalized.contains('@')) {
            ErrorCode.INVALID_EMAIL.raise()
        }
        return normalized
    }
}
