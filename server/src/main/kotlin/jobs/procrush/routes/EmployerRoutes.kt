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
import jobs.procrush.domain.EmployerProfileService
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
            try {
                employerProfileService.updateProfile(user.id, body)
            } catch (e: IllegalArgumentException) {
                return@patch call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Некорректные данные")))
            }
            call.respond(employerProfileService.getOrCreateEmployer(user.id))
        }
        get("/job-profiles") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@get
            call.respond(employerProfileService.listJobProfiles(user.id))
        }
        post("/job-profiles") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@post
            val body = call.receive<CreateJobProfileRequest>()
            try {
                val created = employerProfileService.createJobProfile(user.id, body)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Ошибка")))
            }
        }
        patch("/job-profiles/{id}") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@patch
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            val body = call.receive<UpdateJobProfileRequest>()
            try {
                employerProfileService.updateJobProfile(user.id, id, body)
                call.respond(employerProfileService.findJobProfile(user.id, id))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Не найдено")))
            }
        }
        delete("/job-profiles/{id}") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@delete
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            try {
                employerProfileService.deleteJobProfile(user.id, id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Не найдено")))
            }
        }
        get("/job-profiles/{id}/candidates") {
            val user = roleGuard.requireRole(call, UserRole.EMPLOYER) ?: return@get
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Некорректный id"))
            try {
                employerProfileService.findJobProfile(user.id, id)
                call.respond(employerProfileService.candidates(id))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Не найдено")))
            }
        }
    }
}
