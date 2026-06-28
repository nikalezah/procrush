package jobs.procrush.api.handler

import io.ktor.server.application.ApplicationCall
import jobs.procrush.api.generated.employer_models_yaml.employer_models.CreateJobProfileRequest
import jobs.procrush.api.generated.employer_models_yaml.employer_models.UpdateEmployerProfileRequest
import jobs.procrush.api.generated.employer_models_yaml.employer_models.UpdateJobProfileRequest
import jobs.procrush.api.generated.employer_paths_yaml.employer_paths.EmployerServerApi
import jobs.procrush.api.mapper.toApi
import jobs.procrush.api.mapper.toContract
import jobs.procrush.auth.service.RoleGuard
import jobs.procrush.employer.service.EmployerProfileService
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.shared.CodedException
import jobs.procrush.shared.ResourceNotFoundException

class EmployerHandler(
    private val roleGuard: RoleGuard,
    private val employerProfileService: EmployerProfileService,
    private val matchInterestService: MatchInterestService,
) : EmployerServerApi {
    override suspend fun getEmployerDashboard(call: ApplicationCall): EmployerServerApi.GetEmployerDashboardResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.GetEmployerDashboardResponse::unauthorized,
            EmployerServerApi.GetEmployerDashboardResponse::forbidden,
        ) { user ->
            EmployerServerApi.GetEmployerDashboardResponse.ok(employerProfileService.dashboard(user.id).toApi())
        }

    override suspend fun getEmployerProfile(call: ApplicationCall): EmployerServerApi.GetEmployerProfileResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.GetEmployerProfileResponse::unauthorized,
            EmployerServerApi.GetEmployerProfileResponse::forbidden,
        ) { user ->
            EmployerServerApi.GetEmployerProfileResponse.ok(employerProfileService.getOrCreateEmployer(user.id).toApi())
        }

    override suspend fun updateEmployerProfile(
        request: UpdateEmployerProfileRequest,
        call: ApplicationCall,
    ): EmployerServerApi.UpdateEmployerProfileResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.UpdateEmployerProfileResponse::unauthorized,
            EmployerServerApi.UpdateEmployerProfileResponse::forbidden,
        ) { user ->
            try {
                employerProfileService.updateProfile(user.id, request.toContract())
                EmployerServerApi.UpdateEmployerProfileResponse.ok(
                    employerProfileService.getOrCreateEmployer(user.id).toApi(),
                )
            } catch (e: CodedException) {
                EmployerServerApi.UpdateEmployerProfileResponse.badRequest(errorBadRequest(e.errorCode, e.details))
            }
        }

    override suspend fun listJobProfiles(call: ApplicationCall): EmployerServerApi.ListJobProfilesResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.ListJobProfilesResponse::unauthorized,
            EmployerServerApi.ListJobProfilesResponse::forbidden,
        ) { user ->
            EmployerServerApi.ListJobProfilesResponse.ok(
                employerProfileService.listJobProfiles(user.id).map { it.toApi() },
            )
        }

    override suspend fun createJobProfile(
        request: CreateJobProfileRequest,
        call: ApplicationCall,
    ): EmployerServerApi.CreateJobProfileResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.CreateJobProfileResponse::unauthorized,
            EmployerServerApi.CreateJobProfileResponse::forbidden,
        ) { user ->
            try {
                EmployerServerApi.CreateJobProfileResponse.created(
                    employerProfileService.createJobProfile(user.id, request.toContract()).toApi(),
                )
            } catch (e: CodedException) {
                EmployerServerApi.CreateJobProfileResponse.badRequest(errorBadRequest(e.errorCode, e.details))
            }
        }

    override suspend fun deleteJobProfile(
        id: Long,
        call: ApplicationCall,
    ): EmployerServerApi.DeleteJobProfileResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.DeleteJobProfileResponse::unauthorized,
            EmployerServerApi.DeleteJobProfileResponse::forbidden,
        ) { user ->
            try {
                employerProfileService.deleteJobProfile(user.id, id)
                EmployerServerApi.DeleteJobProfileResponse.noContent()
            } catch (_: ResourceNotFoundException) {
                EmployerServerApi.DeleteJobProfileResponse.notFound(errorNotFound())
            }
        }

    override suspend fun updateJobProfile(
        request: UpdateJobProfileRequest,
        id: Long,
        call: ApplicationCall,
    ): EmployerServerApi.UpdateJobProfileResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.UpdateJobProfileResponse::unauthorized,
            EmployerServerApi.UpdateJobProfileResponse::forbidden,
        ) { user ->
            try {
                employerProfileService.updateJobProfile(user.id, id, request.toContract())
                EmployerServerApi.UpdateJobProfileResponse.ok(
                    employerProfileService.findJobProfile(user.id, id).toApi(),
                )
            } catch (e: CodedException) {
                EmployerServerApi.UpdateJobProfileResponse.badRequest(errorBadRequest(e.errorCode, e.details))
            } catch (_: ResourceNotFoundException) {
                EmployerServerApi.UpdateJobProfileResponse.notFound(errorNotFound())
            }
        }

    override suspend fun getJobCandidates(
        id: Long,
        call: ApplicationCall,
    ): EmployerServerApi.GetJobCandidatesResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.GetJobCandidatesResponse::unauthorized,
            EmployerServerApi.GetJobCandidatesResponse::forbidden,
        ) { user ->
            try {
                EmployerServerApi.GetJobCandidatesResponse.ok(
                    employerProfileService.candidates(user.id, id).map { it.toApi() },
                )
            } catch (_: ResourceNotFoundException) {
                EmployerServerApi.GetJobCandidatesResponse.notFound(errorNotFound())
            }
        }

    override suspend fun getJobCandidatesOverview(
        id: Long,
        call: ApplicationCall,
    ): EmployerServerApi.GetJobCandidatesOverviewResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.GetJobCandidatesOverviewResponse::unauthorized,
            EmployerServerApi.GetJobCandidatesOverviewResponse::forbidden,
        ) { user ->
            try {
                EmployerServerApi.GetJobCandidatesOverviewResponse.ok(
                    employerProfileService.candidatesOverview(user.id, id).toApi(),
                )
            } catch (_: ResourceNotFoundException) {
                EmployerServerApi.GetJobCandidatesOverviewResponse.notFound(errorNotFound())
            }
        }

    override suspend fun employerRespondToCandidate(
        id: Long,
        seekerId: Long,
        call: ApplicationCall,
    ): EmployerServerApi.EmployerRespondToCandidateResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.EmployerRespondToCandidateResponse::unauthorized,
            EmployerServerApi.EmployerRespondToCandidateResponse::forbidden,
        ) { user ->
            try {
                EmployerServerApi.EmployerRespondToCandidateResponse.created(
                    employerProfileService.respondToCandidate(user.id, id, seekerId).toApi(),
                )
            } catch (_: ResourceNotFoundException) {
                EmployerServerApi.EmployerRespondToCandidateResponse.notFound(errorNotFound())
            }
        }

    override suspend fun getEmployerInterests(
        id: Long,
        call: ApplicationCall,
    ): EmployerServerApi.GetEmployerInterestsResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.GetEmployerInterestsResponse::unauthorized,
            EmployerServerApi.GetEmployerInterestsResponse::forbidden,
        ) { user ->
            try {
                EmployerServerApi.GetEmployerInterestsResponse.ok(
                    employerProfileService.interestsOutsideRecommendations(user.id, id).toApi(),
                )
            } catch (_: ResourceNotFoundException) {
                EmployerServerApi.GetEmployerInterestsResponse.notFound(errorNotFound())
            }
        }

    override suspend fun getEmployerMatchInterestCount(call: ApplicationCall): EmployerServerApi.GetEmployerMatchInterestCountResponse =
        roleGuard.withEmployer(
            call,
            EmployerServerApi.GetEmployerMatchInterestCountResponse::unauthorized,
            EmployerServerApi.GetEmployerMatchInterestCountResponse::forbidden,
        ) { user ->
            EmployerServerApi.GetEmployerMatchInterestCountResponse.ok(
                matchInterestService.actionableCountForEmployer(user.id).toApi(),
            )
        }
}
