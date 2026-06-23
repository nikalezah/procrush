package jobs.procrush.seeker.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import jobs.procrush.auth.UserRole
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.bootstrap.route.requireLongParam
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.seeker.dto.CreateSeekerEducationRequest
import jobs.procrush.seeker.dto.CreateSeekerExperienceRequest
import jobs.procrush.seeker.dto.UpdateSeekerDesiredPositionsRequest
import jobs.procrush.seeker.dto.UpdateSeekerEducationRequest
import jobs.procrush.seeker.dto.UpdateSeekerExperienceRequest
import jobs.procrush.seeker.dto.UpdateSeekerProfileRequest
import jobs.procrush.seeker.dto.UpdateSeekerSkillsRequest
import jobs.procrush.seeker.service.SeekerProfileService
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

private val matchInterestJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

fun Route.seekerProfileRoutes(
    roleGuard: RoleGuard,
    seekerProfileService: SeekerProfileService,
    matchInterestService: MatchInterestService,
) {
    route("/api/seeker") {
        get("/dashboard") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.dashboard(user.id))
        }
        get("/me") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.getOrCreateSeeker(user.id))
        }
        patch("/me") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@patch
            val body = call.receive<UpdateSeekerProfileRequest>()
            seekerProfileService.updateProfile(user.id, body)
            call.respond(seekerProfileService.getOrCreateSeeker(user.id))
        }
        get("/experience") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.listExperience(user.id))
        }
        post("/experience") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            val body = call.receive<CreateSeekerExperienceRequest>()
            call.respond(HttpStatusCode.Created, seekerProfileService.createExperience(user.id, body))
        }
        patch("/experience/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@patch
            val id = call.requireLongParam() ?: return@patch
            val body = call.receive<UpdateSeekerExperienceRequest>()
            call.respond(seekerProfileService.updateExperience(user.id, id, body))
        }
        delete("/experience/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@delete
            val id = call.requireLongParam() ?: return@delete
            seekerProfileService.deleteExperience(user.id, id)
            call.respond(HttpStatusCode.NoContent)
        }
        get("/education") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.listEducation(user.id))
        }
        post("/education") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            val body = call.receive<CreateSeekerEducationRequest>()
            call.respond(HttpStatusCode.Created, seekerProfileService.createEducation(user.id, body))
        }
        patch("/education/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@patch
            val id = call.requireLongParam() ?: return@patch
            val body = call.receive<UpdateSeekerEducationRequest>()
            call.respond(seekerProfileService.updateEducation(user.id, id, body))
        }
        delete("/education/{id}") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@delete
            val id = call.requireLongParam() ?: return@delete
            seekerProfileService.deleteEducation(user.id, id)
            call.respond(HttpStatusCode.NoContent)
        }
        get("/skills") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.getSkills(user.id))
        }
        put("/skills") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@put
            val body = call.receive<UpdateSeekerSkillsRequest>()
            call.respond(seekerProfileService.setSkills(user.id, body.skillIds))
        }
        get("/desired-positions") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.getDesiredPositions(user.id))
        }
        get("/positions-overview") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.positionsOverview(user.id))
        }
        put("/desired-positions") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@put
            val body = call.receive<UpdateSeekerDesiredPositionsRequest>()
            call.respond(seekerProfileService.setDesiredPositions(user.id, body.occupationIds))
        }
        get("/recommendations") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.recommendations(user.id))
        }
        post("/recommendations/{jobProfileId}/respond") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            val jobProfileId = call.requireLongParam("jobProfileId") ?: return@post
            call.respond(
                HttpStatusCode.Created,
                seekerProfileService.respondToJob(user.id, jobProfileId),
            )
        }
        get("/interests") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.interestsOutsideRecommendations(user.id))
        }
        get("/match-interests/count") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(matchInterestService.actionableCountForSeeker(user.id))
        }
        sse("/match-interests/events") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@sse
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
