package jobs.procrush.routes

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
import jobs.procrush.auth.RoleGuard
import jobs.procrush.auth.UserRole
import jobs.procrush.domain.personality.PersonalityProfileService
import jobs.procrush.domain.seeker.SeekerProfileService
import jobs.procrush.models.CreateSeekerEducationRequest
import jobs.procrush.models.CreateSeekerExperienceRequest
import jobs.procrush.models.UpdateSeekerDesiredPositionsRequest
import jobs.procrush.models.UpdateSeekerEducationRequest
import jobs.procrush.models.UpdateSeekerExperienceRequest
import jobs.procrush.models.UpdateSeekerProfileRequest
import jobs.procrush.models.UpdateSeekerSkillsRequest

fun Route.seekerProfileRoutes(
    roleGuard: RoleGuard,
    seekerProfileService: SeekerProfileService,
    personalityProfileService: PersonalityProfileService,
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
        put("/desired-positions") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@put
            val body = call.receive<UpdateSeekerDesiredPositionsRequest>()
            call.respond(seekerProfileService.setDesiredPositions(user.id, body.occupationIds))
        }
        get("/personality-preview") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(personalityProfileService.getPreview(user.id))
        }
        post("/personality/generate") {
            val user = roleGuard.requireRole(call, UserRole.SEEKER) ?: return@post
            personalityProfileService.triggerGeneration(user.id)
            call.respond(mapOf("status" to "PROCESSING"))
        }
        get("/recommendations") {
            roleGuard.requireRole(call, UserRole.SEEKER) ?: return@get
            call.respond(seekerProfileService.recommendations())
        }
    }
}
