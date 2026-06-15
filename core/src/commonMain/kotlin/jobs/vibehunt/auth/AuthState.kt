package jobs.procrush.auth

sealed interface AuthState {
    data object Loading : AuthState

    data object Unauthenticated : AuthState

    data class NeedsRegistration(val user: AuthUserDto) : AuthState

    data class Authenticated(val user: AuthUserDto) : AuthState
}
