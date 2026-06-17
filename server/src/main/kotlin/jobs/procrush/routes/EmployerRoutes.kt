package jobs.procrush.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import jobs.procrush.auth.RoleGuard
import jobs.procrush.auth.UserRole
import jobs.procrush.domain.employer.EmployerProfileService
import jobs.procrush.models.CreateJobProfileRequest
import jobs.procrush.models.UpdateEmployerProfileRequest
import jobs.procrush.models.UpdateJobProfileRequest

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
            employerProfileService.findJobProfile(user.id, id)
            call.respond(employerProfileService.candidates(id))
        }
    }
}
