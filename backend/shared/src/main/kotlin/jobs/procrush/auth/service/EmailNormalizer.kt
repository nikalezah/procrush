package jobs.procrush.auth.service

object EmailNormalizer {
    fun normalize(email: String): String {
        val normalized = email.trim().lowercase()
        require(normalized.contains('@')) { "Некорректный адрес электронной почты" }
        return normalized
    }
}
