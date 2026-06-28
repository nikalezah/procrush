package jobs.procrush.api.handler

import io.ktor.server.application.ApplicationCall
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.CreateSeekerEducationRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.CreateSeekerExperienceRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerDesiredPositionsRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerEducationRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerExperienceRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerProfileRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerSkillsRequest
import jobs.procrush.api.generated.seeker_paths_yaml.seeker_paths.SeekerProfileServerApi
import jobs.procrush.api.mapper.toApi
import jobs.procrush.api.mapper.toContract
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.seeker.service.SeekerProfileService
import jobs.procrush.shared.ResourceNotFoundException

class SeekerProfileHandler(
    private val roleGuard: RoleGuard,
    private val seekerProfileService: SeekerProfileService,
    private val matchInterestService: MatchInterestService,
) : SeekerProfileServerApi {
    override suspend fun getSeekerDashboard(call: ApplicationCall): SeekerProfileServerApi.GetSeekerDashboardResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.GetSeekerDashboardResponse::unauthorized,
            SeekerProfileServerApi.GetSeekerDashboardResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.GetSeekerDashboardResponse.ok(seekerProfileService.dashboard(user.id).toApi())
        }

    override suspend fun getSeekerProfile(call: ApplicationCall): SeekerProfileServerApi.GetSeekerProfileResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.GetSeekerProfileResponse::unauthorized,
            SeekerProfileServerApi.GetSeekerProfileResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.GetSeekerProfileResponse.ok(seekerProfileService.getOrCreateSeeker(user.id).toApi())
        }

    override suspend fun updateSeekerProfile(
        request: UpdateSeekerProfileRequest,
        call: ApplicationCall,
    ): SeekerProfileServerApi.UpdateSeekerProfileResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.UpdateSeekerProfileResponse::unauthorized,
            SeekerProfileServerApi.UpdateSeekerProfileResponse::forbidden,
        ) { user ->
            try {
                seekerProfileService.updateProfile(user.id, request.toContract())
                SeekerProfileServerApi.UpdateSeekerProfileResponse.ok(
                    seekerProfileService.getOrCreateSeeker(user.id).toApi(),
                )
            } catch (e: IllegalArgumentException) {
                SeekerProfileServerApi.UpdateSeekerProfileResponse.badRequest(badRequest(e.message ?: "Некорректные данные"))
            }
        }

    override suspend fun listSeekerExperience(call: ApplicationCall): SeekerProfileServerApi.ListSeekerExperienceResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.ListSeekerExperienceResponse::unauthorized,
            SeekerProfileServerApi.ListSeekerExperienceResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.ListSeekerExperienceResponse.ok(
                seekerProfileService.listExperience(user.id).map { it.toApi() },
            )
        }

    override suspend fun createSeekerExperience(
        request: CreateSeekerExperienceRequest,
        call: ApplicationCall,
    ): SeekerProfileServerApi.CreateSeekerExperienceResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.CreateSeekerExperienceResponse::unauthorized,
            SeekerProfileServerApi.CreateSeekerExperienceResponse::forbidden,
        ) { user ->
            try {
                SeekerProfileServerApi.CreateSeekerExperienceResponse.created(
                    seekerProfileService.createExperience(user.id, request.toContract()).toApi(),
                )
            } catch (e: IllegalArgumentException) {
                SeekerProfileServerApi.CreateSeekerExperienceResponse.badRequest(badRequest(e.message ?: "Некорректные данные"))
            }
        }

    override suspend fun deleteSeekerExperience(
        id: Long,
        call: ApplicationCall,
    ): SeekerProfileServerApi.DeleteSeekerExperienceResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.DeleteSeekerExperienceResponse::unauthorized,
            SeekerProfileServerApi.DeleteSeekerExperienceResponse::forbidden,
        ) { user ->
            try {
                seekerProfileService.deleteExperience(user.id, id)
                SeekerProfileServerApi.DeleteSeekerExperienceResponse.noContent()
            } catch (_: ResourceNotFoundException) {
                SeekerProfileServerApi.DeleteSeekerExperienceResponse.notFound(notFound())
            }
        }

    override suspend fun updateSeekerExperience(
        request: UpdateSeekerExperienceRequest,
        id: Long,
        call: ApplicationCall,
    ): SeekerProfileServerApi.UpdateSeekerExperienceResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.UpdateSeekerExperienceResponse::unauthorized,
            SeekerProfileServerApi.UpdateSeekerExperienceResponse::forbidden,
        ) { user ->
            try {
                SeekerProfileServerApi.UpdateSeekerExperienceResponse.ok(
                    seekerProfileService.updateExperience(user.id, id, request.toContract()).toApi(),
                )
            } catch (e: IllegalArgumentException) {
                SeekerProfileServerApi.UpdateSeekerExperienceResponse.badRequest(badRequest(e.message ?: "Некорректные данные"))
            } catch (_: ResourceNotFoundException) {
                SeekerProfileServerApi.UpdateSeekerExperienceResponse.notFound(notFound())
            }
        }

    override suspend fun listSeekerEducation(call: ApplicationCall): SeekerProfileServerApi.ListSeekerEducationResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.ListSeekerEducationResponse::unauthorized,
            SeekerProfileServerApi.ListSeekerEducationResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.ListSeekerEducationResponse.ok(
                seekerProfileService.listEducation(user.id).map { it.toApi() },
            )
        }

    override suspend fun createSeekerEducation(
        request: CreateSeekerEducationRequest,
        call: ApplicationCall,
    ): SeekerProfileServerApi.CreateSeekerEducationResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.CreateSeekerEducationResponse::unauthorized,
            SeekerProfileServerApi.CreateSeekerEducationResponse::forbidden,
        ) { user ->
            try {
                SeekerProfileServerApi.CreateSeekerEducationResponse.created(
                    seekerProfileService.createEducation(user.id, request.toContract()).toApi(),
                )
            } catch (e: IllegalArgumentException) {
                SeekerProfileServerApi.CreateSeekerEducationResponse.badRequest(badRequest(e.message ?: "Некорректные данные"))
            }
        }

    override suspend fun deleteSeekerEducation(
        id: Long,
        call: ApplicationCall,
    ): SeekerProfileServerApi.DeleteSeekerEducationResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.DeleteSeekerEducationResponse::unauthorized,
            SeekerProfileServerApi.DeleteSeekerEducationResponse::forbidden,
        ) { user ->
            try {
                seekerProfileService.deleteEducation(user.id, id)
                SeekerProfileServerApi.DeleteSeekerEducationResponse.noContent()
            } catch (_: ResourceNotFoundException) {
                SeekerProfileServerApi.DeleteSeekerEducationResponse.notFound(notFound())
            }
        }

    override suspend fun updateSeekerEducation(
        request: UpdateSeekerEducationRequest,
        id: Long,
        call: ApplicationCall,
    ): SeekerProfileServerApi.UpdateSeekerEducationResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.UpdateSeekerEducationResponse::unauthorized,
            SeekerProfileServerApi.UpdateSeekerEducationResponse::forbidden,
        ) { user ->
            try {
                SeekerProfileServerApi.UpdateSeekerEducationResponse.ok(
                    seekerProfileService.updateEducation(user.id, id, request.toContract()).toApi(),
                )
            } catch (e: IllegalArgumentException) {
                SeekerProfileServerApi.UpdateSeekerEducationResponse.badRequest(badRequest(e.message ?: "Некорректные данные"))
            } catch (_: ResourceNotFoundException) {
                SeekerProfileServerApi.UpdateSeekerEducationResponse.notFound(notFound())
            }
        }

    override suspend fun getSeekerSkills(call: ApplicationCall): SeekerProfileServerApi.GetSeekerSkillsResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.GetSeekerSkillsResponse::unauthorized,
            SeekerProfileServerApi.GetSeekerSkillsResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.GetSeekerSkillsResponse.ok(seekerProfileService.getSkills(user.id).toApi())
        }

    override suspend fun setSeekerSkills(
        request: UpdateSeekerSkillsRequest,
        call: ApplicationCall,
    ): SeekerProfileServerApi.SetSeekerSkillsResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.SetSeekerSkillsResponse::unauthorized,
            SeekerProfileServerApi.SetSeekerSkillsResponse::forbidden,
        ) { user ->
            try {
                SeekerProfileServerApi.SetSeekerSkillsResponse.ok(
                    seekerProfileService.setSkills(user.id, request.skillIds).toApi(),
                )
            } catch (e: IllegalArgumentException) {
                SeekerProfileServerApi.SetSeekerSkillsResponse.badRequest(badRequest(e.message ?: "Некорректные данные"))
            }
        }

    override suspend fun getSeekerDesiredPositions(call: ApplicationCall): SeekerProfileServerApi.GetSeekerDesiredPositionsResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.GetSeekerDesiredPositionsResponse::unauthorized,
            SeekerProfileServerApi.GetSeekerDesiredPositionsResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.GetSeekerDesiredPositionsResponse.ok(
                seekerProfileService.getDesiredPositions(user.id).toApi(),
            )
        }

    override suspend fun setSeekerDesiredPositions(
        request: UpdateSeekerDesiredPositionsRequest,
        call: ApplicationCall,
    ): SeekerProfileServerApi.SetSeekerDesiredPositionsResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.SetSeekerDesiredPositionsResponse::unauthorized,
            SeekerProfileServerApi.SetSeekerDesiredPositionsResponse::forbidden,
        ) { user ->
            try {
                SeekerProfileServerApi.SetSeekerDesiredPositionsResponse.ok(
                    seekerProfileService.setDesiredPositions(user.id, request.occupationIds).toApi(),
                )
            } catch (e: IllegalArgumentException) {
                SeekerProfileServerApi.SetSeekerDesiredPositionsResponse.badRequest(badRequest(e.message ?: "Некорректные данные"))
            }
        }

    override suspend fun getSeekerPositionsOverview(call: ApplicationCall): SeekerProfileServerApi.GetSeekerPositionsOverviewResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.GetSeekerPositionsOverviewResponse::unauthorized,
            SeekerProfileServerApi.GetSeekerPositionsOverviewResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.GetSeekerPositionsOverviewResponse.ok(
                seekerProfileService.positionsOverview(user.id).toApi(),
            )
        }

    override suspend fun getSeekerRecommendations(call: ApplicationCall): SeekerProfileServerApi.GetSeekerRecommendationsResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.GetSeekerRecommendationsResponse::unauthorized,
            SeekerProfileServerApi.GetSeekerRecommendationsResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.GetSeekerRecommendationsResponse.ok(
                seekerProfileService.recommendations(user.id).map { it.toApi() },
            )
        }

    override suspend fun seekerRespondToJob(
        jobProfileId: Long,
        call: ApplicationCall,
    ): SeekerProfileServerApi.SeekerRespondToJobResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.SeekerRespondToJobResponse::unauthorized,
            SeekerProfileServerApi.SeekerRespondToJobResponse::forbidden,
        ) { user ->
            try {
                SeekerProfileServerApi.SeekerRespondToJobResponse.created(
                    seekerProfileService.respondToJob(user.id, jobProfileId).toApi(),
                )
            } catch (_: ResourceNotFoundException) {
                SeekerProfileServerApi.SeekerRespondToJobResponse.notFound(notFound())
            }
        }

    override suspend fun getSeekerInterests(call: ApplicationCall): SeekerProfileServerApi.GetSeekerInterestsResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.GetSeekerInterestsResponse::unauthorized,
            SeekerProfileServerApi.GetSeekerInterestsResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.GetSeekerInterestsResponse.ok(
                seekerProfileService.interestsOutsideRecommendations(user.id).toApi(),
            )
        }

    override suspend fun getSeekerMatchInterestCount(call: ApplicationCall): SeekerProfileServerApi.GetSeekerMatchInterestCountResponse =
        roleGuard.withSeeker(
            call,
            SeekerProfileServerApi.GetSeekerMatchInterestCountResponse::unauthorized,
            SeekerProfileServerApi.GetSeekerMatchInterestCountResponse::forbidden,
        ) { user ->
            SeekerProfileServerApi.GetSeekerMatchInterestCountResponse.ok(
                matchInterestService.actionableCountForSeeker(user.id).toApi(),
            )
        }
}
