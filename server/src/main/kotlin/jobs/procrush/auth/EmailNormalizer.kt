package jobs.procrush.auth

object EmailNormalizer {
    fun normalize(email: String): String {
        val normalized = email.trim().lowercase()
        require(normalized.contains('@')) { "Некорректный адрес электронной почты" }
        return normalized
    }
}
