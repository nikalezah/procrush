package jobs.procrush.employer.service

import jobs.procrush.employer.dto.CreateJobProfileRequest
import jobs.procrush.employer.dto.EmployerDashboardDto
import jobs.procrush.employer.dto.JobProfileDto
import jobs.procrush.employer.dto.UpdateEmployerProfileRequest
import jobs.procrush.employer.dto.UpdateJobProfileRequest
import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.matching.cache.CachedMatchingService
import jobs.procrush.matching.cache.MatchingCacheInvalidator
import jobs.procrush.matching.dto.EmployerCandidatesOverviewDto
import jobs.procrush.matching.dto.EmployerInterestsResponseDto
import jobs.procrush.matching.service.MatchInterestService
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.shared.repository.ReferenceRepository
import java.util.UUID

class EmployerProfileService(
    private val employerRepository: EmployerRepository,
    private val referenceRepository: ReferenceRepository,
    private val matchingService: CachedMatchingService,
    private val matchInterestService: MatchInterestService,
    private val matchingCacheInvalidator: MatchingCacheInvalidator,
) {
    fun getOrCreateEmployer(userId: UUID) =
        employerRepository.findByUserId(userId) ?: employerRepository.createForUser(userId)

    fun updateProfile(userId: UUID, request: UpdateEmployerProfileRequest) {
        val name = request.name.trim()
        require(name.isNotBlank()) { "Укажите название компании" }
        val employer = getOrCreateEmployer(userId)
        employerRepository.updateProfile(employer.id, request)
            ?: throw ResourceNotFoundException("Не удалось обновить профиль компании")
    }

    fun listJobProfiles(userId: UUID) =
        employerRepository.listJobProfiles(getOrCreateEmployer(userId).id)

    fun createJobProfile(userId: UUID, request: CreateJobProfileRequest): JobProfileDto {
        referenceRepository.findOccupationById(request.occupationId)
            ?: throw IllegalArgumentException("Должность не найдена")
        val created = employerRepository.createJobProfile(getOrCreateEmployer(userId).id, request)
        matchingCacheInvalidator.invalidateJobCandidates(created.id)
        return created
    }

    fun updateJobProfile(userId: UUID, jobProfileId: Long, request: UpdateJobProfileRequest) {
        referenceRepository.findOccupationById(request.occupationId)
            ?: throw IllegalArgumentException("Должность не найдена")
        employerRepository.updateJobProfile(getOrCreateEmployer(userId).id, jobProfileId, request)
            ?: throw ResourceNotFoundException("Профиль не найден")
        matchingCacheInvalidator.invalidateJobCandidates(jobProfileId)
    }

    fun deleteJobProfile(userId: UUID, jobProfileId: Long) {
        if (!employerRepository.deleteJobProfile(getOrCreateEmployer(userId).id, jobProfileId)) {
            throw ResourceNotFoundException("Профиль не найден")
        }
        matchingCacheInvalidator.invalidateJobCandidates(jobProfileId)
    }

    fun findJobProfile(userId: UUID, jobProfileId: Long) =
        employerRepository.findJobProfile(getOrCreateEmployer(userId).id, jobProfileId)
            ?: throw ResourceNotFoundException("Профиль не найден")

    fun dashboard(userId: UUID): EmployerDashboardDto {
        val employer = getOrCreateEmployer(userId)
        val profiles = employerRepository.listJobProfiles(employer.id)
        val activeProfiles = profiles.filter { it.isActive }
        val countsByOccupation =
            matchingService.countMatchedCandidatesForOccupations(
                activeProfiles.map { it.occupationId }.distinct(),
            )
        val totalMatchedCandidates =
            activeProfiles.sumOf { profile ->
                countsByOccupation[profile.occupationId] ?: 0
            }
        return EmployerDashboardDto(
            companyName = employer.name.ifBlank { "Компания не указана" },
            jobProfilesCount = profiles.size,
            activeJobProfilesCount = activeProfiles.size,
            totalMatchedCandidates = totalMatchedCandidates,
        )
    }

    fun candidates(userId: UUID, jobProfileId: Long): List<jobs.procrush.matching.dto.CandidateRecommendationDto> {
        val jobProfile = findJobProfile(userId, jobProfileId)
        val candidates =
            matchingService.candidateRecommendationsForJob(jobProfile.occupationId, jobProfileId)
        return matchInterestService.enrichCandidateRecommendations(jobProfileId, candidates)
    }

    fun respondToCandidate(userId: UUID, jobProfileId: Long, seekerId: Long) =
        matchInterestService.employerRespond(userId, jobProfileId, seekerId)

    fun interestsOutsideRecommendations(userId: UUID, jobProfileId: Long): EmployerInterestsResponseDto {
        val jobProfile = findJobProfile(userId, jobProfileId)
        val candidates =
            matchingService.candidateRecommendationsForJob(jobProfile.occupationId, jobProfileId)
        return matchInterestService.employerInterestsOutsideRecommendations(
            userId,
            jobProfileId,
            candidates.map { it.id }.toSet(),
        )
    }

    fun candidatesOverview(userId: UUID, jobProfileId: Long): EmployerCandidatesOverviewDto {
        val jobProfile = findJobProfile(userId, jobProfileId)
        val candidates =
            matchingService.candidateRecommendationsForJob(jobProfile.occupationId, jobProfileId)
        val enriched = matchInterestService.enrichCandidateRecommendations(jobProfileId, candidates)
        val interests =
            matchInterestService.employerInterestsOutsideRecommendations(
                userId,
                jobProfileId,
                enriched.map { it.id }.toSet(),
            )
        return EmployerCandidatesOverviewDto(
            candidates = enriched,
            interests = interests,
        )
    }
}
