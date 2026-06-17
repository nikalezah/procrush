package jobs.procrush.routes

import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
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
import jobs.procrush.auth.SessionService
import jobs.procrush.auth.UserAuthService
import jobs.procrush.config.AppConfig
import java.util.UUID

fun Route.authRoutes(
    config: AppConfig,
    userAuthService: UserAuthService,
    sessionService: SessionService,
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
                try {
                    userAuthService.findDevUser(body.email)
                        ?: return@post call.respond(userAuthService.pendingDevRegistration(body.email))
                } catch (e: IllegalArgumentException) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Некорректный email")))
                }
            val sessionToken = sessionService.createSession(UUID.fromString(user.id))
            call.setSessionCookie(config, sessionToken)
            call.respond(userAuthService.withProfileName(user))
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
            val updated =
                try {
                    userAuthService.completeRegistration(email, body)
                } catch (e: IllegalArgumentException) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Некорректные данные")))
                } catch (e: IllegalStateException) {
                    return@post call.respond(HttpStatusCode.Conflict, mapOf("message" to (e.message ?: "Пользователь уже зарегистрирован")))
                }
            val sessionToken = sessionService.createSession(UUID.fromString(updated.id))
            call.setSessionCookie(config, sessionToken)
            call.respond(userAuthService.withProfileName(updated))
        }

        delete("/account") {
            val token =
                call.request.cookies[config.sessionCookieName]
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Не авторизован")
            val user =
                sessionService.resolveUser(token)
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Не авторизован")
            val userId =
                try {
                    java.util.UUID.fromString(user.id)
                } catch (_: IllegalArgumentException) {
                    return@delete call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id пользователя"))
                }
            sessionService.invalidate(token)
            try {
                userAuthService.deleteAccount(userId)
            } catch (e: IllegalStateException) {
                return@delete call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Пользователь не найден")))
            }
            call.clearSessionCookie(config)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun ApplicationCall.setSessionCookie(config: AppConfig, token: String) {
    response.cookies.append(
        Cookie(
            name = config.sessionCookieName,
            value = token,
            httpOnly = true,
            secure = config.cookieSecure,
            path = "/",
            maxAge = (config.sessionDays * 24 * 60 * 60).toInt(),
            extensions = mapOf("SameSite" to "Lax"),
        ),
    )
}

private fun ApplicationCall.clearSessionCookie(config: AppConfig) {
    response.cookies.append(
        Cookie(
            name = config.sessionCookieName,
            value = "",
            httpOnly = true,
            secure = config.cookieSecure,
            path = "/",
            maxAge = 0,
            extensions = mapOf("SameSite" to "Lax"),
        ),
    )
}
