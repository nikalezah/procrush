package jobs.procrush.auth

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    SEEKER,
    EMPLOYER,
}
