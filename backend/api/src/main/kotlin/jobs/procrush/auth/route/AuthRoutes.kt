package jobs.procrush.auth.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import jobs.procrush.auth.CompleteRegistrationRequest
import jobs.procrush.auth.DevLoginRequest
import jobs.procrush.auth.MeResponse
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.auth.service.SessionService
import jobs.procrush.auth.service.UserAuthService
import jobs.procrush.auth.service.clearSessionCookie
import jobs.procrush.auth.service.setSessionCookie
import jobs.procrush.bootstrap.config.AppConfig
import java.util.UUID

fun Route.authRoutes(
    config: AppConfig,
    userAuthService: UserAuthService,
    sessionService: SessionService,
    roleGuard: RoleGuard,
) {
    route("/api/auth") {
        post("/dev/login") {
            if (!config.authDevMode) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "error" to "dev_auth_disabled",
                        "message" to "Установите AUTH_DEV_MODE=true в .env, чтобы включить вход для разработки.",
                    ),
                )
                return@post
            }
            val body = call.receive<DevLoginRequest>()
            val user =
                userAuthService.findDevUser(body.email)
                    ?: return@post call.respond(userAuthService.pendingDevRegistration(body.email))
            val sessionToken = sessionService.createSession(UUID.fromString(user.id))
            call.setSessionCookie(config, sessionToken)
            call.respond(userAuthService.enrich(user))
        }

        get("/me") {
            val token = call.request.cookies[config.sessionCookieName]
            val user = sessionService.resolveUser(token)
            call.respond(MeResponse(user = user))
        }

        post("/logout") {
            val token = call.request.cookies[config.sessionCookieName]
            sessionService.invalidate(token)
            call.clearSessionCookie(config)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/complete-registration") {
            val body = call.receive<CompleteRegistrationRequest>()
            val token = call.request.cookies[config.sessionCookieName]
            val email =
                body.email?.trim()?.lowercase()
                    ?: sessionService.resolveUser(token)?.email
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Укажите email"))
            val updated = userAuthService.completeRegistration(email, body)
            val sessionToken = sessionService.createSession(UUID.fromString(updated.id))
            call.setSessionCookie(config, sessionToken)
            call.respond(userAuthService.enrich(updated))
        }

        delete("/account") {
            val user = roleGuard.requireAuth(call) ?: return@delete
            val token = call.request.cookies[config.sessionCookieName]
            sessionService.invalidate(token)
            userAuthService.deleteAccount(user.id)
            call.clearSessionCookie(config)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
