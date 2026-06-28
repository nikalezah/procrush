package jobs.procrush.api.handler

import io.ktor.server.application.ApplicationCall
import jobs.procrush.api.generated.reference_paths_yaml.reference_paths.ReferenceServerApi
import jobs.procrush.api.mapper.toApi
import jobs.procrush.auth.RoleGuard
import jobs.procrush.shared.repository.ReferenceRepository

class ReferenceHandler(
    private val roleGuard: RoleGuard,
    private val referenceRepository: ReferenceRepository,
) : ReferenceServerApi {
    override suspend fun listOccupations(
        leafOnly: Boolean?,
        call: ApplicationCall,
    ): ReferenceServerApi.ListOccupationsResponse =
        roleGuard.withAuth(
            call = call,
            onUnauthorized = ReferenceServerApi.ListOccupationsResponse::unauthorized,
            onForbidden = ReferenceServerApi.ListOccupationsResponse::unauthorized,
        ) {
            val leaf = leafOnly ?: false
            ReferenceServerApi.ListOccupationsResponse.ok(referenceRepository.listOccupations(leaf).map { it.toApi() })
        }

    override suspend fun searchSkills(
        q: String?,
        call: ApplicationCall,
    ): ReferenceServerApi.SearchSkillsResponse =
        roleGuard.withAuth(
            call = call,
            onUnauthorized = ReferenceServerApi.SearchSkillsResponse::unauthorized,
            onForbidden = ReferenceServerApi.SearchSkillsResponse::unauthorized,
        ) {
            ReferenceServerApi.SearchSkillsResponse.ok(referenceRepository.searchSkills(q).map { it.toApi() })
        }
}
