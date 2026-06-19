package jobs.procrush.employer.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import jobs.procrush.auth.UserRole
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.bootstrap.route.requireLongParam
import jobs.procrush.employer.dto.CreateJobProfileRequest
import jobs.procrush.employer.dto.UpdateEmployerProfileRequest
import jobs.procrush.employer.dto.UpdateJobProfileRequest
import jobs.procrush.employer.service.EmployerProfileService

fun Route.employerRoutes(
    roleGuard: RoleGuard,
    employerProfileService: EmployerProfileService,
) {
    route("/api/employer") {
        get("/dashboard") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@get
            call.respond(employerProfileService.dashboard(user.id))
        }
        get("/me") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@get
            call.respond(employerProfileService.getOrCreateEmployer(user.id))
        }
        patch("/me") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@patch
            val body = call.receive<UpdateEmployerProfileRequest>()
            employerProfileService.updateProfile(user.id, body)
            call.respond(employerProfileService.getOrCreateEmployer(user.id))
        }
        get("/job-profiles") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@get
            call.respond(employerProfileService.listJobProfiles(user.id))
        }
        post("/job-profiles") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@post
            val body = call.receive<CreateJobProfileRequest>()
            val created = employerProfileService.createJobProfile(user.id, body)
            call.respond(HttpStatusCode.Created, created)
        }
        patch("/job-profiles/{id}") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@patch
            val id = call.requireLongParam() ?: return@patch
            val body = call.receive<UpdateJobProfileRequest>()
            employerProfileService.updateJobProfile(user.id, id, body)
            call.respond(employerProfileService.findJobProfile(user.id, id))
        }
        delete("/job-profiles/{id}") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@delete
            val id = call.requireLongParam() ?: return@delete
            employerProfileService.deleteJobProfile(user.id, id)
            call.respond(HttpStatusCode.NoContent)
        }
        get("/job-profiles/{id}/candidates") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@get
            val id = call.requireLongParam() ?: return@get
            call.respond(employerProfileService.candidates(user.id, id))
        }
        post("/job-profiles/{id}/candidates/{seekerId}/respond") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@post
            val jobProfileId = call.requireLongParam() ?: return@post
            val seekerId = call.requireLongParam("seekerId") ?: return@post
            call.respond(
                HttpStatusCode.Created,
                employerProfileService.respondToCandidate(user.id, jobProfileId, seekerId),
            )
        }
        get("/job-profiles/{id}/interests") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@get
            val id = call.requireLongParam() ?: return@get
            call.respond(employerProfileService.interestsOutsideRecommendations(user.id, id))
        }
    }
}
