package jobs.procrush.api.handler

import io.ktor.server.application.ApplicationCall
import jobs.procrush.api.generated.common_models_yaml.common_models.ErrorResponse
import jobs.procrush.auth.UserRole
import jobs.procrush.auth.service.AuthenticatedUser
import jobs.procrush.auth.service.RoleGuard

fun apiError(message: String, error: String? = null): ErrorResponse =
    ErrorResponse(message = message, error = error)

fun unauthorized(message: String = "Не авторизован"): ErrorResponse = apiError(message)

fun forbidden(message: String = "Доступ запрещён"): ErrorResponse = apiError(message)

fun notFound(message: String = "Не найдено"): ErrorResponse = apiError(message)

fun badRequest(message: String): ErrorResponse = apiError(message)

fun conflict(message: String): ErrorResponse = apiError(message)

fun devAuthDisabled(): ErrorResponse =
    apiError(
        message = "Установите AUTH_DEV_MODE=true в .env, чтобы включить вход для разработки.",
        error = "dev_auth_disabled",
    )

suspend fun RoleGuard.requireSeeker(call: ApplicationCall): AuthenticatedUser? = peekRole(call, UserRole.SEEKER)

suspend fun RoleGuard.requireEmployer(call: ApplicationCall): AuthenticatedUser? = peekRole(call, UserRole.EMPLOYER)

suspend fun RoleGuard.requireAnyAuth(call: ApplicationCall): AuthenticatedUser? = peekAuth(call)

suspend fun RoleGuard.seekerAuthError(call: ApplicationCall): Pair<Boolean, ErrorResponse> {
    val problem = authProblem(call, UserRole.SEEKER)
    return problem.unauthorized to apiError(problem.message)
}

suspend fun RoleGuard.employerAuthError(call: ApplicationCall): Pair<Boolean, ErrorResponse> {
    val problem = authProblem(call, UserRole.EMPLOYER)
    return problem.unauthorized to apiError(problem.message)
}

suspend fun RoleGuard.anyAuthError(call: ApplicationCall): Pair<Boolean, ErrorResponse> {
    val problem = authProblem(call)
    return problem.unauthorized to apiError(problem.message)
}

suspend inline fun <T> RoleGuard.withSeeker(
    call: ApplicationCall,
    onUnauthorized: (ErrorResponse) -> T,
    onForbidden: (ErrorResponse) -> T,
    block: (AuthenticatedUser) -> T,
): T {
    val user = requireSeeker(call) ?: run {
        val (isUnauthorized, error) = seekerAuthError(call)
        return if (isUnauthorized) onUnauthorized(error) else onForbidden(error)
    }
    return block(user)
}

suspend inline fun <T> RoleGuard.withEmployer(
    call: ApplicationCall,
    onUnauthorized: (ErrorResponse) -> T,
    onForbidden: (ErrorResponse) -> T,
    block: (AuthenticatedUser) -> T,
): T {
    val user = requireEmployer(call) ?: run {
        val (isUnauthorized, error) = employerAuthError(call)
        return if (isUnauthorized) onUnauthorized(error) else onForbidden(error)
    }
    return block(user)
}

suspend inline fun <T> RoleGuard.withAuth(
    call: ApplicationCall,
    onUnauthorized: (ErrorResponse) -> T,
    onForbidden: (ErrorResponse) -> T,
    block: (AuthenticatedUser) -> T,
): T {
    val user = requireAnyAuth(call) ?: run {
        val (isUnauthorized, error) = anyAuthError(call)
        return if (isUnauthorized) onUnauthorized(error) else onForbidden(error)
    }
    return block(user)
}
