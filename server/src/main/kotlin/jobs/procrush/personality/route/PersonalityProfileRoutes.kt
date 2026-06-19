package jobs.procrush.personality.route

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import jobs.procrush.auth.UserRole
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.personality.service.PersonalityProfileService
import kotlin.time.Duration.Companion.seconds

fun Route.personalityProfileRoutes(
    roleGuard: RoleGuard,
    personalityProfileService: PersonalityProfileService,
) {
    route("/api/seeker") {
        get("/personality-preview") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(personalityProfileService.getPreview(user.id))
        }
        sse("/personality-preview/events") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@sse
            heartbeat {
                period = 30.seconds
                event = ServerSentEvent(comments = "keepalive")
            }
            personalityProfileService.streamStatusEvents(user.id) { status ->
                send(
                    data = """{"status":"${status.name}"}""",
                    event = "personality-status",
                )
            }
        }
        post("/personality/generate") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            personalityProfileService.triggerGeneration(user.id)
            call.respond(mapOf("status" to "PROCESSING"))
        }
    }
}
