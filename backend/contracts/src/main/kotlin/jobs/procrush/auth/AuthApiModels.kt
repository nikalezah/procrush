package jobs.procrush.auth

import kotlinx.serialization.Serializable

@Serializable
data class DevLoginRequest(
    val email: String,
)

@Serializable
data class CompleteRegistrationRequest(
    val email: String? = null,
    val role: UserRole,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val companyName: String? = null,
)

@Serializable
data class MeResponse(
    val user: AuthUserDto?,
)
