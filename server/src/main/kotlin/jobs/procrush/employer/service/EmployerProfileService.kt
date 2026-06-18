package jobs.procrush.employer.service

import jobs.procrush.employer.dto.CreateJobProfileRequest
import jobs.procrush.employer.dto.EmployerDashboardDto
import jobs.procrush.employer.dto.JobProfileDto
import jobs.procrush.employer.dto.UpdateEmployerProfileRequest
import jobs.procrush.employer.dto.UpdateJobProfileRequest
import jobs.procrush.employer.repository.EmployerRepository
import jobs.procrush.matching.service.MatchingService
import jobs.procrush.shared.ResourceNotFoundException
import jobs.procrush.shared.repository.ReferenceRepository
import java.util.UUID

class EmployerProfileService(
    private val employerRepository: EmployerRepository,
    private val referenceRepository: ReferenceRepository,
    private val matchingService: MatchingService,
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
        return employerRepository.createJobProfile(getOrCreateEmployer(userId).id, request)
    }

    fun updateJobProfile(userId: UUID, jobProfileId: Long, request: UpdateJobProfileRequest) {
        referenceRepository.findOccupationById(request.occupationId)
            ?: throw IllegalArgumentException("Должность не найдена")
        employerRepository.updateJobProfile(getOrCreateEmployer(userId).id, jobProfileId, request)
            ?: throw ResourceNotFoundException("Профиль не найден")
    }

    fun deleteJobProfile(userId: UUID, jobProfileId: Long) {
        if (!employerRepository.deleteJobProfile(getOrCreateEmployer(userId).id, jobProfileId)) {
            throw ResourceNotFoundException("Профиль не найден")
        }
    }

    fun findJobProfile(userId: UUID, jobProfileId: Long) =
        employerRepository.findJobProfile(getOrCreateEmployer(userId).id, jobProfileId)
            ?: throw ResourceNotFoundException("Профиль не найден")

    fun dashboard(userId: UUID): EmployerDashboardDto {
        val employer = getOrCreateEmployer(userId)
        val profiles = employerRepository.listJobProfiles(employer.id)
        val activeProfiles = profiles.filter { it.isActive }
        val totalMatchedCandidates =
            activeProfiles.sumOf { profile ->
                matchingService.countMatchedCandidatesForOccupation(profile.occupationId)
            }
        return EmployerDashboardDto(
            companyName = employer.name.ifBlank { "Компания не указана" },
            jobProfilesCount = profiles.size,
            activeJobProfilesCount = activeProfiles.size,
            totalMatchedCandidates = totalMatchedCandidates,
        )
    }

    fun candidates(userId: UUID, jobProfileId: Long) =
        matchingService.candidateRecommendationsForJob(
            findJobProfile(userId, jobProfileId).occupationId,
            jobProfileId,
        )
}
