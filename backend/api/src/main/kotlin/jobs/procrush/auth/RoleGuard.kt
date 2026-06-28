package jobs.procrush.auth.service

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import jobs.procrush.auth.UserRole
import jobs.procrush.bootstrap.config.AppConfig
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
            call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Не авторизован"))
            return null
        }
        val user = sessionService.resolveUser(token) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Не авторизован"))
            return null
        }
        val role = user.role ?: run {
            call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Роль не выбрана"))
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
            call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Доступ запрещён"))
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
            return AuthProblem(unauthorized = true, message = "Не авторизован")
        }
        val user = sessionService.resolveUser(token)!!
        if (user.role == null) {
            return AuthProblem(unauthorized = false, message = "Роль не выбрана")
        }
        if (requiredRole != null && user.role != requiredRole) {
            return AuthProblem(unauthorized = false, message = "Доступ запрещён")
        }
        return AuthProblem(unauthorized = false, message = "Доступ запрещён")
    }
}

data class AuthProblem(
    val unauthorized: Boolean,
    val message: String,
)
