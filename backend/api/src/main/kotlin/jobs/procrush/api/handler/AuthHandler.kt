package jobs.procrush.api.handler

import io.ktor.server.application.ApplicationCall
import jobs.procrush.api.generated.auth_models_yaml.auth_models.CompleteRegistrationRequest
import jobs.procrush.api.generated.auth_models_yaml.auth_models.DevLoginRequest
import jobs.procrush.api.generated.auth_paths_yaml.auth_paths.AuthServerApi
import jobs.procrush.api.mapper.toApi
import jobs.procrush.api.mapper.toContract
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.auth.service.SessionService
import jobs.procrush.auth.service.UserAuthService
import jobs.procrush.auth.service.clearSessionCookie
import jobs.procrush.auth.service.setSessionCookie
import jobs.procrush.bootstrap.config.AppConfig
import jobs.procrush.i18n.ErrorCode
import jobs.procrush.shared.CodedException
import jobs.procrush.shared.RegistrationConflictException
import java.util.UUID

class AuthHandler(
    private val config: AppConfig,
    private val userAuthService: UserAuthService,
    private val sessionService: SessionService,
    private val roleGuard: RoleGuard,
) : AuthServerApi {
    override suspend fun devLogin(
        request: DevLoginRequest,
        call: ApplicationCall,
    ): AuthServerApi.DevLoginResponse {
        if (!config.authDevMode) {
            return AuthServerApi.DevLoginResponse.notFound(errorDevAuthDisabled())
        }
        val user =
            userAuthService.findDevUser(request.email)
                ?: return AuthServerApi.DevLoginResponse.ok(userAuthService.pendingDevRegistration(request.email).toApi())
        val sessionToken = sessionService.createSession(UUID.fromString(user.id))
        call.setSessionCookie(config, sessionToken)
        return AuthServerApi.DevLoginResponse.ok(userAuthService.enrich(user).toApi())
    }

    override suspend fun getMe(call: ApplicationCall): AuthServerApi.GetMeResponse {
        val token = call.request.cookies[config.sessionCookieName]
        val user = sessionService.resolveUser(token)
        return AuthServerApi.GetMeResponse.ok(
            jobs.procrush.auth.MeResponse(user = user).toApi(),
        )
    }

    override suspend fun logout(call: ApplicationCall): AuthServerApi.LogoutResponse {
        val token = call.request.cookies[config.sessionCookieName]
        sessionService.invalidate(token)
        call.clearSessionCookie(config)
        return AuthServerApi.LogoutResponse.noContent()
    }

    override suspend fun completeRegistration(
        request: CompleteRegistrationRequest,
        call: ApplicationCall,
    ): AuthServerApi.CompleteRegistrationResponse {
        return try {
            val token = call.request.cookies[config.sessionCookieName]
            val email =
                request.email?.trim()?.lowercase()
                    ?: sessionService.resolveUser(token)?.email
                    ?: return AuthServerApi.CompleteRegistrationResponse.badRequest(errorBadRequest(ErrorCode.EMAIL_REQUIRED))
            val updated = userAuthService.completeRegistration(email, request.toContract())
            val sessionToken = sessionService.createSession(UUID.fromString(updated.id))
            call.setSessionCookie(config, sessionToken)
            AuthServerApi.CompleteRegistrationResponse.ok(userAuthService.enrich(updated).toApi())
        } catch (e: CodedException) {
            AuthServerApi.CompleteRegistrationResponse.badRequest(errorBadRequest(e.errorCode, e.details))
        } catch (_: RegistrationConflictException) {
            AuthServerApi.CompleteRegistrationResponse.conflict(errorConflict(ErrorCode.REGISTRATION_CONFLICT))
        }
    }

    override suspend fun deleteAccount(call: ApplicationCall): AuthServerApi.DeleteAccountResponse {
        val user = roleGuard.peekAuth(call)
            ?: return AuthServerApi.DeleteAccountResponse.unauthorized(errorUnauthorized())
        val token = call.request.cookies[config.sessionCookieName]
        sessionService.invalidate(token)
        userAuthService.deleteAccount(user.id)
        call.clearSessionCookie(config)
        return AuthServerApi.DeleteAccountResponse.noContent()
    }
}
