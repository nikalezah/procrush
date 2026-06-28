package jobs.procrush.auth

import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import jobs.procrush.bootstrap.config.AppConfig

fun ApplicationCall.setSessionCookie(config: AppConfig, token: String) {
    response.cookies.append(sessionCookie(config, token, config.sessionDays * 24 * 60 * 60))
}

fun ApplicationCall.clearSessionCookie(config: AppConfig) {
    response.cookies.append(sessionCookie(config, "", maxAge = 0))
}

private fun sessionCookie(config: AppConfig, value: String, maxAge: Long): Cookie =
    Cookie(
        name = config.sessionCookieName,
        value = value,
        httpOnly = true,
        secure = config.cookieSecure,
        path = "/",
        maxAge = maxAge.toInt(),
        extensions = mapOf("SameSite" to "Lax"),
    )
