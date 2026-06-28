package jobs.procrush.api.route

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import jobs.procrush.auth.RoleGuard
import jobs.procrush.auth.UserRole
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.personality.service.PersonalityProfileService
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

private val matchInterestJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

fun Route.sseRoutes(
    roleGuard: RoleGuard,
    matchInterestService: MatchInterestService,
    personalityProfileService: PersonalityProfileService,
) {
    route("/api/seeker") {
        sse("/match-interests/events") {
            val user = roleGuard.peekRole(call, UserRole.SEEKER) ?: return@sse
            heartbeat {
                period = 30.seconds
                event = ServerSentEvent(comments = "keepalive")
            }
            matchInterestService.streamEvents(user.id) { event ->
                send(
                    data = matchInterestJson.encodeToString(event),
                    event = "match-interest",
                )
            }
        }
        sse("/personality-preview/events") {
            val user = roleGuard.peekRole(call, UserRole.SEEKER) ?: return@sse
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
    }
    route("/api/employer") {
        sse("/match-interests/events") {
            val user = roleGuard.peekRole(call, UserRole.EMPLOYER) ?: return@sse
            heartbeat {
                period = 30.seconds
                event = ServerSentEvent(comments = "keepalive")
            }
            matchInterestService.streamEvents(user.id) { event ->
                send(
                    data = matchInterestJson.encodeToString(event),
                    event = "match-interest",
                )
            }
        }
    }
}
