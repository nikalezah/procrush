package jobs.procrush.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import jobs.procrush.auth.service.SessionService
import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.shared.toResponseBody
import java.util.UUID

data class AuthenticatedUser(
    val id: UUID,
    val email: String,
    val role: UserRole,
)

class RoleGuard(
    private val config: AppConfig,
    private val sessionService: SessionService,
) {
    suspend fun requireAuth(call: ApplicationCall): AuthenticatedUser? {
        val token = call.request.cookies[config.sessionCookieName] ?: run {
            respondError(call, ErrorCode.UNAUTHORIZED)
            return null
        }
        val user = sessionService.resolveUser(token) ?: run {
            respondError(call, ErrorCode.UNAUTHORIZED)
            return null
        }
        val role = user.role ?: run {
            respondError(call, ErrorCode.ROLE_NOT_SELECTED)
            return null
        }
        return AuthenticatedUser(
            id = UUID.fromString(user.id),
            email = user.email,
            role = role,
        )
    }

    suspend fun requireRole(call: ApplicationCall, requiredRole: UserRole): AuthenticatedUser? {
        val authUser = requireAuth(call) ?: return null
        if (authUser.role != requiredRole) {
            respondError(call, ErrorCode.FORBIDDEN)
            return null
        }
        return authUser
    }

    suspend fun peekAuth(call: ApplicationCall): AuthenticatedUser? {
        val token = call.request.cookies[config.sessionCookieName] ?: return null
        val user = sessionService.resolveUser(token) ?: return null
        val role = user.role ?: return null
        return AuthenticatedUser(
            id = UUID.fromString(user.id),
            email = user.email,
            role = role,
        )
    }

    suspend fun peekRole(call: ApplicationCall, requiredRole: UserRole): AuthenticatedUser? {
        val authUser = peekAuth(call) ?: return null
        if (authUser.role != requiredRole) return null
        return authUser
    }

    suspend fun authProblem(call: ApplicationCall, requiredRole: UserRole? = null): AuthProblem {
        val token = call.request.cookies[config.sessionCookieName]
        if (token == null || sessionService.resolveUser(token) == null) {
            return AuthProblem(unauthorized = true, errorCode = ErrorCode.UNAUTHORIZED)
        }
        val user = sessionService.resolveUser(token)!!
        if (user.role == null) {
            return AuthProblem(unauthorized = false, errorCode = ErrorCode.ROLE_NOT_SELECTED)
        }
        if (requiredRole != null && user.role != requiredRole) {
            return AuthProblem(unauthorized = false, errorCode = ErrorCode.FORBIDDEN)
        }
        return AuthProblem(unauthorized = false, errorCode = ErrorCode.FORBIDDEN)
    }

    private suspend fun respondError(call: ApplicationCall, code: ErrorCode) {
        call.respond(HttpStatusCode.fromValue(code.httpStatus), code.toResponseBody())
    }
}

data class AuthProblem(
    val unauthorized: Boolean,
    val errorCode: ErrorCode,
)
