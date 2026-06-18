package jobs.procrush.shared.route

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.shared.repository.ReferenceRepository

fun Route.referenceRoutes(
    roleGuard: RoleGuard,
    referenceRepository: ReferenceRepository,
) {
    route("/api") {
        get("/occupations") {
            roleGuard.requireAuth(call) ?: return@get
            val leafOnly = call.request.queryParameters["leafOnly"]?.toBooleanStrictOrNull() ?: false
            call.respond(referenceRepository.listOccupations(leafOnly))
        }
        get("/skills") {
            roleGuard.requireAuth(call) ?: return@get
            val query = call.request.queryParameters["q"]
            call.respond(referenceRepository.searchSkills(query))
        }
    }
}
