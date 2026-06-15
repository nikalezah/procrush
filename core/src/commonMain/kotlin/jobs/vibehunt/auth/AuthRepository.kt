package jobs.procrush.auth

interface AuthRepository {
    suspend fun fetchMe(): AuthUserDto?

    suspend fun devLogin(email: String): AuthUserDto

    suspend fun logout()

    suspend fun completeRegistration(email: String, role: UserRole): AuthUserDto
}
