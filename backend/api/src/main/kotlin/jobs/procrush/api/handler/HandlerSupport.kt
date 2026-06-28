package jobs.procrush.api.handler

import io.ktor.server.application.ApplicationCall
import jobs.procrush.api.generated.common_models_yaml.common_models.ErrorResponse
import jobs.procrush.auth.AuthenticatedUser
import jobs.procrush.auth.RoleGuard
import jobs.procrush.auth.UserRole
import jobs.procrush.i18n.ErrorCode

fun apiError(code: ErrorCode, details: Map<String, String> = emptyMap()): ErrorResponse =
    ErrorResponse(
        code = code.name,
        message = code.formatMessage(details),
        details = details.takeIf { it.isNotEmpty() },
    )

fun errorUnauthorized(): ErrorResponse = apiError(ErrorCode.UNAUTHORIZED)

fun errorForbidden(): ErrorResponse = apiError(ErrorCode.FORBIDDEN)

fun errorNotFound(): ErrorResponse = apiError(ErrorCode.NOT_FOUND)

fun errorBadRequest(code: ErrorCode = ErrorCode.INVALID_REQUEST, details: Map<String, String> = emptyMap()): ErrorResponse =
    apiError(code, details)

fun errorConflict(code: ErrorCode, details: Map<String, String> = emptyMap()): ErrorResponse = apiError(code, details)

fun errorDevAuthDisabled(): ErrorResponse = apiError(ErrorCode.DEV_AUTH_DISABLED)

suspend fun RoleGuard.requireSeeker(call: ApplicationCall): AuthenticatedUser? = peekRole(call, UserRole.SEEKER)

suspend fun RoleGuard.requireEmployer(call: ApplicationCall): AuthenticatedUser? = peekRole(call, UserRole.EMPLOYER)

suspend fun RoleGuard.requireAnyAuth(call: ApplicationCall): AuthenticatedUser? = peekAuth(call)

suspend fun RoleGuard.seekerAuthError(call: ApplicationCall): Pair<Boolean, ErrorResponse> {
    val problem = authProblem(call, UserRole.SEEKER)
    return problem.unauthorized to apiError(problem.errorCode)
}

suspend fun RoleGuard.employerAuthError(call: ApplicationCall): Pair<Boolean, ErrorResponse> {
    val problem = authProblem(call, UserRole.EMPLOYER)
    return problem.unauthorized to apiError(problem.errorCode)
}

suspend fun RoleGuard.anyAuthError(call: ApplicationCall): Pair<Boolean, ErrorResponse> {
    val problem = authProblem(call)
    return problem.unauthorized to apiError(problem.errorCode)
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
