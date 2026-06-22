package jobs.procrush.auth

actual fun createAuthRepository(): AuthRepository = UnsupportedAuthRepository()

private class UnsupportedAuthRepository : AuthRepository {
    override suspend fun fetchMe(): AuthUserDto? = null

    override suspend fun devLogin(email: String): AuthUserDto =
        throw UnsupportedOperationException("Auth is web-only; native clients are not supported")

    override suspend fun logout() {}

    override suspend fun completeRegistration(email: String, role: UserRole): AuthUserDto =
        throw UnsupportedOperationException("Auth is web-only; native clients are not supported")
}
