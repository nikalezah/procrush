package jobs.procrush.domain.employer

import jobs.procrush.db.EmployerRepository
import jobs.procrush.db.ReferenceRepository
import jobs.procrush.domain.ResourceNotFoundException
import jobs.procrush.fixtures.RecommendationStubs
import jobs.procrush.models.CreateJobProfileRequest
import jobs.procrush.models.EmployerDashboardDto
import jobs.procrush.models.JobProfileDto
import jobs.procrush.models.UpdateEmployerProfileRequest
import jobs.procrush.models.UpdateJobProfileRequest
import java.util.UUID

class EmployerProfileService(
    private val employerRepository: EmployerRepository,
    private val referenceRepository: ReferenceRepository,
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
        return EmployerDashboardDto(
            companyName = employer.name.ifBlank { "Компания не указана" },
            jobProfilesCount = profiles.size,
            activeJobProfilesCount = profiles.count { it.isActive },
            totalCandidatesStub = profiles.size * 3,
        )
    }

    fun candidates(jobProfileId: Long) = RecommendationStubs.candidateRecommendations(jobProfileId)
}
